package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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
import static nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*;

public class TAGMLListener extends TAGMLParserBaseListener {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListener.class);
  public static final String TILDE = "~";

  private final TAGStore store;
  private final DocumentWrapper document;
  private final ErrorListener errorListener;
  private final Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
  private final Deque<MarkupWrapper> suspendedMarkup = new ArrayDeque<>();
  private final Deque<TextNodeWrapper> textVariationStartNodeStack = new ArrayDeque<>();
  private final Deque<List<TextNodeWrapper>> lastTextNodeStack = new ArrayDeque<>();
  private final HashMap<String, MarkupWrapper> identifiedMarkups = new HashMap<>();
  private final HashMap<String, String> idsInUse = new HashMap<>();
  private final Map<String, String> namespaces = new HashMap<>();

  private boolean atDocumentStart = true;
  private TextNodeWrapper previousTextNode = null;

  public TAGMLListener(final TAGStore store, ErrorListener errorListener) {
    this.store = store;
    this.document = store.createDocumentWrapper();
    this.errorListener = errorListener;
  }

  public DocumentWrapper getDocument() {
    return document;
  }

  @Override
  public void exitDocument(TAGMLParser.DocumentContext ctx) {
    update(document.getDocument());
    if (!openMarkup.isEmpty()) {
      String openRanges = openMarkup.stream()//
          .map(m -> "[" + m.getExtendedTag() + ">")//
          .collect(joining(", "));
      errorListener.addError(
          "Unclosed TAGML tag(s): %s",
          openRanges
      );
    }
    if (!suspendedMarkup.isEmpty()) {
      String suspendedMarkupString = suspendedMarkup.stream()//
          .map(this::suspendTag)//
          .collect(Collectors.joining(", "));
      errorListener.addError("Some suspended markup was not resumed: %s", suspendedMarkupString);
    }
  }

  private String suspendTag(MarkupWrapper markupWrapper) {
    return "<" + markupWrapper.getExtendedTag() + "]";
  }

  @Override
  public void exitNamespaceDefinition(NamespaceDefinitionContext ctx) {
    String ns = ctx.IN_NamespaceIdentifier().getText();
    String url = ctx.IN_NamespaceURI().getText();
    namespaces.put(ns, url);
  }

  @Override
  public void exitText(TextContext ctx) {
    String text = ctx.getText();
    LOG.info("text=<{}>", text);
    atDocumentStart = atDocumentStart && StringUtils.isBlank(text);
    if (!atDocumentStart) {
      TextNodeWrapper tn = store.createTextNodeWrapper(text);
      if (previousTextNode != null) {
        tn.setPreviousTextNode(previousTextNode);
      }
      previousTextNode = tn;
      document.addTextNode(tn);
      openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    }
  }

  @Override
  public void enterStartTag(StartTagContext ctx) {
    String markupName = ctx.markupName().name().getText();
    LOG.info("startTag.markupName=<{}>", markupName);
    if (markupName.contains(":")) {
      String namespace = markupName.split(":", 2)[0];
      if (!namespaces.containsKey(namespace)) {
        errorListener.addError(
            "%s namespace %s has not been defined.",
            errorPrefix(ctx), namespace
        );
      }
    }
    ctx.annotation()
        .forEach(annotation -> LOG.info("  startTag.annotation={{}}", annotation.getText()));

    TerminalNode prefix = ctx.markupName().IMO_Prefix();
    boolean optional = prefix != null && prefix.getText().equals("?");
    boolean resume = prefix != null && prefix.getText().equals("+");

    MarkupWrapper markup = resume
        ? resumeMarkup(ctx)
        : addMarkup(markupName, ctx.annotation(), ctx).setOptional(optional);

    if (markup != null) {
      TerminalNode suffix = ctx.markupName().IMO_Suffix();
      if (suffix != null) {
        String id = suffix.getText().replace(TILDE, "");
        markup.setSuffix(id);
      }

      openMarkup.add(markup);
    }
  }

  private MarkupWrapper resumeMarkup(StartTagContext ctx) {
    String tag = ctx.markupName().getText().replace("+", "");
    MarkupWrapper markup = removeFromMarkupStack(tag, suspendedMarkup);
    if (markup == null) {
      errorListener.addError(
          "%s Resuming tag %s found, which has no corresponding earlier suspending tag <-%s].",
          errorPrefix(ctx), ctx.getText(), tag
      );
    }
    return markup;
  }

  @Override
  public void exitEndTag(EndTagContext ctx) {
    removeFromOpenMarkup(ctx.markupName());
  }

  @Override
  public void exitMilestoneTag(MilestoneTagContext ctx) {
//    String markupName = ctx.name().getText();
//    LOG.info("milestone.markupName=<{}>", markupName);
//    ctx.annotation()
//        .forEach(annotation -> LOG.info("milestone.annotation={{}}", annotation.getText()));
    TextNodeWrapper tn = store.createTextNodeWrapper("");
    document.addTextNode(tn);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    MarkupWrapper markup = addMarkup(ctx.name().getText(), ctx.annotation(), ctx);
    linkTextToMarkup(tn, markup);
  }

  @Override
  public void enterTextVariation(final TextVariationContext ctx) {
    TextNodeWrapper tn = store.createTextNodeWrapper("");
    document.addTextNode(tn);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    textVariationStartNodeStack.push(tn);
  }

  @Override
  public void exitTextVariationSeparator(final TextVariationSeparatorContext ctx) {
    if (lastTextNodeStack.isEmpty()) {
      lastTextNodeStack.add(new ArrayList<>());
    }
    List<TextNodeWrapper> lastTextNodes = lastTextNodeStack.peek();

    List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
    TextNodeWrapper lastTextNode = textNodeWrappers.get(textNodeWrappers.size() - 1);
    lastTextNodes.add(lastTextNode);
    lastTextNode = textVariationStartNodeStack.peek();
  }

  @Override
  public void exitTextVariation(final TextVariationContext ctx) {
    TextNodeWrapper textVariationEndNode = store.createTextNodeWrapper("");
    document.addTextNode(textVariationEndNode);
    openMarkup.forEach(m -> linkTextToMarkup(textVariationEndNode, m));
    lastTextNodeStack.pop().forEach(n -> n.setNextTextNode(textVariationEndNode));
    textVariationStartNodeStack.pop();
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
            "%s id '%s' was already used in markup [%s>.",
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

    TerminalNode suffix = ctx.IMC_Suffix();
    if (suffix != null) {
      extendedMarkupName += suffix.getText();
    }

    MarkupWrapper markup = removeFromMarkupStack(extendedMarkupName, openMarkup);
    if (markup == null) {
      errorListener.addError(
          "%s Closing tag <%s] found without corresponding open tag.",
          errorPrefix(ctx), extendedMarkupName
      );
    } else {

      TerminalNode prefixNode = ctx.IMC_Prefix();
      if (prefixNode != null) {
        String prefixNodeText = prefixNode.getText();
        if (prefixNodeText.equals("?")) {
          // optional
          // TODO

        } else if (prefixNodeText.equals("-")) {
          // suspend
          suspendedMarkup.add(markup);
        }
      }
    }

    return markup;
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

  private String errorPrefix(ParserRuleContext ctx) {
    Token startToken = ctx.start;
    return format("line %d:%d :", startToken.getLine(), startToken.getCharPositionInLine());
  }

}
