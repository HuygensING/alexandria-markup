package nl.knaw.huc.di.tag.tagml.importer

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

import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES
import nl.knaw.huc.di.tag.tagml.TAGML.CLOSE_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.CLOSE_TAG_STARTCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.CONVERGENCE
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huc.di.tag.tagml.TAGML.DIVERGENCE
import nl.knaw.huc.di.tag.tagml.TAGML.DIVIDER
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_STARTCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPTIONAL_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.RESUME_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.SUSPEND_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.unEscape
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*
import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.storage.TAGTextNode
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO
import org.antlr.v4.runtime.ParserRuleContext
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import java.util.stream.Collectors

class TAGMLListener(private val store: TAGStore, errorListener: ErrorListener?) : AbstractTAGMLListener(errorListener) {

    var document: TAGDocument
        private set

    private val idsInUse = HashMap<String, String>()
    private val namespaces: MutableMap<String, String> = mutableMapOf()
    private val annotationFactory: AnnotationFactory
    private var state = State()
    private val stateStack: Deque<State> = ArrayDeque()
    private val documentStack: Deque<TAGDocument> = ArrayDeque() // TODO: move to state
    private val textVariationStateStack: Deque<TextVariationState> = ArrayDeque()
    private var atDocumentStart = true
    private var openTagRange: MutableMap<Long, Range> = mutableMapOf()
    private var closeTagRange: MutableMap<Long, Range> = mutableMapOf()

    private val markupRanges: Map<Long, RangePair>
        get() {
            val markupRangeMap: MutableMap<Long, RangePair> = HashMap()
            for (markupId: Long in openTagRange.keys) {
                markupRangeMap[markupId] = RangePair(openTagRange[markupId], closeTagRange[markupId])
            }
            return markupRangeMap
        }

    class State {
        var openMarkup: MutableMap<String, Deque<TAGMarkup>> = mutableMapOf()
        var suspendedMarkup: MutableMap<String, Deque<TAGMarkup>> = mutableMapOf()
        var allOpenMarkup: Deque<TAGMarkup> = ArrayDeque()
        var rootMarkupId: Long? = null
        var eof = false

        fun copy(): State {
            val copy = State()
            copy.openMarkup = HashMap()
            openMarkup.forEach { (k: String, v: Deque<TAGMarkup>) -> copy.openMarkup[k] = ArrayDeque(v) }
            copy.suspendedMarkup = HashMap()
            suspendedMarkup.forEach { (k: String, v: Deque<TAGMarkup>) -> copy.suspendedMarkup[k] = ArrayDeque(v) }
            copy.allOpenMarkup = ArrayDeque(allOpenMarkup)
            copy.rootMarkupId = rootMarkupId
            copy.eof = eof
            return copy
        }

        fun rootMarkupIsNotSet(): Boolean =
                rootMarkupId == null
    }

    class TextVariationState {
        var startState: State? = null
        var endStates: MutableList<State> = mutableListOf()
        var startMarkup: TAGMarkup? = null

        //    public List<TAGTextNode> endNodes = new ArrayList<>();
        private var openMarkup: MutableMap<Int, MutableList<TAGMarkup>> = mutableMapOf()
        var branch = 0
        fun addOpenMarkup(markup: TAGMarkup) {
            openMarkup.getOrPut(branch) { mutableListOf() }
                    .add(markup)
        }

        fun removeOpenMarkup(markup: TAGMarkup?) {
            openMarkup.getOrPut(branch) { mutableListOf() }
                    .remove(markup)
        }
    }

    private fun verifyNoSuspendedMarkupLeft() {
        val noSuspendedMarkup = state.suspendedMarkup.values.stream().allMatch { obj: Deque<TAGMarkup> -> obj.isEmpty() }
        if (!noSuspendedMarkup) {
            state.suspendedMarkup.values.stream()
                    .flatMap { obj: Deque<TAGMarkup> -> obj.stream() } //          .map(this::suspendTag)
                    .distinct()
                    .forEach { markup: TAGMarkup ->
                        val range = closeTagRange[markup.dbId]
                        val startPosition = range!!.startPosition
                        val endPosition = range.endPosition
                        errorListener.addError(
                                startPosition,
                                endPosition,
                                "Some suspended markup was not resumed: %s",
                                suspendTag(markup)) // TODO: add range of unresumed tags
                    }
        }
    }

    override fun exitDocument(ctx: DocumentContext) {
        document.removeDefaultLayerIfUnused()
        document.linkParentlessLayerRootsToDocument()
        document.namespaces = namespaces
        document.markupRangeMap = markupRanges
        update(document.dto)
        verifyNoMarkupUnclosed()
        verifyNoSuspendedMarkupLeft()
    }

