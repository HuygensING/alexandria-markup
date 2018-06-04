package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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
import nl.knaw.huygens.alexandria.storage.*;
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;
import static nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.convergence;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.divergence;

public class TAGMLListener2 extends TAGMLParserBaseListener {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListener2.class);
  public static final String TILDE = "~";

  private final TAGStore store;
  private final TAGDocument document;
  private final ErrorListener errorListener;
  private final HashMap<String, String> idsInUse = new HashMap<>();
  private final Map<String, String> namespaces = new HashMap<>();
  private State state = new State();

  private final Deque<TextVariationState> textVariationStateStack = new ArrayDeque<>();

  private boolean atDocumentStart = true;
  private TAGTextNode previousTextNode = null;

  public TAGMLListener2(final TAGStore store, ErrorListener errorListener) {
    this.store = store;
    this.document = store.createDocument();
    this.errorListener = errorListener;
    this.textVariationStateStack.push(new TextVariationState());
  }

  public TAGDocument getDocument() {
    return document;
  }

  public class State {
    public Map<String, Deque<TAGMarkup>> openMarkup = new HashMap<>();
    public Map<String, Deque<TAGMarkup>> suspendedMarkup = new HashMap();

    public State copy() {
      State copy = new State();
      copy.openMarkup = new HashMap<>(openMarkup);
      copy.suspendedMarkup = new HashMap<>(suspendedMarkup);
      return copy;
    }
  }

  public class TextVariationState {
    public State startState;
    public List<State> endStates = new ArrayList<>();
    public TAGTextNode startNode;
    public List<TAGTextNode> endNodes = new ArrayList<>();
    public Map<Integer, List<TAGMarkup>> openMarkup = new HashMap<>();
    public int branch = 0;

    public void addOpenMarkup(TAGMarkup markup) {
      openMarkup.computeIfAbsent(branch, (b) -> new ArrayList<>());
      openMarkup.get(branch).add(markup);
    }

    public void removeOpenMarkup(TAGMarkup markup) {
      openMarkup.computeIfAbsent(branch, (b) -> new ArrayList<>());
      openMarkup.get(branch).remove(markup);
    }
  }

  @Override
  public void exitDocument(DocumentContext ctx) {
    update(document.getDTO());
    boolean noOpenMarkup = state.openMarkup.values().stream().allMatch(Collection::isEmpty);
    if (!noOpenMarkup) {
      String openRanges = state.openMarkup.values().stream().flatMap(Collection::stream)//
          .map(this::openTag)//
          .distinct()
          .collect(joining(", "));
      errorListener.addError(
          "Missing close tag(s) for: %s",
          openRanges
      );
    }
    boolean noSuspendedMarkup = state.suspendedMarkup.values().stream().allMatch(Collection::isEmpty);
    if (!noSuspendedMarkup) {
      String suspendedMarkupString = state.suspendedMarkup.values().stream().flatMap(Collection::stream)//
          .map(this::suspendTag)//
          .distinct()
          .collect(Collectors.joining(", "));
      errorListener.addError("Some suspended markup was not resumed: %s", suspendedMarkupString);
    }
  }

  @Override
  public void exitNamespaceDefinition(NamespaceDefinitionContext ctx) {
    String ns = ctx.IN_NamespaceIdentifier().getText();
    String url = ctx.IN_NamespaceURI().getText();
    namespaces.put(ns, url);
  }

