package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGMarkupDAO;
import nl.knaw.huygens.alexandria.storage.TAGTextNodeDAO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.*;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;
import static nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*;

public class TAGMLListener extends TAGMLParserBaseListener {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListener.class);
  public static final String TILDE = "~";

  private final ErrorListener errorListener;
  private final HashMap<String, String> idsInUse = new HashMap<>();
  private final Map<String, String> namespaces = new HashMap<>();
  private State state = new State();
  private final Deque<State> stateStack = new ArrayDeque<>();
  private final Deque<TextVariationState> textVariationStateStack = new ArrayDeque<>();
  private static final Set<String> DEFAULT_LAYER_ONLY = singleton(TAGML.DEFAULT_LAYER);
  private boolean atDocumentStart = true;

  private TAGModelBuilder tagModelBuilder;

  public TAGMLListener(final TAGModelBuilder tagModelBuilder) {
    this.tagModelBuilder = tagModelBuilder;
    this.errorListener = tagModelBuilder.getErrorListener();
    this.textVariationStateStack.push(new TextVariationState());
  }

  public class State {
    public Map<String, Deque<TAGMarkupDAO>> openMarkup = new HashMap<>();
    public Map<String, Deque<TAGMarkupDAO>> suspendedMarkup = new HashMap();
    public Deque<TAGMarkupDAO> allOpenMarkup = new ArrayDeque<>();
    public Long rootMarkupId = null;
    public boolean eof = false;

    public State copy() {
      State copy = new State();
      copy.openMarkup = new HashMap<>();
      openMarkup.forEach((k, v) -> copy.openMarkup.put(k, new ArrayDeque<>(v)));
      copy.suspendedMarkup = new HashMap<>();
      suspendedMarkup.forEach((k, v) -> copy.suspendedMarkup.put(k, new ArrayDeque<>(v)));
      copy.allOpenMarkup = new ArrayDeque<>(allOpenMarkup);
      copy.rootMarkupId = rootMarkupId;
      copy.eof = eof;
      return copy;
    }

    public boolean rootMarkupIsSet() {
      return rootMarkupId != null;
    }
  }

  public class TextVariationState {
    public State startState;
    public List<State> endStates = new ArrayList<>();
    public TAGMarkupDAO startMarkup;
    public Map<Integer, List<TAGMarkupDAO>> openMarkup = new HashMap<>();
    public int branch = 0;

    public void addOpenMarkup(TAGMarkupDAO markup) {
      openMarkup.computeIfAbsent(branch, (b) -> new ArrayList<>());
      openMarkup.get(branch).add(markup);
    }

    public void removeOpenMarkup(TAGMarkupDAO markup) {
      openMarkup.computeIfAbsent(branch, (b) -> new ArrayList<>());
      openMarkup.get(branch).remove(markup);
    }
  }

  @Override
  public void exitDocument(DocumentContext ctx) {
    tagModelBuilder.exitDocument(namespaces);
    verifyNoMarkupUnclosed();
    verifyNoSuspendedMarkupLeft();
  }

  private void verifyNoSuspendedMarkupLeft() {
    boolean noSuspendedMarkup = state.suspendedMarkup.values().stream()
        .allMatch(Collection::isEmpty);
    if (!noSuspendedMarkup) {
      String suspendedMarkupString = state.suspendedMarkup.values()
          .stream()
          .flatMap(Collection::stream)//
          .map(this::suspendTag)//
          .distinct()
          .collect(joining(", "));
      errorListener.addError("Some suspended markup was not resumed: %s", suspendedMarkupString);
    }
  }

  private void verifyNoMarkupUnclosed() {
    boolean noOpenMarkup = state.openMarkup.values().stream()
        .allMatch(Collection::isEmpty);
    if (!noOpenMarkup) {
      String openRanges = state.openMarkup.values()
          .stream()
          .flatMap(Collection::stream)//
          .map(this::openTag)//
          .distinct()
          .collect(joining(", "));
      errorListener.addError(
          "Missing close tag(s) for: %s",
          openRanges
      );
    }
  }

  @Override
  public void exitNamespaceDefinition(NamespaceDefinitionContext ctx) {
    String ns = ctx.IN_NamespaceIdentifier().getText();
    String url = ctx.IN_NamespaceURI().getText();
    namespaces.put(ns, url);
  }

  @Override
  public void exitText(TextContext ctx) {
    String text = unEscape(ctx.getText());
//    LOG.debug("text=<{}>", text);
    atDocumentStart = atDocumentStart && StringUtils.isBlank(text);
    // TODO: smarter whitespace handling
    boolean useText = !atDocumentStart /*&& !StringUtils.isBlank(text)*/;
    if (useText) {
      if (StringUtils.isNotBlank(text)) {
        checkEOF(ctx);
      }
      if (!state.rootMarkupIsSet()) {
        errorListener.addBreakingError(
            "%s No text allowed here, the root markup must be started first.",
            errorPrefix(ctx));
      }
      tagModelBuilder.createConnectedTextNode(text, state.allOpenMarkup);
    }
  }

  @Override
  public void enterStartTag(StartTagContext ctx) {
    checkEOF(ctx);
    if (tagNameIsValid(ctx)) {
      MarkupNameContext markupNameContext = ctx.markupName();
      String markupName = markupNameContext.name().getText();
//      LOG.debug("startTag.markupName=<{}>", markupName);
      checkNameSpace(ctx, markupName);
      ctx.annotation()
          .forEach(annotation -> LOG.debug("  startTag.annotation={{}}", annotation.getText()));

      PrefixContext prefix = markupNameContext.prefix();
      boolean optional = prefix != null && prefix.getText().equals(OPTIONAL_PREFIX);
      boolean resume = prefix != null && prefix.getText().equals(RESUME_PREFIX);

      TAGMarkupDAO markup = resume
          ? resumeMarkup(ctx)
          : addMarkup(markupName, ctx.annotation(), ctx).setOptional(optional);

      Set<String> layerIds = extractLayerInfo(ctx.markupName().layerInfo());
      Set<String> layers = new HashSet<>();
      state.allOpenMarkup.push(markup);

      boolean firstTag = tagModelBuilder.isFirstTag();
      if (firstTag) {
        addDefaultLayer(markup, layers);
        state.rootMarkupId = markup.getDbId();
      }
      layerIds.forEach(layerId -> {
        if (layerId.contains("+")) {
          String[] parts = layerId.split("\\+");
          String parentLayer = parts[0];
          String newLayerId = parts[1];
          tagModelBuilder.addLayer(newLayerId, markup, parentLayer);
          layers.add(newLayerId);

        } else if (!(firstTag && DEFAULT_LAYER.equals(layerId))) {
          checkLayerWasAdded(ctx, layerId);
          checkLayerIsOpen(ctx, layerId);
          tagModelBuilder.openMarkupInLayer(markup, layerId);
          layers.add(layerId);
        }
      });
      markup.addAllLayers(layers);

      addSuffix(markupNameContext, markup);
      markup.getLayers().forEach(l -> {
        state.openMarkup.putIfAbsent(l, new ArrayDeque<>());
        state.openMarkup.get(l).push(markup);
      });

      currentTextVariationState().addOpenMarkup(markup);
      tagModelBuilder.persist(markup);
    }
  }

  @Override
  public void enterRichTextValue(final RichTextValueContext ctx) {
    stateStack.push(state);
    state = new State();
    tagModelBuilder.enterRichTextValue();
    super.enterRichTextValue(ctx);
  }

  @Override
  public void exitRichTextValue(final RichTextValueContext ctx) {
    super.exitRichTextValue(ctx);
    state = stateStack.pop();
    tagModelBuilder.exitRichTextValue();
  }

  private void addSuffix(final MarkupNameContext markupNameContext, final TAGMarkupDAO markup) {
    SuffixContext suffix = markupNameContext.suffix();
    if (suffix != null) {
      String id = suffix.getText().replace(TILDE, "");
      markup.setSuffix(id);
    }
  }

  private void checkLayerIsOpen(final StartTagContext ctx, final String layerId) {
    if (state.openMarkup.get(layerId).isEmpty()) {
      String layer = layerId.isEmpty() ? "the default layer" : "layer '" + layerId + "'";
      errorListener.addBreakingError(
          "%s %s cannot be used here, since the root markup of this layer has closed already.",
          errorPrefix(ctx), layer);
    }
  }

  private void checkLayerWasAdded(final StartTagContext ctx, final String layerId) {
    if (!state.openMarkup.containsKey(layerId)) {
      errorListener.addBreakingError(
          "%s Layer %s has not been added at this point, use +%s to add a layer.",
          errorPrefix(ctx, true), layerId, layerId);
    }
  }

  private void checkNameSpace(final StartTagContext ctx, final String markupName) {
    if (markupName.contains(":")) {
      String namespace = markupName.split(":", 2)[0];
      if (!namespaces.containsKey(namespace)) {
        errorListener.addError(
            "%s Namespace %s has not been defined.",
            errorPrefix(ctx), namespace
        );
      }
    }
  }

  private void addDefaultLayer(final TAGMarkupDAO markup, final Set<String> layers) {
    tagModelBuilder.addLayer(TAGML.DEFAULT_LAYER, markup, null);
    layers.add(TAGML.DEFAULT_LAYER);
  }

  @Override
  public void exitEndTag(EndTagContext ctx) {
    checkEOF(ctx);
    if (tagNameIsValid(ctx)) {
      String markupName = ctx.markupName().name().getText();
//      LOG.debug("endTag.markupName=<{}>", markupName);
      removeFromOpenMarkup(ctx.markupName());
    }
  }

  @Override
  public void exitMilestoneTag(MilestoneTagContext ctx) {
    if (!state.rootMarkupIsSet()) {
      errorListener.addBreakingError(
          "%s The root markup cannot be a milestone tag.",
          errorPrefix(ctx));
    }
    if (tagNameIsValid(ctx)) {
      String markupName = ctx.name().getText();
//      LOG.debug("milestone.markupName=<{}>", markupName);
      ctx.annotation()
          .forEach(annotation -> LOG.debug("milestone.annotation={{}}", annotation.getText()));
      Set<String> layers = extractLayerInfo(ctx.layerInfo());
      final TAGTextNodeDAO tn = tagModelBuilder.createConnectedTextNode("", state.allOpenMarkup);
//      logTextNode(tn);
      TAGMarkupDAO markup = addMarkup(ctx.name().getText(), ctx.annotation(), ctx);
      markup.addAllLayers(layers);
      layers.forEach(layerName -> {
        tagModelBuilder.associateTextNodeWithMarkupForLayer(tn, markup, layerName);
        tagModelBuilder.openMarkupInLayer(markup, layerName);
        tagModelBuilder.closeMarkupInLayer(markup, layerName);
      });
      tagModelBuilder.persist(markup);
    }
  }

  @Override
  public void enterTextVariation(final TextVariationContext ctx) {
    checkEOF(ctx);

//    LOG.debug("<| lastTextNodeInTextVariationStack.size()={}",lastTextNodeInTextVariationStack.size());

    TAGMarkupDAO branches = openTextVariationMarkup(BRANCHES, DEFAULT_LAYER_ONLY);

    TextVariationState textVariationState = new TextVariationState();
    textVariationState.startMarkup = branches;
    textVariationState.startState = state.copy();
    textVariationState.branch = 0;
    textVariationStateStack.push(textVariationState);
    openTextVariationMarkup(BRANCH, DEFAULT_LAYER_ONLY);
  }

  private TAGMarkupDAO openTextVariationMarkup(final String tagName, final Set<String> layers) {
    TAGMarkupDAO markup = tagModelBuilder.addMarkup(tagName);
    markup.addAllLayers(layers);

    state.allOpenMarkup.push(markup);
    markup.getLayers().forEach(l -> {
      tagModelBuilder.openMarkupInLayer(markup, l);
      state.openMarkup.putIfAbsent(l, new ArrayDeque<>());
      state.openMarkup.get(l).push(markup);
    });

    currentTextVariationState().addOpenMarkup(markup);
    tagModelBuilder.persist(markup);
    return markup;
  }

  @Override
  public void exitTextVariationSeparator(final TextVariationSeparatorContext ctx) {
    checkEOF(ctx);
    closeSystemMarkup(BRANCH, DEFAULT_LAYER_ONLY);
    checkForOpenMarkupInBranch(ctx);

    currentTextVariationState().endStates.add(state.copy());
    currentTextVariationState().branch += 1;
    state = currentTextVariationState().startState.copy();
    openTextVariationMarkup(BRANCH, DEFAULT_LAYER_ONLY);
  }

  private void closeTextVariationMarkup(final String extendedMarkupName, final Set<String> layers) {
    removeFromMarkupStack2(extendedMarkupName, state.allOpenMarkup);
    TAGMarkupDAO markup;
    for (String l : layers) {
      state.openMarkup.putIfAbsent(l, new ArrayDeque<>());
      Deque<TAGMarkupDAO> markupStack = state.openMarkup.get(l);
      markup = removeFromMarkupStack2(extendedMarkupName, markupStack);
      tagModelBuilder.closeMarkupInLayer(markup, l);
    }
  }

  private void checkForOpenMarkupInBranch(final ParserRuleContext ctx) {
    int branch = currentTextVariationState().branch + 1;
    Map<String, Deque<TAGMarkupDAO>> openMarkupAtStart = currentTextVariationState().startState.openMarkup;
    Map<String, Deque<TAGMarkupDAO>> currentOpenMarkup = state.openMarkup;
    for (final String layerName : openMarkupAtStart.keySet()) {
      Deque<TAGMarkupDAO> openMarkupAtStartInLayer = openMarkupAtStart.get(layerName);
      Deque<TAGMarkupDAO> currentOpenMarkupInLayer = currentOpenMarkup.get(layerName);
      List<TAGMarkupDAO> closedInBranch = new ArrayList<>(openMarkupAtStartInLayer);
      closedInBranch.removeAll(currentOpenMarkupInLayer);
      if (!closedInBranch.isEmpty()) {
        String openTags = closedInBranch.stream().map(this::openTag).collect(joining(","));
        errorListener.addBreakingError(
            "%s Markup %s opened before branch %s, should not be closed in a branch.",
            errorPrefix(ctx), openTags, branch);
      }
      List<TAGMarkupDAO> openedInBranch = new ArrayList<>(currentOpenMarkupInLayer);
      openedInBranch.removeAll(openMarkupAtStartInLayer);
      String openTags = openedInBranch.stream()
          .filter(m -> !m.getTag().startsWith(":"))
          .map(this::openTag)
          .collect(joining(","));
      if (!openTags.isEmpty()) {
        errorListener.addBreakingError(
            "%s Markup %s opened in branch %s must be closed before starting a new branch.",
            errorPrefix(ctx), openTags, branch);
      }
    }
  }

  @Override
  public void exitTextVariation(final TextVariationContext ctx) {
    checkEOF(ctx);
    closeSystemMarkup(BRANCH, DEFAULT_LAYER_ONLY);
    checkForOpenMarkupInBranch(ctx);
    closeSystemMarkup(BRANCHES, DEFAULT_LAYER_ONLY);
    currentTextVariationState().endStates.add(state.copy());
    checkEndStates(ctx);
    if (errorListener.hasErrors()) { // TODO: check if a breaking error should have been set earlier
      return;
    }
    textVariationStateStack.pop();
  }

  private void closeSystemMarkup(String tag, Set<String> layers) {
    for (String l : layers) {
      String suffix = TAGML.DEFAULT_LAYER.equals(l)
          ? ""
          : "|" + l;
      Set<String> layer = new HashSet<>();
      layer.add(l);
      closeTextVariationMarkup(tag + suffix, layer);
    }
  }

  private Set<String> getOpenLayers() {
    return getRelevantOpenMarkup().stream()
        .map(TAGMarkupDAO::getLayers)
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  private List<TAGMarkupDAO> getRelevantOpenMarkup() {
    return tagModelBuilder.getRelevantOpenMarkup(state.allOpenMarkup);
  }

  private void checkEndStates(final TextVariationContext ctx) {
    List<List<String>> suspendedMarkupInBranch = new ArrayList<>();
    List<List<String>> resumedMarkupInBranch = new ArrayList<>();

    List<List<String>> openedMarkupInBranch = new ArrayList<>();
    List<List<String>> closedMarkupInBranch = new ArrayList<>();

    State startState = currentTextVariationState().startState;
//    Map<String, Deque<TAGMarkup>> suspendedMarkupBeforeDivergence = startState.suspendedMarkup;
//    Map<String, Deque<TAGMarkup>> openMarkupBeforeDivergence = startState.openMarkup;

//    currentTextVariationState().endStates.forEach(state -> {
//      List<String> suspendedMarkup = state.suspendedMarkup.stream()
//          .filter(m -> !suspendedMarkupBeforeDivergence.contains(m))
//          .map(this::suspendTag)
//          .collect(toList());
//      suspendedMarkupInBranch.add(suspendedMarkup);
//
//      // TODO: resumedMarkup
//
//      List<String> openedInBranch = state.openMarkup.stream()
//          .filter(m -> !openMarkupBeforeDivergence.contains(m))
//          .map(this::openTag)
//          .collect(toList());
//      openedMarkupInBranch.add(openedInBranch);
//
//      List<String> closedInBranch = openMarkupBeforeDivergence.stream()
//          .filter(m -> !state.openMarkup.contains(m))
//          .map(this::closeTag)
//          .collect(toList());
//      closedMarkupInBranch.add(closedInBranch);
//    });

    String errorPrefix = errorPrefix(ctx, true);
    checkSuspendedOrResumedMarkupBetweenBranches(suspendedMarkupInBranch, resumedMarkupInBranch, errorPrefix);
    checkOpenedOrClosedMarkupBetweenBranches(openedMarkupInBranch, closedMarkupInBranch, errorPrefix);
  }

  private void checkSuspendedOrResumedMarkupBetweenBranches(final List<List<String>> suspendedMarkupInBranch, final List<List<String>> resumedMarkupInBranch, final String errorPrefix) {
    Set<List<String>> suspendedMarkupSet = new HashSet<>(suspendedMarkupInBranch);
    if (suspendedMarkupSet.size() > 1) {
      StringBuilder branchLines = new StringBuilder();
      for (int i = 0; i < suspendedMarkupInBranch.size(); i++) {
        List<String> suspendedMarkup = suspendedMarkupInBranch.get(i);
        String has = suspendedMarkup.isEmpty() ? "no suspended markup." : "suspended markup " + suspendedMarkup + ".";
        branchLines.append("\n\tbranch ")
            .append(i + 1)
            .append(" has ")
            .append(has);
      }
      errorListener.addBreakingError(
          "%s There is a discrepancy in suspended markup between branches:%s",
          errorPrefix, branchLines);
    }
  }

  private void checkOpenedOrClosedMarkupBetweenBranches(final List<List<String>> openedMarkupInBranch, final List<List<String>> closedMarkupInBranch, final String errorPrefix) {
    Set<List<String>> branchMarkupSet = new HashSet<>(openedMarkupInBranch);
    branchMarkupSet.addAll(closedMarkupInBranch);
    if (branchMarkupSet.size() > 2) {
      StringBuilder branchLines = new StringBuilder();
      for (int i = 0; i < openedMarkupInBranch.size(); i++) {
        String closed = String.join(", ", closedMarkupInBranch.get(i));
        String closedStatement = closed.isEmpty()
            ? "didn't close any markup"
            : "closed markup " + closed;
        String opened = String.join(", ", openedMarkupInBranch.get(i));
        String openedStatement = opened.isEmpty()
            ? "didn't open any new markup"
            : "opened markup " + opened;
        branchLines.append("\n\tbranch ")
            .append(i + 1)
            .append(" ")
            .append(closedStatement)
            .append(" that was opened before the ")
            .append(DIVERGENCE)
            .append(" and ")
            .append(openedStatement)
            .append(" to be closed after the ")
            .append(CONVERGENCE);
      }
      errorListener.addBreakingError(
          "%s There is an open markup discrepancy between the branches:%s",
          errorPrefix, branchLines);
    }
  }

  private TAGMarkupDAO addMarkup(String extendedTag, List<AnnotationContext> atts, ParserRuleContext ctx) {
    TAGMarkupDAO markup = tagModelBuilder.createMarkup(extendedTag);
    addAnnotations(atts, markup);
    tagModelBuilder.addMarkup(markup);
    if (markup.hasMarkupId()) {
//      identifiedMarkups.put(extendedTag, markup);
      String id = markup.getMarkupId();
      if (idsInUse.containsKey(id)) {
        errorListener.addError(
            "%s Id '%s' was already used in markup [%s>.",
            errorPrefix(ctx), id, idsInUse.get(id));
      }
      idsInUse.put(id, extendedTag);
    }
    return markup;
  }

  private void addAnnotations(List<AnnotationContext> annotationContexts, TAGMarkupDAO markup) {
    annotationContexts.forEach(actx -> addAnnotation(markup, actx));
  }

  private void addAnnotation(final TAGMarkupDAO markup, final AnnotationContext actx) {
    if (actx instanceof BasicAnnotationContext) {
      tagModelBuilder.addBasicAnnotation(markup, (BasicAnnotationContext) actx);

    } else if (actx instanceof IdentifyingAnnotationContext) {
      IdentifyingAnnotationContext idAnnotationContext = (IdentifyingAnnotationContext) actx;
      String id = idAnnotationContext.idValue().getText();
      markup.setMarkupId(id);

    } else if (actx instanceof RefAnnotationContext) {
      RefAnnotationContext refAnnotationContext = (RefAnnotationContext) actx;
      String aName = refAnnotationContext.annotationName().getText();
      String refId = refAnnotationContext.refValue().getText();
      tagModelBuilder.addRefAnnotation(markup, aName, refId);
    }
  }

  private TAGMarkupDAO removeFromOpenMarkup(MarkupNameContext ctx) {
    String markupName = ctx.name().getText();
    String extendedMarkupName = markupName;
    extendedMarkupName = withPrefix(ctx, extendedMarkupName);
    extendedMarkupName = withSuffix(ctx, extendedMarkupName);

    boolean isSuspend = ctx.prefix() != null
        && ctx.prefix().getText().equals(TAGML.SUSPEND_PREFIX);

    Set<String> layers = deduceLayers(ctx, markupName, extendedMarkupName);

    boolean layerSuffixNeeded = !(layers.size() == 1
        && layers.iterator().next().equals(TAGML.DEFAULT_LAYER));
    String foundLayerSuffix = layerSuffixNeeded
        ? TAGML.DIVIDER + layers.stream()
        .filter(l -> !TAGML.DEFAULT_LAYER.equals(l))
        .sorted()
        .collect(joining(","))
        : "";

    extendedMarkupName += foundLayerSuffix;
    removeFromMarkupStack2(extendedMarkupName, state.allOpenMarkup);
    TAGMarkupDAO markup = null;
    for (String l : layers) {
      state.openMarkup.putIfAbsent(l, new ArrayDeque<>());
      Deque<TAGMarkupDAO> markupStack = state.openMarkup.get(l);
      markup = removeFromMarkupStack(extendedMarkupName, markupStack);
      if (markup == null) {
        AtomicReference<String> emn = new AtomicReference<>(extendedMarkupName);
        boolean markupIsOpen = markupStack.stream()
            .map(TAGMarkupDAO::getExtendedTag)
            .anyMatch(et -> emn.get().equals(et));
        if (!markupIsOpen) {
          errorListener.addError(
              "%s Close tag <%s] found without corresponding open tag.",
              errorPrefix(ctx), extendedMarkupName
          );
          return null;
        } else if (!isSuspend) {
          TAGMarkupDAO expected = markupStack.peek();
          if (expected.hasTag(BRANCH)) {
            errorListener.addBreakingError(
                "%s Markup [%s> opened before branch %s, should not be closed in a branch.",
                errorPrefix(ctx), extendedMarkupName, currentTextVariationState().branch + 1);
          }
          String hint = l.isEmpty() ? " Use separate layers to allow for overlap." : "";
          errorListener.addBreakingError(
              "%s Close tag <%s] found, expected %s.%s",
              errorPrefix(ctx), extendedMarkupName, closeTag(expected), hint
          );
          return null;
        } else {
          markup = removeFromMarkupStack2(extendedMarkupName, markupStack);
        }
      }
      tagModelBuilder.closeMarkupInLayer(markup, l);
    }
    // for the last closing tag, close the markup for the default layer
    if (!layers.contains(DEFAULT_LAYER) && markup.getLayers().contains(DEFAULT_LAYER)) {
      Deque<TAGMarkupDAO> markupDeque = state.openMarkup.get(DEFAULT_LAYER);
      removeFromMarkupStack(extendedMarkupName, markupDeque);
      tagModelBuilder.closeMarkupInLayer(markup, DEFAULT_LAYER);
    }

    PrefixContext prefixNode = ctx.prefix();
    if (prefixNode != null) {
      String prefixNodeText = prefixNode.getText();
      if (prefixNodeText.equals(OPTIONAL_PREFIX)) {
        // optional
        // TODO

      } else if (prefixNodeText.equals(SUSPEND_PREFIX)) {
        // suspend
        for (String l : layers) {
          state.suspendedMarkup.putIfAbsent(l, new ArrayDeque<>());
          state.suspendedMarkup.get(l).add(markup);
        }
      }
    }
    state.eof = (markup.getDbId().equals(state.rootMarkupId));
    if (isSuspend && state.eof) {
      TAGMarkupDAO rootMarkup = tagModelBuilder.getMarkup(state.rootMarkupId);
      errorListener.addBreakingError(
          "%s The root markup %s cannot be suspended.",
          errorPrefix(ctx), rootMarkup);
    }
    return markup;
  }

  private Set<String> deduceLayers(final MarkupNameContext ctx, final String markupName, final String extendedMarkupName) {
    LayerInfoContext layerInfoContext = ctx.layerInfo();
    Set<String> layers = extractLayerInfo(layerInfoContext);
    boolean hasLayerInfo = (layerInfoContext != null);
    if (!hasLayerInfo) {
      List<TAGMarkupDAO> correspondingOpenMarkupList = state.allOpenMarkup.stream()
          .filter(m -> m.hasTag(markupName))
          .collect(toList());
      if (correspondingOpenMarkupList.isEmpty()) {
        // nothing found? error!
        errorListener.addBreakingError(
            "%s Close tag <%s] found without corresponding open tag.",
            errorPrefix(ctx), extendedMarkupName);

      } else if (correspondingOpenMarkupList.size() == 1) {
        // only one? then we found our corresponding start tag, and we can get the layer info from this tag
        layers = correspondingOpenMarkupList.get(0).getLayers();

      } else {
        // multiple open tags found? compare their layers
        List<Set<String>> correspondingLayers = correspondingOpenMarkupList.stream()
            .map(TAGMarkupDAO::getLayers)
            .distinct()
            .collect(toList());
        if (correspondingLayers.size() == 1) {
          // all open tags have the same layer set (which could be empty (just the default layer))
          layers = correspondingLayers.get(0);

        } else {
          // not all open tags belong to the same sets of layers: ambiguous situation
          errorListener.addBreakingError(
              "%s There are multiple start-tags that can correspond with end-tag <%s]; add layer information to the end-tag to solve this ambiguity.",
              errorPrefix(ctx), extendedMarkupName);
        }
      }
    }

    return layers;
  }

  private String withSuffix(final MarkupNameContext ctx, String extendedMarkupName) {
    SuffixContext suffix = ctx.suffix();
    if (suffix != null) {
      extendedMarkupName += suffix.getText();
    }
    return extendedMarkupName;
  }

  private String withPrefix(final MarkupNameContext ctx, String extendedMarkupName) {
    PrefixContext prefix = ctx.prefix();
    if (prefix != null && prefix.getText().equals(OPTIONAL_PREFIX)) {
      extendedMarkupName = prefix.getText() + extendedMarkupName;
    }
    return extendedMarkupName;
  }

  private TAGMarkupDAO removeFromMarkupStack(String extendedTag, Deque<TAGMarkupDAO> markupStack) {
    if (markupStack == null || markupStack.isEmpty()) {
      return null;
    }
    final TAGMarkupDAO expected = markupStack.peek();
    if (extendedTag.equals(expected.getExtendedTag())) {
      markupStack.pop();
      currentTextVariationState().removeOpenMarkup(expected);
      return expected;
    }
    return null;
  }

  private TAGMarkupDAO removeFromMarkupStack2(String extendedTag, Deque<TAGMarkupDAO> markupStack) {
    Iterator<TAGMarkupDAO> iterator = markupStack.iterator();
    TAGMarkupDAO markup = null;
    while (iterator.hasNext()) {
      markup = iterator.next();
      if (markup.getExtendedTag().equals(extendedTag)) {
        break;
      }
      markup = null;
    }
    if (markup != null) {
      markupStack.remove(markup);
      currentTextVariationState().removeOpenMarkup(markup);
    }
    return markup;
  }

  private TAGMarkupDAO resumeMarkup(StartTagContext ctx) {
    String tag = ctx.markupName().getText().replace(RESUME_PREFIX, "");
    TAGMarkupDAO suspendedMarkup = null;
    Set<String> layers = extractLayerInfo(ctx.markupName().layerInfo());
    for (String layer : layers) {
      suspendedMarkup = removeFromMarkupStack(tag, state.suspendedMarkup.get(layer));
      checkForCorrespondingSuspendTag(ctx, tag, suspendedMarkup);
      checkForTextBetweenSuspendAndResumeTags(suspendedMarkup, ctx);
      suspendedMarkup.setIsDiscontinuous(true);
    }
    return tagModelBuilder.resumeMarkup(suspendedMarkup, layers);
  }

  private void checkForCorrespondingSuspendTag(final StartTagContext ctx, final String tag,
      final TAGMarkupDAO markup) {
    if (markup == null) {
      errorListener.addBreakingError(
          "%s Resume tag %s found, which has no corresponding earlier suspend tag <%s%s].",
          errorPrefix(ctx), ctx.getText(), SUSPEND_PREFIX, tag
      );
    }
  }

  private void checkForTextBetweenSuspendAndResumeTags(final TAGMarkupDAO suspendedMarkup, final StartTagContext ctx) {
    final TAGTextNodeDAO previousTextNode = tagModelBuilder.getLastTextNode();
    Set<TAGMarkupDAO> previousMarkup = tagModelBuilder.getMarkupStreamForTextNode(previousTextNode).collect(toSet());
    if (previousMarkup.contains(suspendedMarkup)) {
      errorListener.addBreakingError(
          "%s There is no text between this resume tag: %s and its corresponding suspend tag: %s. This is not allowed.",
          errorPrefix(ctx), resumeTag(suspendedMarkup), suspendTag(suspendedMarkup)
      );
    }
  }

  private boolean tagNameIsValid(final StartTagContext ctx) {
    LayerInfoContext layerInfoContext = ctx.markupName().layerInfo();
    NameContext nameContext = ctx.markupName().name();
    return nameContextIsValid(ctx, nameContext, layerInfoContext);
  }

  private boolean tagNameIsValid(final EndTagContext ctx) {
    LayerInfoContext layerInfoContext = ctx.markupName().layerInfo();
    NameContext nameContext = ctx.markupName().name();
    return nameContextIsValid(ctx, nameContext, layerInfoContext);
  }

  private boolean tagNameIsValid(final MilestoneTagContext ctx) {
    LayerInfoContext layerInfoContext = ctx.layerInfo();
    NameContext nameContext = ctx.name();
    return nameContextIsValid(ctx, nameContext, layerInfoContext);
  }

  private boolean nameContextIsValid(final ParserRuleContext ctx,
      final NameContext nameContext, final LayerInfoContext layerInfoContext) {
    AtomicBoolean valid = new AtomicBoolean(true);
    if (layerInfoContext != null) {
      layerInfoContext.layerName().stream()
          .map(LayerNameContext::getText)
          .forEach(lid -> {
//            if (!document.getLayerNames().contains(lid)) {
//              valid.set(false);
//              errorListener.addError(
//                  "%s Layer %s is undefined at this point.",
//                  errorPrefix(ctx), lid);
//            }
          });
    }

    if (nameContext == null || nameContext.getText().isEmpty()) {
      errorListener.addError(
          "%s Nameless markup is not allowed here.",
          errorPrefix(ctx)
      );
      valid.set(false);
    }
    return valid.get();
  }

  private TextVariationState currentTextVariationState() {
    return textVariationStateStack.peek();
  }

  private String openTag(final TAGMarkupDAO m) {
    return OPEN_TAG_STARTCHAR + m.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String closeTag(final TAGMarkupDAO m) {
    return CLOSE_TAG_STARTCHAR + m.getExtendedTag() + CLOSE_TAG_ENDCHAR;
  }

  private String suspendTag(TAGMarkupDAO tagMarkupDAO) {
    return CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX + tagMarkupDAO.getExtendedTag() + CLOSE_TAG_ENDCHAR;
  }

  private String resumeTag(TAGMarkupDAO tagMarkupDAO) {
    return OPEN_TAG_STARTCHAR + RESUME_PREFIX + tagMarkupDAO.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String errorPrefix(ParserRuleContext ctx) {
    return errorPrefix(ctx, false);
  }

  private String errorPrefix(ParserRuleContext ctx, boolean useStopToken) {
    Token token = useStopToken ? ctx.stop : ctx.start;
    return format("line %d:%d :", token.getLine(), token.getCharPositionInLine() + 1);
  }

  private void logTextNode(final TAGTextNodeDAO textNode) {
    TAGTextNode dto = textNode.getDTO();
    LOG.debug("TextNode(id={}, text=<{}>)",
        textNode.getDbId(),
        dto.getText()
    );
  }

  private Set<String> extractLayerInfo(final LayerInfoContext layerInfoContext) {
    final Set<String> layers = new HashSet<>();
    if (layerInfoContext != null) {
      List<String> explicitLayers = layerInfoContext.layerName()
          .stream()
          .map(LayerNameContext::getText)
          .collect(toList());
      layers.addAll(explicitLayers);
    }
    if (layers.isEmpty()) {
      layers.add(TAGML.DEFAULT_LAYER);
    }
    return layers;
  }

  private void checkEOF(final ParserRuleContext ctx) {
    if (state.eof) {
      TAGMarkupDAO rootMarkup = tagModelBuilder.getMarkup(state.rootMarkupId);
      errorListener.addBreakingError(
          "%s No text or markup allowed after the root markup %s has been ended.",
          errorPrefix(ctx), rootMarkup);
    }
  }

}
