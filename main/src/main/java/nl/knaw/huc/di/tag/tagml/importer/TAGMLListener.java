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

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGObject;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;
import static nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.convergence;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.divergence;

public class TAGMLListener extends TAGMLParserBaseListener {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListener.class);
  public static final String TILDE = "~";

  private final TAGStore store;
  private final DocumentWrapper document;
  private final ErrorListener errorListener;
  private final HashMap<String, MarkupWrapper> identifiedMarkups = new HashMap<>();
  private final HashMap<String, String> idsInUse = new HashMap<>();
  private final Map<String, String> namespaces = new HashMap<>();
  private State state = new State();

  private final Deque<TextVariationState> textVariationStateStack = new ArrayDeque<>();

  private boolean atDocumentStart = true;
  private TextNodeWrapper previousTextNode = null;

  public TAGMLListener(final TAGStore store, ErrorListener errorListener) {
    this.store = store;
    this.document = store.createDocumentWrapper();
    this.errorListener = errorListener;
    this.textVariationStateStack.push(new TextVariationState());
  }

  public DocumentWrapper getDocument() {
    return document;
  }

  public class State {
    public Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
    public Deque<MarkupWrapper> suspendedMarkup = new ArrayDeque<>();

    public State copy() {
      State copy = new State();
      copy.openMarkup = new ArrayDeque<>(openMarkup);
      copy.suspendedMarkup = new ArrayDeque<>(suspendedMarkup);
      return copy;
    }
  }

  public class TextVariationState {
    public State startState;
    public List<State> endStates = new ArrayList<>();
    public TextNodeWrapper startNode;
    public List<TextNodeWrapper> endNodes = new ArrayList<>();
  }

  @Override
  public void exitDocument(TAGMLParser.DocumentContext ctx) {
    update(document.getDocument());
    if (!state.openMarkup.isEmpty()) {
      String openRanges = state.openMarkup.stream()//
          .map(this::openTag)//
          .collect(joining(", "));
      errorListener.addError(
          "Missing close tag(s) for: %s",
          openRanges
      );
    }
    if (!state.suspendedMarkup.isEmpty()) {
      String suspendedMarkupString = state.suspendedMarkup.stream()//
          .map(this::suspendTag)//
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

  @Override
  public void exitText(TextContext ctx) {
    String text = unEscape(ctx.getText());
//    LOG.debug("text=<{}>", text);
    atDocumentStart = atDocumentStart && StringUtils.isBlank(text);
    if (!atDocumentStart) {
      TextNodeWrapper tn = store.createTextNodeWrapper(text);
      if (previousTextNode != null) {
        tn.addPreviousTextNode(previousTextNode);
      }
      previousTextNode = tn;
      document.addTextNode(tn);
      state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
      logTextNode(tn);
    }
  }

  @Override
  public void enterStartTag(StartTagContext ctx) {
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

      TerminalNode prefix = markupNameContext.IMO_Prefix();
      boolean optional = prefix != null && prefix.getText().equals(OPTIONAL_PREFIX);
      boolean resume = prefix != null && prefix.getText().equals(RESUME_PREFIX);

      MarkupWrapper markup = resume
          ? resumeMarkup(ctx)
          : addMarkup(markupName, ctx.annotation(), ctx).setOptional(optional);

      if (markup != null) {
        TerminalNode suffix = markupNameContext.IMO_Suffix();
        if (suffix != null) {
          String id = suffix.getText().replace(TILDE, "");
          markup.setSuffix(id);
        }

        state.openMarkup.add(markup);
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
      TextNodeWrapper tn = store.createTextNodeWrapper("");
      document.addTextNode(tn);
      logTextNode(tn);
      state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
      MarkupWrapper markup = addMarkup(ctx.name().getText(), ctx.annotation(), ctx);
      linkTextToMarkup(tn, markup);
    }
  }

  @Override
  public void enterTextVariation(final TextVariationContext ctx) {
//    LOG.debug("<| lastTextNodeInTextVariationStack.size()={}",lastTextNodeInTextVariationStack.size());
    TextNodeWrapper tn = store.createTextNodeWrapper(divergence);
    if (previousTextNode != null) {
      tn.addPreviousTextNode(previousTextNode);
    }
    previousTextNode = tn;
    document.addTextNode(tn);
    state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    TextVariationState textVariationState = new TextVariationState();
    textVariationState.startNode = tn;
    textVariationState.startState = state.copy();
    logTextNode(tn);
    textVariationStateStack.push(textVariationState);
  }

  @Override
  public void exitTextVariationSeparator(final TextVariationSeparatorContext ctx) {
    List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
    TextNodeWrapper lastTextNode = textNodeWrappers.get(textNodeWrappers.size() - 1);
    currentTextVariationState().endNodes.add(lastTextNode);
    previousTextNode = currentTextVariationState().startNode;
    currentTextVariationState().endStates.add(state.copy());
//    state = currentTextVariationState().startState;
  }

  @Override
  public void exitTextVariation(final TextVariationContext ctx) {
    currentTextVariationState().endNodes.add(previousTextNode);
//    LOG.debug("lastTextNodeInTextVariationStack.peek()={}", lastTextNodeInTextVariationStack.peek().stream().map(TextNodeWrapper::getDbId).collect(toList()));
    TextNodeWrapper tn = store.createTextNodeWrapper(convergence);
    previousTextNode = tn;
    document.addTextNode(tn);
    state.openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    textVariationStateStack.pop().endNodes.forEach(n -> {
//      logTextNode(n);
      n.addNextTextNode(tn);
      tn.addPreviousTextNode(n);
    });
//    LOG.debug("|> lastTextNodeInTextVariationStack.size()={}",lastTextNodeInTextVariationStack.size());
    logTextNode(tn);
  }

  private MarkupWrapper addMarkup(String extendedTag, List<AnnotationContext> atts, ParserRuleContext ctx) {
    MarkupWrapper markup = store.createMarkupWrapper(document, extendedTag);
    addAnnotations(atts, markup);
    document.addMarkup(markup);
    if (markup.hasMarkupId()) {
      identifiedMarkups.put(extendedTag, markup);
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

  private void addAnnotations(List<AnnotationContext> annotationContexts, MarkupWrapper markup) {
    annotationContexts.forEach(actx -> {
      if (actx instanceof BasicAnnotationContext) {
        BasicAnnotationContext basicAnnotationContext = (BasicAnnotationContext) actx;
        String aName = basicAnnotationContext.annotationName().getText();
        String quotedAttrValue = basicAnnotationContext.annotationValue().getText();
        // TODO: handle recursion, value types
//      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
        AnnotationWrapper annotation = store.createAnnotationWrapper(aName, quotedAttrValue);
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
        AnnotationWrapper annotation = store.createAnnotationWrapper(aName, refId);
        markup.addAnnotation(annotation);
      }
    });
  }

  private void linkTextToMarkup(TextNodeWrapper tn, MarkupWrapper markup) {
    document.associateTextNodeWithMarkup(tn, markup);
    markup.addTextNode(tn);
  }

  private Long update(TAGObject tagObject) {
    return store.persist(tagObject);
  }

  private MarkupWrapper removeFromOpenMarkup(MarkupNameContext ctx) {
    String extendedMarkupName = ctx.name().getText();

    extendedMarkupName = withPrefix(ctx, extendedMarkupName);
    extendedMarkupName = withSuffix(ctx, extendedMarkupName);

    MarkupWrapper markup = removeFromMarkupStack(extendedMarkupName, state.openMarkup);
    if (markup == null) {
      errorListener.addError(
          "%s Close tag <%s] found without corresponding open tag.",
          errorPrefix(ctx), extendedMarkupName
      );
      return null;
    }

    TerminalNode prefixNode = ctx.IMC_Prefix();
    if (prefixNode != null) {
      String prefixNodeText = prefixNode.getText();
      if (prefixNodeText.equals(OPTIONAL_PREFIX)) {
        // optional
        // TODO

      } else if (prefixNodeText.equals(SUSPEND_PREFIX)) {
        // suspend
        state.suspendedMarkup.add(markup);
      }
    }

    return markup;
  }

  private String withSuffix(final MarkupNameContext ctx, String extendedMarkupName) {
    TerminalNode suffix = ctx.IMC_Suffix();
    if (suffix != null) {
      extendedMarkupName += suffix.getText();
    }
    return extendedMarkupName;
  }

  private String withPrefix(final MarkupNameContext ctx, String extendedMarkupName) {
    TerminalNode prefix = ctx.IMC_Prefix();
    if (prefix != null && prefix.getText().equals(OPTIONAL_PREFIX)) {
      extendedMarkupName = prefix.getText() + extendedMarkupName;
    }
    return extendedMarkupName;
  }

  private MarkupWrapper removeFromMarkupStack(String extendedTag, Deque<MarkupWrapper> markupStack) {
    Iterator<MarkupWrapper> descendingIterator = markupStack.descendingIterator();
    MarkupWrapper markup = null;
    while (descendingIterator.hasNext()) {
      markup = descendingIterator.next();
      if (markup.getExtendedTag().equals(extendedTag)) {
        break;
      }
      markup = null;
    }
    if (markup != null) {
      markupStack.remove(markup);
    }
    return markup;
  }

  private MarkupWrapper resumeMarkup(StartTagContext ctx) {
    String tag = ctx.markupName().getText().replace(RESUME_PREFIX, "");
    MarkupWrapper markup = removeFromMarkupStack(tag, state.suspendedMarkup);
    checkForCorrespondingSuspendTag(ctx, tag, markup);
    checkForTextBetweenSuspendAndResumeTags(ctx, markup);
    markup.setIsDiscontinuous(true);
    return markup;
  }

  private void checkForCorrespondingSuspendTag(final StartTagContext ctx, final String tag, final MarkupWrapper markup) {
    if (markup == null) {
      errorListener.addError(
          "%s Resume tag %s found, which has no corresponding earlier suspend tag <%s%s].",
          errorPrefix(ctx), ctx.getText(), SUSPEND_PREFIX, tag
      );
    }
  }

  private void checkForTextBetweenSuspendAndResumeTags(final StartTagContext ctx, final MarkupWrapper markup) {
    List<Long> markupTextNodeIds = markup.getMarkup().getTextNodeIds();
    Long lastMarkupTextNodeId = markupTextNodeIds.get(markupTextNodeIds.size() - 1);
    List<Long> documentTextNodeIds = document.getDocument().getTextNodeIds();
    Long lastDocumentTextNodeId = documentTextNodeIds.get(documentTextNodeIds.size() - 1);
    if (lastDocumentTextNodeId.equals(lastMarkupTextNodeId)) {
      errorListener.addError(
          "%s There is no text between this resume tag %s and it's corresponding suspend tag %s. This is not allowed.",
          errorPrefix(ctx), resumeTag(markup), suspendTag(markup)
      );
    }
  }

  private boolean tagNameIsValid(final StartTagContext ctx) {
    NameContext nameContext = ctx.markupName().name();
    return nameContextIsValid(ctx, nameContext);
  }

  private boolean tagNameIsValid(final EndTagContext ctx) {
    NameContext nameContext = ctx.markupName().name();
    return nameContextIsValid(ctx, nameContext);
  }

  private boolean tagNameIsValid(final MilestoneTagContext ctx) {
    NameContext nameContext = ctx.name();
    return nameContextIsValid(ctx, nameContext);
  }

  private boolean nameContextIsValid(final ParserRuleContext ctx, final NameContext nameContext) {
    if (nameContext == null || nameContext.getText().isEmpty()) {
      errorListener.addError(
          "%s Nameless markup is not allowed here.",
          errorPrefix(ctx)
      );
      return false;
    }
    return true;
  }

  private TextVariationState currentTextVariationState() {
    return textVariationStateStack.peek();
  }

  private String openTag(final MarkupWrapper m) {
    return OPEN_TAG_STARTCHAR + m.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String suspendTag(MarkupWrapper markupWrapper) {
    return CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX + markupWrapper.getExtendedTag() + CLOSE_TAG_ENDCHAR;
  }

  private String resumeTag(MarkupWrapper markupWrapper) {
    return OPEN_TAG_STARTCHAR + RESUME_PREFIX + markupWrapper.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String errorPrefix(ParserRuleContext ctx) {
    Token startToken = ctx.start;
    return format("line %d:%d :", startToken.getLine(), startToken.getCharPositionInLine());
  }

  private void logTextNode(final TextNodeWrapper nodeWrapper) {
    TAGTextNode textNode = nodeWrapper.getTextNode();
    LOG.debug("TextNode(id={}, type={}, text=<{}>, prev={}, next={})",
        nodeWrapper.getDbId(),
        textNode.getType(),
        textNode.getText(),
        textNode.getPrevTextNodeIds(),
        textNode.getNextTextNodeIds()
    );
  }
}