    private fun verifyNoMarkupUnclosed() {
        val noOpenMarkup = state.openMarkup.values.stream().allMatch { obj: Deque<TAGMarkup> -> obj.isEmpty() }
        if (!noOpenMarkup) {
            state.openMarkup.values.stream()
                    .flatMap { obj: Deque<TAGMarkup> -> obj.stream() } //          .map(this::openTag)
                    .distinct()
                    .forEach { openMarkup: TAGMarkup ->
                        val markupId = openMarkup.dbId
                        val range = openTagRange[markupId]
                        val startPos = range!!.startPosition
                        val endPos = range.endPosition
                        errorListener.addError(
                                startPos,
                                endPos,
                                "Missing close tag(s) for: %s",
                                openTag(openMarkup)) // TODO: add range of unclosed tag(s)
                    }
        }
    }

    override fun exitText(ctx: TextContext) {
        val text = unEscape(ctx.text)
        //    LOG.debug("text=<{}>", text);
        atDocumentStart = atDocumentStart && StringUtils.isBlank(text)
        // TODO: smarter whitespace handling
        val useText = !atDocumentStart /*&& !StringUtils.isBlank(text)*/
        if (useText) {
            if (StringUtils.isNotBlank(text)) {
                checkEOF(ctx)
            }
            if (state.rootMarkupIsNotSet()) {
                addBreakingError(ctx, "No text allowed here, the root markup must be started first.")
            }
            val tn = store.createTextNode(text)
            addAndConnectToMarkup(tn)
        }
    }

    private fun checkLayerIsOpen(ctx: StartTagContext, layerId: String) {
        if (state.openMarkup[layerId]!!.isEmpty()) {
            val layer = if (layerId.isEmpty()) "the default layer" else "layer '$layerId'"
            addError(
                    ctx,
                    "%s cannot be used here, since the root markup of this layer has closed already.",
                    layer)
        }
    }

    private fun addAndConnectToMarkup(tn: TAGTextNode) {
        val relevantMarkup = relevantOpenMarkup
        document.addTextNode(tn, relevantMarkup)
    }

    // Once again, the default layer is special! TODO: fix default
    // layer usage
    private val relevantOpenMarkup: List<TAGMarkup>
        get() {
            val relevantMarkup: MutableList<TAGMarkup> = ArrayList()
            if (!state.allOpenMarkup.isEmpty()) {
                val handledLayers: MutableSet<String> = HashSet()
                for (m in state.allOpenMarkup) {
                    val layers = m!!.layers
                    val markupHasNoHandledLayer = layers.stream().noneMatch { o: String -> handledLayers.contains(o) }
                    if (markupHasNoHandledLayer) {
                        relevantMarkup.add(m)
                        handledLayers.addAll(layers)
                        var goOn = true
                        while (goOn) {
                            val newParentLayers = handledLayers
                                    .map { l: String -> document.dto.textGraph.parentLayerMap[l] }
                                    .filterNotNull()
                                    .filter { l: String -> !handledLayers.contains(l) }
                                    .filter { l: String -> DEFAULT_LAYER != l }
                                    .toSet()
                            handledLayers.addAll(newParentLayers)
                            goOn = newParentLayers.isNotEmpty()
                        }
                    }
                }
            }
            return relevantMarkup
        }

    override fun enterStartTag(ctx: StartTagContext) {
        checkEOF(ctx)
        if (tagNameIsValid(ctx)) {
            val markupNameContext = ctx.markupName()
            val markupName = markupNameContext.name().text
            //      LOG.debug("startTag.markupName=<{}>", markupName);
            checkNameSpace(ctx, markupName)
            for (annotation: AnnotationContext in ctx.annotation()) {
                LOG.debug("  startTag.annotation={{}}", annotation.text)
            }
            val prefix = markupNameContext.prefix()
            val optional = prefix != null && prefix.text == OPTIONAL_PREFIX
            val resume = prefix != null && prefix.text == RESUME_PREFIX
            val markup = if (resume) resumeMarkup(ctx) else addMarkup(markupName, ctx.annotation(), ctx).setOptional(optional)
            val layerIds = extractLayerInfo(ctx.markupName().layerInfo())
            val layers: MutableSet<String> = HashSet()
            state.allOpenMarkup.push(markup)
            openTagRange[markup.dbId] = rangeOf(ctx)
            val firstTag = !document.layerNames.contains(DEFAULT_LAYER)
            if (firstTag) {
                addDefaultLayer(markup, layers)
                state.rootMarkupId = markup.dbId
            }
            for (layerId: String in layerIds) {
                if (layerId.contains("+")) {
                    val parts = layerId.split("\\+".toRegex()).toTypedArray()
                    val parentLayer = parts[0]
                    val newLayerId = parts[1]
                    document.addLayer(newLayerId, markup, parentLayer)
                    //          layers.add(parentLayer);
                    layers.add(newLayerId)
                } else if (!(firstTag && DEFAULT_LAYER == layerId)) {
                    checkLayerWasAdded(ctx, layerId)
                    checkLayerIsOpen(ctx, layerId)
                    document.openMarkupInLayer(markup, layerId)
                    layers.add(layerId)
                }
            }
            markup.addAllLayers(layers)
            addSuffix(markupNameContext, markup)
            for (layer in markup.layers) {
                state.openMarkup.getOrPut(layer) { ArrayDeque() }
                        .push(markup)
            }
            currentTextVariationState().addOpenMarkup(markup)
            store.persist(markup.dto)
        }
    }