//  @Override
//  public void exitLayerDefinition(LayerDefinitionContext ctx) {
//    String id = ctx.ILD_LayerIdentifier().getText();
//    String description = ctx.ILD_QuotedLayerDescription().getText()
//        .replaceFirst("^\"", "")
//        .replaceFirst("\"$", "")
//        .replaceAll("\\\"", "\"");
//    layerInfo.put(id, description);
//    document.addLayerId(id);
//  }

  @Override
  public void exitText(TextContext ctx) {
    String text = unEscape(ctx.getText());
//    LOG.debug("text=<{}>", text);
    atDocumentStart = atDocumentStart && StringUtils.isBlank(text);
    if (!atDocumentStart) {
      TAGTextNode tn = store.createTextNode(text);
      if (previousTextNode != null) {
        tn.addPreviousTextNode(previousTextNode);
      }
      previousTextNode = tn;
      document.addTextNode(tn);
//      state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
      logTextNode(tn);
    }
  }

  @Override
  public void enterStartTag(StartTagContext ctx) {
    // determine layer
    // open markup in layer
    // add markup to layer tree as child of layer.openmarkupstack.peek()
    // push markup to layer.openmarkupstack()

    if (tagNameIsValid(ctx)) {
      MarkupNameContext markupNameContext = ctx.markupName();
      String markupName = markupNameContext.name().getText();
      LOG.debug("startTag.markupName=<{}>", markupName);
      if (markupName.contains(":")) {
        String namespace = markupName.split(":", 2)[0];
        if (!namespaces.containsKey(namespace)) {
          errorListener.addError(
              "%s Namespace %s has not been defined.",
              errorPrefix(ctx), namespace
          );
        }
      }
      ctx.annotation()
          .forEach(annotation -> LOG.debug("  startTag.annotation={{}}", annotation.getText()));

      PrefixContext prefix = markupNameContext.prefix();
      boolean optional = prefix != null && prefix.getText().equals(OPTIONAL_PREFIX);
      boolean resume = prefix != null && prefix.getText().equals(RESUME_PREFIX);

      TAGMarkup markup = resume
          ? resumeMarkup(ctx)
          : addMarkup(markupName, ctx.annotation(), ctx).setOptional(optional);

      Set<String> layerIds = extractLayerInfo(ctx.markupName().layerInfo());
      Set<String> layers = new HashSet<>();
      layerIds.forEach(layerId -> {
        if (layerId.equals("")) {
          document.addLayer("", markup);
          layers.add("");

        } else if (layerId.contains("+")) {
          String[] parts = layerId.split("\\+");
          String newLayerId = parts[1];
          document.addLayer(newLayerId, markup);
          layers.add(newLayerId);

        } else {
          layers.add(layerId);
        }
      });
      markup.addAllLayers(layers);

      if (markup != null) {
        SuffixContext suffix = markupNameContext.suffix();
        if (suffix != null) {
          String id = suffix.getText().replace(TILDE, "");
          markup.setSuffix(id);
        }
        markup.getLayers().forEach(l -> {
          state.openMarkup.putIfAbsent(l, new ArrayDeque<>());
          state.openMarkup.get(l).push(markup);
        });

        currentTextVariationState().addOpenMarkup(markup);
        store.persist(markup.getDTO());
      }
    }
  }

  @Override
  public void exitEndTag(EndTagContext ctx) {
    if (tagNameIsValid(ctx)) {
      String markupName = ctx.markupName().name().getText();
      LOG.debug("endTag.markupName=<{}>", markupName);
      removeFromOpenMarkup(ctx.markupName());
    }
  }

  @Override
  public void exitMilestoneTag(MilestoneTagContext ctx) {
    if (tagNameIsValid(ctx)) {
//    String markupName = ctx.name().getText();
//    LOG.debug("milestone.markupName=<{}>", markupName);
//    ctx.annotation()
//        .forEach(annotation -> LOG.debug("milestone.annotation={{}}", annotation.getText()));
      Set<String> layers = extractLayerInfo(ctx.layerInfo());
      TAGTextNode tn = store.createTextNode("");
      document.addTextNode(tn);
      logTextNode(tn);
//      state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
      TAGMarkup markup = addMarkup(ctx.name().getText(), ctx.annotation(), ctx);
      linkTextToMarkup(tn, markup);
    }
  }

  @Override
  public void enterTextVariation(final TextVariationContext ctx) {
//    LOG.debug("<| lastTextNodeInTextVariationStack.size()={}",lastTextNodeInTextVariationStack.size());
    TAGTextNode tn = store.createTextNode(divergence);
    if (previousTextNode != null) {
      tn.addPreviousTextNode(previousTextNode);
    }
    previousTextNode = tn;
    document.addTextNode(tn);
//    state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    TextVariationState textVariationState = new TextVariationState();
    textVariationState.startNode = tn;
    textVariationState.startState = state.copy();
    textVariationState.branch = 0;
    logTextNode(tn);
    textVariationStateStack.push(textVariationState);
  }

  @Override
  public void exitTextVariationSeparator(final TextVariationSeparatorContext ctx) {
    List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
    TAGTextNode lastTextNode = TAGTextNodes.get(TAGTextNodes.size() - 1);
    currentTextVariationState().endNodes.add(lastTextNode);
    previousTextNode = currentTextVariationState().startNode;
    currentTextVariationState().endStates.add(state.copy());
    state = currentTextVariationState().startState.copy();
    currentTextVariationState().branch += 1;
  }

  @Override
  public void exitTextVariation(final TextVariationContext ctx) {
    currentTextVariationState().endNodes.add(previousTextNode);
    currentTextVariationState().endStates.add(state.copy());
    checkEndStates(ctx);
    if (errorListener.hasErrors()) {
      return;
    }
    mergeNewOpenMarkup(ctx);
//    LOG.debug("lastTextNodeInTextVariationStack.peek()={}", lastTextNodeInTextVariationStack.peek().stream().map(TextNodeWrapper::getDbId).collect(toList()));
    TAGTextNode tn = store.createTextNode(convergence);
    previousTextNode = tn;
    document.addTextNode(tn);
//    state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    textVariationStateStack.pop().endNodes.forEach(n -> {
//      logTextNode(n);
      n.addNextTextNode(tn);
      tn.addPreviousTextNode(n);
    });
