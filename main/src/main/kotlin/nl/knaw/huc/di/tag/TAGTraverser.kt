package nl.knaw.huc.di.tag

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.google.common.base.Preconditions
import nl.knaw.huc.di.tag.tagml.TAGML.CLOSE_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.CLOSE_TAG_STARTCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huc.di.tag.tagml.TAGML.MILESTONE_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_STARTCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.RESUME_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.SUSPEND_PREFIX
import nl.knaw.huc.di.tag.tagml.importer.AnnotationFactory
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo
import nl.knaw.huygens.alexandria.storage.*
import nl.knaw.huygens.alexandria.view.TAGView
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.stream.Collectors

class TAGTraverser(private val store: TAGStore, private val view: TAGView, private val document: TAGDocument) {

    private val relevantLayers: Set<String>
    private val processedNodes: MutableSet<TAGTextNode> = HashSet()
    private val discontinuousMarkupTextNodesToHandle = HashMap<Long, AtomicInteger>()
    private val textVariationStates: Deque<TextVariationState> = ArrayDeque()

    fun accept(tagVisitor: TAGVisitor) {
        val annotationFactory = AnnotationFactory(store, document.dto.textGraph)
        tagVisitor.setRelevantLayers(relevantLayers)
        tagVisitor.enterDocument(document)
        val openLayers: MutableSet<String> = HashSet()
        openLayers.add(DEFAULT_LAYER)
        val stateRef = AtomicReference(ExporterState())
        document
                .textNodeStream
                .forEach { nodeToProcess: TAGTextNode ->
                    //      logTextNode(nodeToProcess);
                    if (!processedNodes.contains(nodeToProcess)) {
                        val state = stateRef.get()
                        val markupIds: MutableSet<Long> = LinkedHashSet()
                        //        Collections.reverse(markupStreamForTextNode);
                        document
                                .getMarkupStreamForTextNode(nodeToProcess)
                                .forEach { mw: TAGMarkup ->
                                    val id = mw.dbId
                                    markupIds.add(id)
                                    state.openTags.computeIfAbsent(id) { toOpenTag(mw, openLayers) }
                                    state.closeTags.computeIfAbsent(id) { toCloseTag(mw) }
                                    openLayers.addAll(mw.layers)
                                    if (discontinuousMarkupTextNodesToHandle.containsKey(id)) {
                                        discontinuousMarkupTextNodesToHandle[id]!!.decrementAndGet()
                                    }
                                }
                        val relevantMarkupIds: Set<Long> = view.filterRelevantMarkup(markupIds)

                        //        if (needsDivider(nodeToProcess)) {
                        //          tagmlBuilder.append(DIVIDER);
                        //        }
                        var variationState = textVariationStates.peek()
                        if (variationState.isFirstNodeAfterConvergence(nodeToProcess)) {
                            tagVisitor.exitTextVariation()
                            //          tagmlBuilder.append(CONVERGENCE);
                            textVariationStates.pop()
                            variationState = textVariationStates.peek()
                        }
                        val toClose: MutableList<Long> = ArrayList(state.openMarkupIds)
                        toClose.removeAll(relevantMarkupIds)
                        toClose.reverse()
                        toClose.forEach(
                                Consumer { markupId: Long ->
//                                    var closeTag = state.closeTags[markupId].toString()
//                                    closeTag = addSuspendPrefixIfRequired(
//                                            closeTag, markupId, discontinuousMarkupTextNodesToHandle)
                                    val markup = store.getMarkup(markupId)
                                    tagVisitor.exitCloseTag(markup)
                                })
                        val toOpen: MutableList<Long> = ArrayList(relevantMarkupIds)
                        toOpen.removeAll(state.openMarkupIds)
                        toOpen.forEach(
                                Consumer { markupId: Long ->
                                    val markup = store.getMarkup(markupId)
                                    tagVisitor.enterOpenTag(markup)
//                                    var openTag = state.openTags[markupId].toString()
//                                    openTag = addResumePrefixIfRequired(
//                                            openTag, markupId, discontinuousMarkupTextNodesToHandle)
                                    markup
                                            .annotationStream
                                            .forEach { a: AnnotationInfo ->
                                                val value = serializeAnnotation(annotationFactory, a, tagVisitor)
                                                tagVisitor.addAnnotation(value)
                                            }
                                    tagVisitor.exitOpenTag(markup)
                                })
                        state.openMarkupIds.removeAll(toClose)
                        state.openMarkupIds.addAll(toOpen)
                        if (variationState.isBranchStartNode(nodeToProcess)) {
                            // this node starts a new branch of the current textvariation
                            stateRef.set(variationState.getStartState())
                            variationState.incrementBranchesStarted()
                        }
//                        val textNode = nodeToProcess.dto
                        val content = nodeToProcess.text
                        //        String escapedText = variationState.inVariation()
                        //            ? TAGML.escapeVariantText(content)
                        //            : TAGML.escapeRegularText(content);
                        tagVisitor.exitText(content, variationState.inVariation())
                        processedNodes.add(nodeToProcess)
                        state.lastTextNodeId = nodeToProcess.dbId
                        //        LOG.debug("TAGML={}\n", tagmlBuilder);
                    }
                }
        while (!textVariationStates.isEmpty()) {
            val textVariationState = textVariationStates.pop()
            if (textVariationState.inVariation()) {
                tagVisitor.enterTextVariation()
                //        tagmlBuilder.append(CONVERGENCE);
            }
        }
        val state = stateRef.get()
        state!!.openMarkupIds
                .descendingIterator()
                .forEachRemaining { markupId: Long? -> tagVisitor.exitCloseTag(store.getMarkup(markupId)) }
        //        .forEachRemaining(markupId -> tagmlBuilder.append(state.closeTags.get(markupId)));
        tagVisitor.exitDocument(document)
    }