    override fun enterRichTextValue(ctx: RichTextValueContext) {
        stateStack.push(state)
        state = State()
        documentStack.push(document)
        document = store.createDocument()
        super.enterRichTextValue(ctx)
    }

    override fun exitRichTextValue(ctx: RichTextValueContext) {
        super.exitRichTextValue(ctx)
        state = stateStack.pop()
        document = documentStack.pop()
    }

    override fun exitHeader(ctx: HeaderContext) {
        parseHeader(ctx)
    }

    private fun parseHeader(ctx: HeaderContext) {
        document.rawHeader = ctx.text
        val headerMap = mutableMapOf<String, String>()
        for (pair in ctx.json_pair()) {
            val field = pair.JSON_STRING().text
            val value = pair.json_value()
            when (field) {
                ":ontology" -> parseOntology(value)
            }
            headerMap[field] = value.text
//            when (pair.json_value()) {
//                is Json_arrContext -> TODO()
//                is Json_objContext -> TODO()
//                is Json_pairContext -> TODO()
//                is Json_valueContext -> TODO()
//            }
        }
        LOG.info("header={}", headerMap)
    }

    private fun parseOntology(value: Json_valueContext) {
        if (value is Json_objContext) {
            val pairs = value.json_pair()

        } else {
            TODO("invalid format")
        }
    }

    private fun addSuffix(markupNameContext: MarkupNameContext, markup: TAGMarkup) {
        val suffix = markupNameContext.suffix()
        if (suffix != null) {
            val id = suffix.text.replace(TILDE, "")
            markup.suffix = id
        }
    }

    private fun checkLayerWasAdded(ctx: StartTagContext, layerId: String) {
        if (!state.openMarkup.containsKey(layerId)) {
            addBreakingError(
                    ctx.markupName().layerInfo(),
                    "Layer %s has not been added at this point, use +%s to add a layer.",
                    layerId,
                    layerId)
        }
    }

    override fun exitMilestoneTag(ctx: MilestoneTagContext) {
        if (state.rootMarkupIsNotSet()) {
            addError(ctx, "The root markup cannot be a milestone tag.")
        }
        if (tagNameIsValid(ctx)) {
//            val markupName = ctx.name().text
            //      LOG.debug("milestone.markupName=<{}>", markupName);
            for (annotation: AnnotationContext in ctx.annotation()) {
                LOG.debug("milestone.annotation={{}}", annotation.text)
            }
            val layers = extractLayerInfo(ctx.layerInfo())
            val tn = store.createTextNode("")
            addAndConnectToMarkup(tn)
            //      logTextNode(tn);
            val markup = addMarkup(ctx.name().text, ctx.annotation(), ctx)
            markup.addAllLayers(layers)
            for (layerName: String in layers) {
                linkTextToMarkupForLayer(tn, markup, layerName)
                document.openMarkupInLayer(markup, layerName)
                document.closeMarkupInLayer(markup, layerName)
            }
            store.persist(markup.dto)
        }
    }

    private fun checkNameSpace(ctx: StartTagContext, markupName: String) {
        if (markupName.contains(":")) {
            val namespace = markupName.split(":".toRegex(), 2).toTypedArray()[0]
            if (!namespaces.containsKey(namespace)) {
                addError(ctx, "Namespace %s has not been defined.", namespace)
            }
        }
    }

    private fun addDefaultLayer(markup: TAGMarkup, layers: MutableSet<String>) {
        document.addLayer(DEFAULT_LAYER, markup, null)
        layers.add(DEFAULT_LAYER)
    }

    override fun exitEndTag(ctx: EndTagContext) {
        checkEOF(ctx)
        if (tagNameIsValid(ctx)) {
//            val markupName = ctx.markupName().name().text
            //      LOG.debug("endTag.markupName=<{}>", markupName);
            val markup = removeFromOpenMarkup(ctx.markupName())
            if (markup != null) {
                closeTagRange[markup.dbId] = rangeOf(ctx)
            }
        }
    }

