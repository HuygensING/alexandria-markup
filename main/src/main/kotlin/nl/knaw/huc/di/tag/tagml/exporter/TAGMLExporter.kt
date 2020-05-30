package nl.knaw.huc.di.tag.tagml.exporter

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
import nl.knaw.huc.di.tag.TAGExporter
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES_END
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES_START
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH_END
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH_START
import nl.knaw.huc.di.tag.tagml.TAGML.CLOSE_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.CLOSE_TAG_STARTCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.CONVERGENCE
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huc.di.tag.tagml.TAGML.DIVERGENCE
import nl.knaw.huc.di.tag.tagml.TAGML.DIVIDER
import nl.knaw.huc.di.tag.tagml.TAGML.MILESTONE_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_STARTCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.RESUME_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.SUSPEND_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.escapeRegularText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeVariantText
import nl.knaw.huc.di.tag.tagml.importer.AnnotationFactory
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo
import nl.knaw.huygens.alexandria.storage.*
import nl.knaw.huygens.alexandria.view.TAGView
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer
import java.util.stream.Collectors

// TODO: only output layer info on end-tag when needed
// TODO: only show layer info as defined in view
class TAGMLExporter : TAGExporter {
    private var annotationFactory: AnnotationFactory? = null

    constructor(store: TAGStore?) : super(store) {}
    constructor(store: TAGStore?, view: TAGView?) : super(store, view) {}

    internal class ExporterState {
        var openMarkupIds: Deque<Long?> = ArrayDeque()
        var openTags: MutableMap<Long?, StringBuilder> = LinkedHashMap()
        var closeTags: MutableMap<Long?, StringBuilder> = LinkedHashMap()
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

    fun asTAGML(document: TAGDocument): String {
        annotationFactory = AnnotationFactory(store, document.dto.textGraph)
        val discontinuousMarkupTextNodesToHandle: MutableMap<Long?, AtomicInteger> = HashMap()
        document
                .markupStream
                .filter { obj: TAGMarkup -> obj.isDiscontinuous }
                .forEach { mw: TAGMarkup ->
                    discontinuousMarkupTextNodesToHandle[mw.dbId] = AtomicInteger(mw.textNodeCount)
                }
        val textVariationStates: Deque<TextVariationState> = ArrayDeque()
        textVariationStates.push(TextVariationState())
        val openLayers: MutableSet<String> = HashSet()
        openLayers.add(DEFAULT_LAYER)
        val processedNodes: MutableSet<TAGTextNode> = HashSet()
        val stateRef = AtomicReference(ExporterState())
        val tagmlBuilder = StringBuilder()
        tagmlBuilder.append(document.rawHeader).append("\n")
        document
                .textNodeStream
                .forEach { nodeToProcess: TAGTextNode ->
                    handleTextNode(
                            nodeToProcess,
                            document,
                            discontinuousMarkupTextNodesToHandle,
                            textVariationStates,
                            openLayers,
                            processedNodes,
                            stateRef,
                            tagmlBuilder)
                }
        while (!textVariationStates.isEmpty()) {
            val textVariationState = textVariationStates.pop()
            if (textVariationState.inVariation()) {
                tagmlBuilder.append(CONVERGENCE)
            }
        }
        val state = stateRef.get()
        state!!.openMarkupIds
                .descendingIterator()
                .forEachRemaining { markupId: Long? -> tagmlBuilder.append(state.closeTags[markupId]) }
        return tagmlBuilder
                .toString()
                .replace(BRANCHES_START + BRANCH_START, DIVERGENCE)
                .replace(BRANCH_END + BRANCH_START, DIVIDER)
                .replace(BRANCH_END + BRANCHES_END, CONVERGENCE)
    }