    private fun serializeAnnotation(
            annotationFactory: AnnotationFactory, a: AnnotationInfo, tagVisitor: TAGVisitor): String {
        val stringBuilder = StringBuilder()
        if (a.hasName()) {
            val annotationAssigner = tagVisitor.serializeAnnotationAssigner(a.name)
            stringBuilder.append(annotationAssigner)
        }
        val value: String
        value = when (a.type) {
            AnnotationType.String -> {
                val stringValue = annotationFactory.getStringValue(a)
                tagVisitor.serializeStringAnnotationValue(stringValue)
            }
            AnnotationType.Number -> {
                val numberValue = annotationFactory.getNumberValue(a)
                tagVisitor.serializeNumberAnnotationValue(numberValue)
            }
            AnnotationType.Boolean -> {
                val booleanValue = annotationFactory.getBooleanValue(a)
                tagVisitor.serializeBooleanAnnotationValue(booleanValue)
            }
            AnnotationType.List -> {
                val listValue = annotationFactory.getListValue(a)
                val serializedItems = listValue.stream()
                        .map { ai: AnnotationInfo -> serializeAnnotation(annotationFactory, ai, tagVisitor) }
                        .collect(Collectors.toList())
                tagVisitor.serializeListAnnotationValue(serializedItems)
            }
            AnnotationType.Map -> {
                val mapValue = annotationFactory.getMapValue(a)
                val serializedMapItems = mapValue.stream()
                        .map { ai: AnnotationInfo -> serializeAnnotation(annotationFactory, ai, tagVisitor) }
                        .collect(Collectors.toList())
                tagVisitor.serializeMapAnnotationValue(serializedMapItems)
            }
            else -> throw RuntimeException("unhandled annotation type:" + a.type)
        }
        return stringBuilder.append(value).toString()
    }

    internal class ExporterState {
        var openMarkupIds: Deque<Long> = ArrayDeque()
        var openTags: MutableMap<Long, StringBuilder> = LinkedHashMap()
        var closeTags: MutableMap<Long, StringBuilder> = LinkedHashMap()
        var lastTextNodeId: Long? = null
        fun copy(): ExporterState {
            val copy = ExporterState()
            copy.openMarkupIds = openMarkupIds
            copy.openTags = openTags
            copy.closeTags = closeTags
            copy.lastTextNodeId = lastTextNodeId
            return copy
        }
    }

    internal class TextVariationState {
        private var startState: ExporterState? = null
        private var branchStartNodeIds: Set<Long> = HashSet()
        private var convergenceSucceedingNodeId: Long? = null
        private var branchesToTraverse: Int = 0