    private fun checkForOpenMarkupInBranch(ctx: ParserRuleContext) {
        val branch = currentTextVariationState().branch + 1
        val openMarkupAtStart: Map<String, Deque<TAGMarkup>> = currentTextVariationState().startState!!.openMarkup
        val currentOpenMarkup: Map<String, Deque<TAGMarkup>> = state.openMarkup
        for (layerName in openMarkupAtStart.keys) {
            val openMarkupAtStartInLayer = openMarkupAtStart[layerName]
            val currentOpenMarkupInLayer = currentOpenMarkup[layerName]
            val closedInBranch: MutableList<TAGMarkup> = ArrayList(openMarkupAtStartInLayer)
            closedInBranch.removeAll(currentOpenMarkupInLayer!!)
            if (closedInBranch.isNotEmpty()) {
                val openTags = closedInBranch.stream()
                        .map { m: TAGMarkup -> openTag(m) }
                        .collect(Collectors.joining(","))
                addBreakingError(
                        ctx,
                        "Markup %s opened before branch %s, should not be closed in a branch.",
                        openTags,
                        branch)
            }
            val openedInBranch: MutableList<TAGMarkup> = ArrayList(currentOpenMarkupInLayer)
            openedInBranch.removeAll(openMarkupAtStartInLayer!!)
            val openTags = openedInBranch.stream()
                    .filter { m: TAGMarkup -> !m.tag.startsWith(":") }
                    .map { m: TAGMarkup -> openTag(m) }
                    .collect(Collectors.joining(","))
            if (openTags.isNotEmpty()) {
                addBreakingError(
                        ctx,
                        "Markup %s opened in branch %s must be closed before starting a new branch.",
                        openTags,
                        branch)
            }
        }
    }

    override fun enterTextVariation(ctx: TextVariationContext) {
        checkEOF(ctx)

        //    LOG.debug("<|
        // lastTextNodeInTextVariationStack.size()={}",lastTextNodeInTextVariationStack.size());
        val branches = openTextVariationMarkup(BRANCHES, DEFAULT_LAYER_ONLY)
        val textVariationState = TextVariationState().apply {
            startMarkup = branches
            startState = state.copy()
            branch = 0
        }
        textVariationStateStack.push(textVariationState)
        openTextVariationMarkup(BRANCH, DEFAULT_LAYER_ONLY)
    }

    private fun openTextVariationMarkup(tagName: String, layers: Set<String>): TAGMarkup {
        val markup = store.createMarkup(document, tagName)
        document.addMarkup(markup)
        markup.addAllLayers(layers)
        state.allOpenMarkup.push(markup)
        for (layer in markup.layers) {
            document.openMarkupInLayer(markup, layer)
            state.openMarkup.getOrPut(layer) { ArrayDeque() }.push(markup)
        }
        currentTextVariationState().addOpenMarkup(markup)
        store.persist(markup.dto)
        return markup
    }

    override fun exitTextVariationSeparator(ctx: TextVariationSeparatorContext) {
        checkEOF(ctx)
        closeSystemMarkup(BRANCH, DEFAULT_LAYER_ONLY)
        checkForOpenMarkupInBranch(ctx)
        currentTextVariationState().endStates.add(state.copy())
        currentTextVariationState().branch += 1
        state = currentTextVariationState().startState!!.copy()
        openTextVariationMarkup(BRANCH, DEFAULT_LAYER_ONLY)
    }

    private fun closeTextVariationMarkup(extendedMarkupName: String, layers: Set<String>) {
        removeFromMarkupStack2(extendedMarkupName, state.allOpenMarkup)
        for (l in layers) {
            val markupStack = state.openMarkup.getOrPut(l) { ArrayDeque() }
            val markup = removeFromMarkupStack2(extendedMarkupName, markupStack)
            document.closeMarkupInLayer(markup, l)
        }
    }