    private fun handleTextNode(
            nodeToProcess: TAGTextNode,
            document: TAGDocument,
            discontinuousMarkupTextNodesToHandle: Map<Long?, AtomicInteger>,
            textVariationStates: Deque<TextVariationState>,
            openLayers: MutableSet<String>,
            processedNodes: MutableSet<TAGTextNode>,
            stateRef: AtomicReference<ExporterState?>,
            tagmlBuilder: StringBuilder) {
        //      logTextNode(nodeToProcess);
        if (!processedNodes.contains(nodeToProcess)) {
            val state = stateRef.get()
            val markupIds: MutableSet<Long> = LinkedHashSet()
            val markupForTextNode = document.getMarkupStreamForTextNode(nodeToProcess).collect(Collectors.toList())
            //        Collections.reverse(markupForTextNode);
            markupForTextNode.forEach(
                    Consumer { mw: TAGMarkup ->
                        val id = mw.dbId
                        markupIds.add(id)
                        state!!.openTags.computeIfAbsent(id) { k: Long? -> toOpenTag(mw, openLayers) }
                        state.closeTags.computeIfAbsent(id) { k: Long? -> toCloseTag(mw) }
                        openLayers.addAll(mw.layers)
                        if (discontinuousMarkupTextNodesToHandle.containsKey(id)) {
                            discontinuousMarkupTextNodesToHandle[id]!!.decrementAndGet()
                        }
                    })
            val relevantMarkupIds: Set<Long?> = view.filterRelevantMarkup(markupIds)
            if (needsDivider(nodeToProcess)) {
                tagmlBuilder.append(DIVIDER)
            }
            var variationState = textVariationStates.peek()
            if (variationState.isFirstNodeAfterConvergence(nodeToProcess)) {
                tagmlBuilder.append(CONVERGENCE)
                textVariationStates.pop()
                variationState = textVariationStates.peek()
            }
            val toClose: MutableList<Long?> = ArrayList(state!!.openMarkupIds)
            toClose.removeAll(relevantMarkupIds)
            toClose.reverse()
            toClose.forEach(
                    Consumer { markupId: Long? ->
                        var closeTag = state.closeTags[markupId].toString()
                        closeTag = addSuspendPrefixIfRequired(
                                closeTag, markupId, discontinuousMarkupTextNodesToHandle)
                        tagmlBuilder.append(closeTag)
                    })
            val toOpen: MutableList<Long?> = ArrayList(relevantMarkupIds)
            toOpen.removeAll(state.openMarkupIds)
            toOpen.forEach(
                    Consumer { markupId: Long? ->
                        var openTag = state.openTags[markupId].toString()
                        openTag = addResumePrefixIfRequired(openTag, markupId, discontinuousMarkupTextNodesToHandle)
                        tagmlBuilder.append(openTag)
                    })
            state.openMarkupIds.removeAll(toClose)
            state.openMarkupIds.addAll(toOpen)
            if (variationState.isBranchStartNode(nodeToProcess)) {
                // this node starts a new branch of the current textvariation
                stateRef.set(variationState.getStartState())
                variationState.incrementBranchesStarted()
            }
            //        TAGTextNodeDTO textNode = nodeToProcess.getDTO();
            val content = nodeToProcess.text
            val escapedText = if (variationState.inVariation()) escapeVariantText(content) else escapeRegularText(content)
            tagmlBuilder.append(escapedText)
            processedNodes.add(nodeToProcess)
            state.lastTextNodeId = nodeToProcess.dbId
            //        LOG.debug("TAGML={}\n", tagmlBuilder);
        }
    }

    private fun addResumePrefixIfRequired(
            openTag: String,
            markupId: Long?,
            discontinuousMarkupTextNodesToHandle: Map<Long?, AtomicInteger>): String {
        var openTag = openTag
        if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
            val textNodesToHandle = discontinuousMarkupTextNodesToHandle[markupId]!!.get()
            val markup = store.getMarkup(markupId)
            if (textNodesToHandle < markup.textNodeCount - 1) {
                openTag = openTag.replace(OPEN_TAG_STARTCHAR, OPEN_TAG_STARTCHAR + RESUME_PREFIX)
            }
        }
        return openTag
    }