        fun setStartState(startState: ExporterState): TextVariationState {
            Preconditions.checkNotNull(startState)
            this.startState = startState
            return this
        }

        fun getStartState(): ExporterState {
            return startState!!
        }

        fun setBranchStartNodes(branchStartNodes: List<TAGTextNode>): TextVariationState {
            branchStartNodeIds = branchStartNodes.stream().map { obj: TAGTextNode -> obj.dbId }.collect(Collectors.toSet())
            branchesToTraverse = branchStartNodes.size
            return this
        }

        fun isBranchStartNode(node: TAGTextNode): Boolean = branchStartNodeIds.contains(node.dbId)

        fun setConvergenceSucceedingNodeId(convergenceSucceedingNodeId: Long): TextVariationState {
            this.convergenceSucceedingNodeId = convergenceSucceedingNodeId
            return this
        }

        fun isFirstNodeAfterConvergence(node: TAGTextNode): Boolean = node.dbId == convergenceSucceedingNodeId

        fun incrementBranchesStarted() {
            branchesToTraverse--
        }

        fun allBranchesTraversed(): Boolean = branchesToTraverse == 0

        fun inVariation(): Boolean = branchStartNodeIds.isNotEmpty()
    }

    private fun toCloseTag(markup: TAGMarkup): StringBuilder {
        val suspend = if (markup.isSuspended) SUSPEND_PREFIX else ""
        return if (markup.isAnonymous)
            StringBuilder()
        else
            StringBuilder(CLOSE_TAG_STARTCHAR)
                    .append(suspend)
                    .append(markup.extendedTag)
                    .append(CLOSE_TAG_ENDCHAR)
    }

    private fun toOpenTag(markup: TAGMarkup, openLayers: Set<String>): StringBuilder {
        val resume = if (markup.isResumed) RESUME_PREFIX else ""
        val newLayers: MutableSet<String> = HashSet(markup.layers)
        newLayers.removeAll(openLayers)
        val tagBuilder: StringBuilder = StringBuilder(OPEN_TAG_STARTCHAR)
                .append(resume)
                .append(markup.getExtendedTag(newLayers))
        return if (markup.isAnonymous) tagBuilder.append(MILESTONE_TAG_ENDCHAR) else tagBuilder.append(OPEN_TAG_ENDCHAR)
    }

    private fun addResumePrefixIfRequired(
            openTagIn: String,
            markupId: Long,
            discontinuousMarkupTextNodesToHandle: Map<Long, AtomicInteger>): String {
        var openTagOut = openTagIn
        if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
            val textNodesToHandle = discontinuousMarkupTextNodesToHandle[markupId]!!.get()
            val markup = store.getMarkup(markupId)
            if (textNodesToHandle < markup.textNodeCount - 1) {
                openTagOut = openTagOut.replace(OPEN_TAG_STARTCHAR, OPEN_TAG_STARTCHAR + RESUME_PREFIX)
            }
        }
        return openTagOut
    }

    private fun addSuspendPrefixIfRequired(
            closeTagIn: String,
            markupId: Long,
            discontinuousMarkupTextNodesToHandle: Map<Long, AtomicInteger>): String {
        var closeTagOut = closeTagIn
        if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
            val textNodesToHandle = discontinuousMarkupTextNodesToHandle[markupId]!!.get()
            if (textNodesToHandle > 0) {
                closeTagOut = closeTagOut.replace(CLOSE_TAG_STARTCHAR, CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX)
            }
        }
        return closeTagOut
    }

    init {
        //    final AnnotationFactory annotationFactory = new AnnotationFactory(store,
        // document.getDTO().textGraph);
        document
                .markupStream
                .filter { obj: TAGMarkup -> obj.isDiscontinuous }
                .forEach { mw: TAGMarkup ->
                    discontinuousMarkupTextNodesToHandle[mw.dbId] = AtomicInteger(mw.textNodeCount)
                }
        textVariationStates.push(TextVariationState())
        val layerNames = document.layerNames
        relevantLayers = view.filterRelevantLayers(layerNames)
    }
}