    private fun checkEndStates(ctx: TextVariationContext) {
        val suspendedMarkupInBranch: List<List<String>> = ArrayList()
        val resumedMarkupInBranch: List<List<String>> = ArrayList()
        val openedMarkupInBranch: List<List<String>> = ArrayList()
        val closedMarkupInBranch: List<List<String>> = ArrayList()
//        val startState = currentTextVariationState().startState
        //    Map<String, Deque<TAGMarkup>> suspendedMarkupBeforeDivergence =
        // startState.suspendedMarkup;
        //    Map<String, Deque<TAGMarkup>> openMarkupBeforeDivergence = startState.openMarkup;

        //    currentTextVariationState().endStates.forEach(state -> {
        //      List<String> suspendedMarkup = state.suspendedMarkup.stream()
        //          .filter(m -> !suspendedMarkupBeforeDivergence.contains(m))
        //          .map(this::suspendTag)
        //          .collect(toList());
        //      suspendedMarkupInBranch.add(suspendedMarkup);

        //      // TODO: resumedMarkup

        //      List<String> openedInBranch = state.openMarkup.stream()
        //          .filter(m -> !openMarkupBeforeDivergence.contains(m))
        //          .map(this::openTag)
        //          .collect(toList());
        //      openedMarkupInBranch.add(openedInBranch);

        //      List<String> closedInBranch = openMarkupBeforeDivergence.stream()
        //          .filter(m -> !state.openMarkup.contains(m))
        //          .map(this::closeTag)
        //          .collect(toList());
        //      closedMarkupInBranch.add(closedInBranch);
        //    });

        //    String errorPrefix = errorPrefix(ctx, true);
        checkSuspendedOrResumedMarkupBetweenBranches(suspendedMarkupInBranch, resumedMarkupInBranch, ctx)
        checkOpenedOrClosedMarkupBetweenBranches(openedMarkupInBranch, closedMarkupInBranch, ctx)
    }

    override fun exitTextVariation(ctx: TextVariationContext) {
        checkEOF(ctx)
        closeSystemMarkup(BRANCH, DEFAULT_LAYER_ONLY)
        checkForOpenMarkupInBranch(ctx)
        closeSystemMarkup(BRANCHES, DEFAULT_LAYER_ONLY)
        currentTextVariationState().endStates.add(state.copy())
        checkEndStates(ctx)
        if (errorListener.hasErrors()) { // TODO: check if a breaking error should have been set earlier
            return
        }
        textVariationStateStack.pop()
    }

    private fun closeSystemMarkup(tag: String, layers: Set<String>) {
        for (l in layers) {
            val suffix = if (DEFAULT_LAYER == l) "" else "|$l"
            val layer: MutableSet<String> = HashSet()
            layer.add(l)
            closeTextVariationMarkup(tag + suffix, layer)
        }
    }

    private val openLayers: Set<String>
        get() = relevantOpenMarkup.stream()
                .map { obj: TAGMarkup? -> obj!!.layers }
                .flatMap { obj: Set<String> -> obj.stream() }
                .collect(Collectors.toSet())

    private fun checkSuspendedOrResumedMarkupBetweenBranches(
            suspendedMarkupInBranch: List<List<String>>,
            resumedMarkupInBranch: List<List<String>>,
            ctx: ParserRuleContext) {
        val suspendedMarkupSet: Set<List<String>> = HashSet(suspendedMarkupInBranch)
        if (suspendedMarkupSet.size > 1) {
            val branchLines = StringBuilder()
            for (i in suspendedMarkupInBranch.indices) {
                val suspendedMarkup = suspendedMarkupInBranch[i]
                val has = if (suspendedMarkup.isEmpty()) "no suspended markup." else "suspended markup $suspendedMarkup."
                branchLines.append("\n\tbranch ").append(i + 1).append(" has ").append(has)
            }
            addBreakingError(
                    ctx, "There is a discrepancy in suspended markup between branches:%s", branchLines)
        }
    }

    private fun checkOpenedOrClosedMarkupBetweenBranches(
            openedMarkupInBranch: List<List<String>>,
            closedMarkupInBranch: List<List<String>>,
            ctx: ParserRuleContext) {
        val branchMarkupSet: MutableSet<List<String>> = HashSet(openedMarkupInBranch)
        branchMarkupSet.addAll(closedMarkupInBranch)
        if (branchMarkupSet.size > 2) {
            val branchLines = StringBuilder()
            for (i in openedMarkupInBranch.indices) {
                val closed = java.lang.String.join(", ", closedMarkupInBranch[i])
                val closedStatement = if (closed.isEmpty()) "didn't close any markup" else "closed markup $closed"
                val opened = java.lang.String.join(", ", openedMarkupInBranch[i])
                val openedStatement = if (opened.isEmpty()) "didn't open any new markup" else "opened markup $opened"
                branchLines
                        .append("\n\tbranch ")
                        .append(i + 1)
                        .append(" ")
                        .append(closedStatement)
                        .append(" that was opened before the ")
                        .append(DIVERGENCE)
                        .append(" and ")
                        .append(openedStatement)
                        .append(" to be closed after the ")
                        .append(CONVERGENCE)
            }
            addBreakingError(
                    ctx, "There is an open markup discrepancy between the branches:%s", branchLines)
        }
    }