    private fun addSuspendPrefixIfRequired(
            closeTag: String,
            markupId: Long?,
            discontinuousMarkupTextNodesToHandle: Map<Long?, AtomicInteger>): String {
        var closeTag = closeTag
        if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
            val textNodesToHandle = discontinuousMarkupTextNodesToHandle[markupId]!!.get()
            if (textNodesToHandle > 0) {
                closeTag = closeTag.replace(CLOSE_TAG_STARTCHAR, CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX)
            }
        }
        return closeTag
    }

    private fun needsDivider(textNode: TAGTextNode): Boolean {
        // TODO: refactor
        //    List<TAGTextNode> prevTextNodes = textNode.getPrevTextNodes();
        //    return prevTextNodes.size() == 1
        //        && prevTextNodes.get(0).isDivergence()
        //        && !prevTextNodes.get(0).getNextTextNodes().get(0).equals(textNode);
        return false
    }

    private fun toCloseTag(markup: TAGMarkup): StringBuilder {
        val suspend = if (markup.isSuspended) SUSPEND_PREFIX else ""
        return if (markup.isAnonymous) StringBuilder() else StringBuilder(CLOSE_TAG_STARTCHAR)
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
        markup.annotationStream.forEach { a: AnnotationInfo -> tagBuilder.append(" ").append(toTAGML(a)) }
        return if (markup.isAnonymous) tagBuilder.append(MILESTONE_TAG_ENDCHAR) else tagBuilder.append(OPEN_TAG_ENDCHAR)
    }

    fun toTAGML(a: AnnotationInfo): StringBuilder {
        val stringBuilder = StringBuilder()
        if (a.hasName()) {
            val connectingChars = if (a.type == AnnotationType.Reference) "->" else "="
            stringBuilder.append(a.name).append(connectingChars)
        }
        when (a.type) {
            AnnotationType.String -> {
                val stringValue = annotationFactory!!.getStringValue(a).replace("'", "\\'")
                stringBuilder.append("'").append(stringValue).append("'")
            }
            AnnotationType.Number -> {
                val numberValue = annotationFactory!!.getNumberValue(a)
                val asString = numberValue.toString().replaceFirst(".0$".toRegex(), "")
                stringBuilder.append(asString)
            }
            AnnotationType.Boolean -> {
                val booleanValue = annotationFactory!!.getBooleanValue(a)
                stringBuilder.append(booleanValue)
            }
            AnnotationType.List -> {
                stringBuilder.append("[")
                val listValue = annotationFactory!!.getListValue(a)
                stringBuilder.append(listValue.stream().map { a: AnnotationInfo -> toTAGML(a) }.collect(Collectors.joining(",")))
                stringBuilder.append("]")
            }
            AnnotationType.Map -> {
                stringBuilder.append("{")
                val mapValue = annotationFactory!!.getMapValue(a)
                stringBuilder.append(mapValue.stream().map { a: AnnotationInfo -> toTAGML(a) }.collect(Collectors.joining(" ")))
                stringBuilder.append("}")
            }
            AnnotationType.Reference -> {
                val refValue = annotationFactory!!.getReferenceValue(a)
                stringBuilder.append(refValue)
            }
            else -> throw RuntimeException("unhandled annotation type:" + a.type)
        }
        return stringBuilder
    }

    private fun logTextNode(textNode: TAGTextNode) {
        val dto = textNode.dto
        LOG.debug("\n")
        LOG.debug("TextNode(id={}, text=<{}>)", textNode.dbId, dto.text)
    }

    private fun asValueString(o: Any): String =
            if (o is String) {
                "'$o'"
            } else o.toString()

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

        fun getStartState(): ExporterState? {
            Preconditions.checkNotNull(startState)
            return startState
        }

        fun setBranchStartNodes(branchStartNodes: List<TAGTextNode>): TextVariationState {
            branchStartNodeIds = branchStartNodes.stream()
                    .map { obj: TAGTextNode -> obj.dbId }
                    .collect(Collectors.toSet())
            branchesToTraverse = branchStartNodes.size
            return this
        }

        fun isBranchStartNode(node: TAGTextNode): Boolean = branchStartNodeIds.contains(node.dbId)

        fun setConvergenceSucceedingNodeId(convergenceSucceedingNodeId: Long?): TextVariationState {
            this.convergenceSucceedingNodeId = convergenceSucceedingNodeId
            return this
        }

        fun isFirstNodeAfterConvergence(node: TAGTextNode): Boolean = node.dbId == convergenceSucceedingNodeId

        fun incrementBranchesStarted() {
            branchesToTraverse--
        }

        fun allBranchesTraversed(): Boolean = branchesToTraverse == 0

        fun inVariation(): Boolean = !branchStartNodeIds.isEmpty()
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TAGMLExporter::class.java)
    }
}