//    LOG.debug("|> lastTextNodeInTextVariationStack.size()={}",lastTextNodeInTextVariationStack.size());
    logTextNode(tn);
  }

  private void mergeNewOpenMarkup(ParserRuleContext ctx) {
    /// TODO: refactor!
    TextVariationState textVariationState = currentTextVariationState();
    if (textVariationState.openMarkup.isEmpty()) {
      return;
    }
    List<TAGMarkup> markupOpenedInFinalBranch = textVariationState.openMarkup.get(textVariationState.branch);
    for (int branch = textVariationState.branch - 1; branch >= 0; branch--) {
      List<TAGMarkup> markupToMerge = textVariationState.openMarkup.get(branch);
      for (TAGMarkup otherMarkup : markupToMerge) {
        Optional<TAGMarkup> masterMarkupOptional = findMatchingMarkup(markupOpenedInFinalBranch, otherMarkup);
        if (masterMarkupOptional.isPresent()) {
          List<Long> textNodeIdsToAdd = new ArrayList<>();
          TAGMarkup masterMarkup = masterMarkupOptional.get();
          otherMarkup.getTextNodeStream().forEach(textNode -> {
            document.disAssociateTextNodeWithMarkup(textNode, otherMarkup);
            document.associateTextNodeWithMarkup(textNode, masterMarkup);
            textNodeIdsToAdd.add(textNode.getDbId());
          });
          masterMarkup.getDTO().getTextNodeIds().addAll(0, textNodeIdsToAdd);
          document.getDTO().getMarkupIds().remove(otherMarkup.getDbId());
          store.persist(document.getDTO());
          store.persist(masterMarkup.getDTO());
          store.remove(otherMarkup.getDTO());
        } else {
          errorListener.addError(
              "%s Markup %s found in branch %s, but not in branch %s.",
              errorPrefix(ctx, true), openTag(otherMarkup), branch + 1, textVariationState.branch + 1
          );
        }
      }
    }
  }

  private Optional<TAGMarkup> findMatchingMarkup(List<TAGMarkup> markupOpenedInBranch0, TAGMarkup m) {
    return markupOpenedInBranch0.stream().filter(m::matches).findFirst();
  }

  private void checkEndStates(final TextVariationContext ctx) {
    List<List<String>> suspendedMarkupInBranch = new ArrayList<>();
    List<List<String>> resumedMarkupInBranch = new ArrayList<>();

    List<List<String>> openedMarkupInBranch = new ArrayList<>();
    List<List<String>> closedMarkupInBranch = new ArrayList<>();

    State startState = currentTextVariationState().startState;
//    Deque<MarkupWrapper> suspendedMarkupBeforeDivergence = startState.suspendedMarkup;
//    Deque<MarkupWrapper> openMarkupBeforeDivergence = startState.openMarkup;
//
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
      errorListener.addError(
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
        String closed = closedMarkupInBranch.get(i).stream().collect(joining(", "));
        String closedStatement = closed.isEmpty()
            ? "didn't close any markup"
            : "closed markup " + closed;
        String opened = openedMarkupInBranch.get(i).stream().collect(joining(", "));
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
      errorListener.addError(
          "%s There is an open markup discrepancy between the branches:%s",
          errorPrefix, branchLines);
    }
  }

  private TAGMarkup addMarkup(String extendedTag, List<AnnotationContext> atts, ParserRuleContext ctx) {
    TAGMarkup markup = store.createMarkup(document, extendedTag);
    addAnnotations(atts, markup);
    document.addMarkup(markup);
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

  private void addAnnotations(List<AnnotationContext> annotationContexts, TAGMarkup markup) {
    annotationContexts.forEach(actx -> {
      if (actx instanceof BasicAnnotationContext) {
        BasicAnnotationContext basicAnnotationContext = (BasicAnnotationContext) actx;
        String aName = basicAnnotationContext.annotationName().getText();
        String quotedAttrValue = basicAnnotationContext.annotationValue().getText();
        // TODO: handle recursion, value types
//      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
        TAGAnnotation annotation = store.createAnnotation(aName, quotedAttrValue);
        markup.addAnnotation(annotation);

      } else if (actx instanceof IdentifyingAnnotationContext) {
        IdentifyingAnnotationContext idAnnotationContext = (IdentifyingAnnotationContext) actx;
        String id = idAnnotationContext.idValue().getText();
        markup.setMarkupId(id);

      } else if (actx instanceof RefAnnotationContext) {
        RefAnnotationContext refAnnotationContext = (RefAnnotationContext) actx;
        String aName = refAnnotationContext.annotationName().getText();
        String refId = refAnnotationContext.refValue().getText();
        // TODO add ref to model
        TAGAnnotation annotation = store.createAnnotation(aName, refId);
        markup.addAnnotation(annotation);
      }
    });
  }

  private void linkTextToMarkup(TAGTextNode tn, TAGMarkup markup) {
    document.associateTextNodeWithMarkup(tn, markup);
    markup.addTextNode(tn);
  }

  private Long update(TAGDTO tagdto) {
    return store.persist(tagdto);
  }

  private TAGMarkup removeFromOpenMarkup(MarkupNameContext ctx) {
    String extendedMarkupName = ctx.name().getText();

    extendedMarkupName = withPrefix(ctx, extendedMarkupName);
    extendedMarkupName = withSuffix(ctx, extendedMarkupName);

    LayerInfoContext layerInfoContext = ctx.layerInfo();
    Set<String> layers = extractLayerInfo(layerInfoContext);
    TAGMarkup markup = null;
    String foundLayerSuffix = layerInfoContext == null
        ? ""
        : TAGML.DIVIDER + extractLayerInfo(layerInfoContext).stream().sorted().collect(joining(","));
    extendedMarkupName = extendedMarkupName + foundLayerSuffix;
    for (String l : layers) {
      state.openMarkup.putIfAbsent(l, new ArrayDeque<>());
      Deque<TAGMarkup> markupStack = state.openMarkup.get(l);
      markup = removeFromMarkupStack(extendedMarkupName, markupStack);
      if (markup == null) {
        TAGMarkup expected = markupStack.peek();
        errorListener.addError(
            "%s Close tag <%s] found, expected %s.",
            errorPrefix(ctx), extendedMarkupName, closeTag(expected)
        );
        return null;
      }
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

    return markup;
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

  private TAGMarkup removeFromMarkupStack(String extendedTag, Deque<TAGMarkup> markupStack) {
    final TAGMarkup expected = markupStack.peek();
    if (extendedTag.equals(expected.getExtendedTag())) {
      markupStack.pop();
      currentTextVariationState().removeOpenMarkup(expected);
      return expected;
    }
    return null;
  }

  private TAGMarkup resumeMarkup(StartTagContext ctx) {
    String tag = ctx.markupName().getText().replace(RESUME_PREFIX, "");
    TAGMarkup markup = null;
    Set<String> layers = extractLayerInfo(ctx.markupName().layerInfo());
    for (String layer : layers) {
      markup = removeFromMarkupStack(tag, state.suspendedMarkup.get(layer));
      checkForCorrespondingSuspendTag(ctx, tag, markup);
      if (!errorListener.hasErrors()) {
        checkForTextBetweenSuspendAndResumeTags(markup, ctx);
        if (!errorListener.hasErrors()) {
          markup.setIsDiscontinuous(true);
        }
      }
    }
    return markup;
  }

  private void checkForCorrespondingSuspendTag(final StartTagContext ctx, final String tag,
      final TAGMarkup markup) {
    if (markup == null) {
      errorListener.addError(
          "%s Resume tag %s found, which has no corresponding earlier suspend tag <%s%s].",
          errorPrefix(ctx), ctx.getText(), SUSPEND_PREFIX, tag
      );
    }
  }

  private void checkForTextBetweenSuspendAndResumeTags(final TAGMarkup markup, final StartTagContext ctx) {
//    List<Long> markupTextNodeIds = markup.getMarkupDTO().getTextNodeIds();
//    Long lastMarkupTextNodeId = markupTextNodeIds.get(markupTextNodeIds.size() - 1);
//    List<Long> documentTextNodeIds = document.getDocumentDTO().getTextNodeIds();
//    Long lastDocumentTextNodeId = documentTextNodeIds.get(documentTextNodeIds.size() - 1);
//    if (lastDocumentTextNodeId.equals(lastMarkupTextNodeId)) {
//      errorListener.addError(
//          "%s There is no text between this resume tag %s and it's corresponding suspend tag %s. This is not allowed.",
//          errorPrefix(ctx), resumeTag(markup), suspendTag(markup)
//      );
//    }
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
//            if (!layerInfo.containsKey(lid)) {
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

  private String openTag(final TAGMarkup m) {
    return OPEN_TAG_STARTCHAR + m.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String closeTag(final TAGMarkup m) {
    return CLOSE_TAG_STARTCHAR + m.getExtendedTag() + CLOSE_TAG_ENDCHAR;
  }

  private String suspendTag(TAGMarkup tagMarkup) {
    return CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX + tagMarkup.getExtendedTag() + CLOSE_TAG_ENDCHAR;
  }

  private String resumeTag(TAGMarkup tagMarkup) {
    return OPEN_TAG_STARTCHAR + RESUME_PREFIX + tagMarkup.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String errorPrefix(ParserRuleContext ctx) {
    return errorPrefix(ctx, false);
  }

  private String errorPrefix(ParserRuleContext ctx, boolean useStopToken) {
    Token token = useStopToken ? ctx.stop : ctx.start;
    return format("line %d:%d :", token.getLine(), token.getCharPositionInLine() + 1);
  }

  private void logTextNode(final TAGTextNode textNode) {
    TAGTextNodeDTO dto = textNode.getDTO();
    LOG.debug("TextNode(id={}, type={}, text=<{}>, prev={}, next={})",
        textNode.getDbId(),
        dto.getType(),
        dto.getText(),
        dto.getPrevTextNodeIds(),
        dto.getNextTextNodeIds()
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
      layers.add("");
    }
    return layers;
  }
}