    private fun removeFromOpenMarkup(ctx: MarkupNameContext): TAGMarkup? {
        val markupName = ctx.name().text
        var extendedMarkupName = markupName
        extendedMarkupName = withPrefix(ctx, extendedMarkupName)
        extendedMarkupName = withSuffix(ctx, extendedMarkupName)
        val isSuspend = ctx.prefix() != null && ctx.prefix().text == SUSPEND_PREFIX
        val layers = deduceLayers(ctx, markupName, extendedMarkupName)
        val layerSuffixNeeded = !(layers.size == 1 && layers.iterator().next() == DEFAULT_LAYER)
        val foundLayerSuffix = if (layerSuffixNeeded)
            DIVIDER + layers.stream()
                    .filter { l: String -> DEFAULT_LAYER != l }
                    .sorted()
                    .collect(Collectors.joining(","))
        else ""
        extendedMarkupName += foundLayerSuffix
        removeFromMarkupStack2(extendedMarkupName, state.allOpenMarkup)
        var markup: TAGMarkup? = null
        for (l in layers) {
            val markupStack = state.openMarkup.getOrPut(l) { ArrayDeque() }
            markup = removeFromMarkupStack(extendedMarkupName, markupStack)
            if (markup == null) {
                val emn = AtomicReference(extendedMarkupName)
                val markupIsOpen = markupStack.stream()
                        .map { obj: TAGMarkup? -> obj!!.extendedTag }
                        .anyMatch { et: String -> emn.get() == et }
                markup = if (!markupIsOpen) {
                    addError(
                            ctx.getParent(),
                            "Close tag <%s] found without corresponding open tag.",
                            extendedMarkupName)
                    return null
                } else if (!isSuspend) {
                    val expected = markupStack.peek()
                    if (expected!!.hasTag(BRANCH)) {
                        addBreakingError(
                                ctx.getParent(),
                                "Markup [%s> opened before branch %s, should not be closed in a branch.",
                                extendedMarkupName,
                                currentTextVariationState().branch + 1)
                    }
                    val hint = if (l.isEmpty()) " Use separate layers to allow for overlap." else ""
                    addError(
                            ctx.getParent(),
                            "Close tag <%s] found, expected %s.%s",
                            extendedMarkupName,
                            closeTag(expected),
                            hint)
                    return null
                } else {
                    removeFromMarkupStack2(extendedMarkupName, markupStack)
                }
            }
            document.closeMarkupInLayer(markup, l)
        }
        // for the last closing tag, close the markup for the default layer
        if (!layers.contains(DEFAULT_LAYER) && markup!!.layers.contains(DEFAULT_LAYER)) {
            val markupDeque = state.openMarkup[DEFAULT_LAYER]!!
            removeFromMarkupStack(extendedMarkupName, markupDeque)
            document.closeMarkupInLayer(markup, DEFAULT_LAYER)
        }
        val prefixNode = ctx.prefix()
        if (prefixNode != null) {
            val prefixNodeText = prefixNode.text
            if (prefixNodeText == OPTIONAL_PREFIX) {
                // optional
//                TODO()
            } else if (prefixNodeText == SUSPEND_PREFIX) {
                // suspend
                for (l in layers) {
                    state.suspendedMarkup.getOrPut(l) { ArrayDeque() }.add(markup)
                }
            }
        }
        state.eof = markup!!.dbId == state.rootMarkupId
        if (isSuspend && state.eof) {
            val rootMarkup = store.getMarkup(state.rootMarkupId)
            addBreakingError(ctx.getParent(), "The root markup %s cannot be suspended.", rootMarkup)
        }
        return markup
    }

    private fun addMarkup(
            extendedTag: String, atts: List<AnnotationContext>, ctx: ParserRuleContext): TAGMarkup {
        val markup = store.createMarkup(document, extendedTag)
        addAnnotations(atts, markup)
        document.addMarkup(markup)
        if (markup.hasMarkupId()) {
            //      identifiedMarkups.put(extendedTag, markup);
            val id = markup.markupId
            if (idsInUse.containsKey(id)) {
                addError(ctx, "Id '%s' was already used in markup [%s>.", id, idsInUse[id])
            }
            idsInUse[id] = extendedTag
        }
        return markup
    }

    private fun addAnnotations(annotationContexts: List<AnnotationContext>, markup: TAGMarkup) {
        for (actx: AnnotationContext in annotationContexts) {
            addAnnotation(markup, actx)
        }
    }

    private fun addAnnotation(markup: TAGMarkup, actx: AnnotationContext) {
        when (actx) {
            is BasicAnnotationContext -> {
                val aInfo = annotationFactory.makeAnnotation(actx)
                val markupNode = markup.dbId
                document.dto.textGraph.addAnnotationEdge(markupNode, aInfo)
            }
            is IdentifyingAnnotationContext -> {
                val id = actx.idValue().text
                markup.markupId = id
            }
            is RefAnnotationContext -> {
                val aName = actx.annotationName().text
                val refId = actx.refValue().text
                val annotationInfo = annotationFactory.makeReferenceAnnotation(aName, refId)
                val markupNode = markup.dbId
                document.dto.textGraph.addAnnotationEdge(markupNode, annotationInfo)
            }
        }
    }

    private fun linkTextToMarkupForLayer(tn: TAGTextNode, markup: TAGMarkup, layerName: String) {
        document.associateTextNodeWithMarkupForLayer(tn, markup, layerName)
    }

    private fun update(tagdto: TAGDTO): Long {
        return store.persist(tagdto)
    }

    private fun deduceLayers(
            ctx: MarkupNameContext, markupName: String, extendedMarkupName: String): Set<String> {
        val layerInfoContext = ctx.layerInfo()
        var layers = extractLayerInfo(layerInfoContext)
        val hasLayerInfo = layerInfoContext != null
        if (!hasLayerInfo) {
            val correspondingOpenMarkupList: List<TAGMarkup> = state.allOpenMarkup.stream().filter { m: TAGMarkup? -> m!!.hasTag(markupName) }.collect(Collectors.toList())
            if (correspondingOpenMarkupList.isEmpty()) {
                // nothing found? error!
                //        addError(ctx.getParent(), "Close tag <%s] found without corresponding open tag.",
                // extendedMarkupName);
            } else if (correspondingOpenMarkupList.size == 1) {
                // only one? then we found our corresponding start tag, and we can get the layer info from
                // this tag
                layers = correspondingOpenMarkupList[0].layers
            } else {
                // multiple open tags found? compare their layers
                val correspondingLayers = correspondingOpenMarkupList.stream()
                        .map { obj: TAGMarkup -> obj.layers }
                        .distinct()
                        .collect(Collectors.toList())
                if (correspondingLayers.size == 1) {
                    // all open tags have the same layer set (which could be empty (just the default layer))
                    layers = correspondingLayers[0]
                } else {
                    // not all open tags belong to the same sets of layers: ambiguous situation
                    addBreakingError(
                            ctx.getParent(),
                            "There are multiple start-tags that can correspond with end-tag <%s]; add layer information to the end-tag to solve this ambiguity.",
                            extendedMarkupName)
                }
            }
        }
        return layers
    }

    private fun checkForCorrespondingSuspendTag(
            ctx: StartTagContext, tag: String, markup: TAGMarkup?) {
        if (markup == null) {
            addBreakingError(
                    ctx,
                    "Resume tag %s found, which has no corresponding earlier suspend tag <%s%s].",
                    ctx.text,
                    SUSPEND_PREFIX,
                    tag)
        }
    }

    private fun withSuffix(ctx: MarkupNameContext, extendedMarkupName: String): String {
        var extendedMarkupName = extendedMarkupName
        val suffix = ctx.suffix()
        if (suffix != null) {
            extendedMarkupName += suffix.text
        }
        return extendedMarkupName
    }

    private fun withPrefix(ctx: MarkupNameContext, extendedMarkupName: String): String {
        var extendedMarkupName = extendedMarkupName
        val prefix = ctx.prefix()
        if (prefix != null && prefix.text == OPTIONAL_PREFIX) {
            extendedMarkupName = prefix.text + extendedMarkupName
        }
        return extendedMarkupName
    }

    private fun removeFromMarkupStack(extendedTag: String, markupStack: Deque<TAGMarkup>?): TAGMarkup? {
        if (markupStack == null || markupStack.isEmpty()) {
            return null
        }
        val expected = markupStack.peek()
        if (extendedTag == expected!!.extendedTag) {
            markupStack.pop()
            currentTextVariationState().removeOpenMarkup(expected)
            return expected
        }
        return null
    }

    private fun removeFromMarkupStack2(extendedTag: String, markupStack: Deque<TAGMarkup>): TAGMarkup? {
        val iterator: Iterator<TAGMarkup?> = markupStack.iterator()
        var markup: TAGMarkup? = null
        while (iterator.hasNext()) {
            markup = iterator.next()
            if (markup!!.extendedTag == extendedTag) {
                break
            }
            markup = null
        }
        if (markup != null) {
            markupStack.remove(markup)
            currentTextVariationState().removeOpenMarkup(markup)
        }
        return markup
    }

    private fun resumeMarkup(ctx: StartTagContext): TAGMarkup {
        val tag: String = ctx.markupName().text.replace(RESUME_PREFIX, "")
        var suspendedMarkup: TAGMarkup? = null
        val layers = extractLayerInfo(ctx.markupName().layerInfo())
        for (layer in layers) {
            suspendedMarkup = removeFromMarkupStack(tag, state.suspendedMarkup[layer]!!)
            checkForCorrespondingSuspendTag(ctx, tag, suspendedMarkup)
            checkForTextBetweenSuspendAndResumeTags(suspendedMarkup!!, ctx)
            suspendedMarkup.setIsDiscontinuous(true)
        }
        val textGraph = document.dto.textGraph
        val resumedMarkup = store.createMarkup(document, suspendedMarkup!!.tag).addAllLayers(layers)
        document.addMarkup(resumedMarkup)
        update(resumedMarkup.dto)
        textGraph.continueMarkup(suspendedMarkup, resumedMarkup)
        return resumedMarkup
    }

    private fun checkForTextBetweenSuspendAndResumeTags(
            suspendedMarkup: TAGMarkup, ctx: StartTagContext) {
        val previousTextNode = document.lastTextNode
        val previousMarkup = document.getMarkupStreamForTextNode(previousTextNode).collect(Collectors.toSet())
        if (previousMarkup.contains(suspendedMarkup)) {
            addError(
                    ctx,
                    "There is no text between this resume tag: %s and its corresponding suspend tag: %s. This is not allowed.",
                    resumeTag(suspendedMarkup),
                    suspendTag(suspendedMarkup))
        }
    }

    private fun checkEOF(ctx: ParserRuleContext) {
        if (state.eof) {
            val rootMarkup = store.getMarkup(state.rootMarkupId)
            addBreakingError(
                    ctx, "No text or markup allowed after the root markup %s has been ended.", rootMarkup)
        }
    }

    private fun tagNameIsValid(ctx: StartTagContext): Boolean {
        val layerInfoContext = ctx.markupName().layerInfo()
        val nameContext = ctx.markupName().name()
        return nameContextIsValid(ctx, nameContext, layerInfoContext)
    }

    private fun tagNameIsValid(ctx: EndTagContext): Boolean {
        val layerInfoContext = ctx.markupName().layerInfo()
        val nameContext = ctx.markupName().name()
        return nameContextIsValid(ctx, nameContext, layerInfoContext)
    }

    private fun tagNameIsValid(ctx: MilestoneTagContext): Boolean {
        val layerInfoContext = ctx.layerInfo()
        val nameContext = ctx.name()
        return nameContextIsValid(ctx, nameContext, layerInfoContext)
    }

    private fun nameContextIsValid(
            ctx: ParserRuleContext,
            nameContext: NameContext?,
            layerInfoContext: LayerInfoContext?): Boolean {
        val valid = AtomicBoolean(true)
        layerInfoContext?.layerName()?.stream()?.map { obj: LayerNameContext -> obj.text }?.forEach { lid: String? -> }
        if (nameContext == null || nameContext.text.isEmpty()) {
            addError(ctx, "Nameless markup is not allowed here.")
            valid.set(false)
        }
        return valid.get()
    }

    private fun currentTextVariationState(): TextVariationState =
            textVariationStateStack.peek()

    private fun openTag(m: TAGMarkup): String =
            OPEN_TAG_STARTCHAR + m.extendedTag + OPEN_TAG_ENDCHAR

    private fun closeTag(m: TAGMarkup): String =
            CLOSE_TAG_STARTCHAR + m.extendedTag + CLOSE_TAG_ENDCHAR

    private fun suspendTag(tagMarkup: TAGMarkup): String =
            CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX + tagMarkup.extendedTag + CLOSE_TAG_ENDCHAR

    private fun resumeTag(tagMarkup: TAGMarkup): String =
            OPEN_TAG_STARTCHAR + RESUME_PREFIX + tagMarkup.extendedTag + OPEN_TAG_ENDCHAR

    private fun logTextNode(textNode: TAGTextNode) {
        val dto = textNode.dto
        LOG.debug("TextNode(id={}, text=<{}>)", textNode.dbId, dto.text)
    }

    private fun extractLayerInfo(layerInfoContext: LayerInfoContext?): Set<String> {
        val layers: MutableSet<String> = HashSet()
        if (layerInfoContext != null) {
            val explicitLayers = layerInfoContext.layerName().map { it.text }
            layers.addAll(explicitLayers)
        }
        if (layers.isEmpty()) {
            layers.add(DEFAULT_LAYER)
        }
        return layers
    }

    private fun rangeOf(ctx: ParserRuleContext): Range =
            Range(
                    Position(ctx.start.line, ctx.start.charPositionInLine + 1),
                    Position(ctx.stop.line, ctx.stop.charPositionInLine + 2))

    companion object {
        private val LOG = LoggerFactory.getLogger(TAGMLListener::class.java)
        const val TILDE = "~"
        private val DEFAULT_LAYER_ONLY = setOf(DEFAULT_LAYER)
    }

    init {
        document = store.createDocument()
        textVariationStateStack.push(TextVariationState())
        annotationFactory = AnnotationFactory(store, document.dto.textGraph, errorListener)
    }
}
