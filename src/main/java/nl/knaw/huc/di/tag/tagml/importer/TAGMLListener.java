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

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener;
import nl.knaw.huygens.alexandria.storage.*;
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*;

public class TAGMLListener extends TAGMLParserBaseListener {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListener.class);

  private final TAGStore store;
  private final DocumentWrapper document;
  private final Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
  private final Deque<MarkupWrapper> suspendedMarkup = new ArrayDeque<>();
  private final HashMap<String, MarkupWrapper> identifiedMarkups = new HashMap<>();
  private final List<String> errors = new ArrayList<>();
  private final HashMap<String, String> idsInUse = new HashMap<>();

  public TAGMLListener(final TAGStore store) {
    this.store = store;
    this.document = store.createDocumentWrapper();
  }

  public DocumentWrapper getDocument() {
    return document;
  }

  @Override
  public void exitText(TextContext ctx) {
    String text = ctx.getText();
    LOG.info("text=<{}>", text);
    TextNodeWrapper tn = store.createTextNodeWrapper(text);
    document.addTextNode(tn);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
  }

  @Override
  public void enterStartTag(StartTagContext ctx) {
    String markupName = ctx.markupName().getText();
    LOG.info("startTag.markupName=<{}>", markupName);
    ctx.annotation()
        .forEach(annotation -> LOG.info("  startTag.annotation={{}}", annotation.getText()));

    MarkupWrapper markup = addMarkup(markupName, ctx.annotation());
    openMarkup.add(markup);

  }

  @Override
  public void exitEndTag(EndTagContext ctx) {
    String markupName = ctx.markupName().getText();
    LOG.info("   endTag.markupName=<{}>", markupName);
    removeFromOpenMarkup(ctx.markupName());
  }

  @Override
  public void exitMilestone(MilestoneContext ctx) {
//    String markupName = ctx.name().getText();
//    LOG.info("milestone.markupName=<{}>", markupName);
//    ctx.annotation()
//        .forEach(annotation -> LOG.info("milestone.annotation={{}}", annotation.getText()));
    TextNodeWrapper tn = store.createTextNodeWrapper("");
    document.addTextNode(tn);
    openMarkup.forEach(m -> linkTextToMarkup(tn, m));
    MarkupWrapper markup = addMarkup(ctx.name().getText(), ctx.annotation());
    linkTextToMarkup(tn, markup);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public List<String> getErrors() {
    return errors;
  }

  private class DocumentContext {
    private final TAGDocument document;
    private final Deque<TAGMarkup> openMarkupDeque = new ArrayDeque<>();
    private final Stack<TAGMarkup> openMarkupStack = new Stack<>();
    private final Stack<TAGAnnotation> annotationStack = new Stack<>();
    private final ListenerContext listenerContext;

    DocumentContext(TAGDocument document, ListenerContext listenerContext) {
      this.document = document;
      this.listenerContext = listenerContext;
    }

    void openMarkup(TAGMarkup markup) {
      openMarkupDeque.push(markup);
      openMarkupStack.push(markup);
      document.getMarkupIds().add(markup.getId());
    }

    void pushOpenMarkup(String rangeName) {
      // LOG.info("currentDocumentContext().openMarkupDeque={}", openMarkupDeque.stream().map(Markup::getTag).collect(Collectors.toList()));
      Optional<TAGMarkup> findFirst = openMarkupDeque.stream()//
          .filter(tr -> tr.getExtendedTag().equals(rangeName))//
          .findFirst();
      if (findFirst.isPresent()) {
        TAGMarkup markup = findFirst.get();
        if (markup.getTextNodeIds().isEmpty()) {
          // every markup should have at least one textNode
          TAGTextNode emptyTextNode = new TAGTextNode("");
          update(emptyTextNode);
          addTextNode(emptyTextNode);
          closeMarkup();
        }
        openMarkupStack.push(markup);
      } else {
        listenerContext.errors.add(format("%s Closing tag <%s] found without corresponding open tag.",
            errorPrefix(listenerContext.getCurrentToken()), rangeName));
      }
    }

    private String errorPrefix(Token currentToken) {
      return format("line %d:%d :", currentToken.getLine(), currentToken.getCharPositionInLine());
    }

    void popOpenMarkup() {
      openMarkupStack.pop();
    }

    void closeMarkup() {
      if (!openMarkupStack.isEmpty()) {
        TAGMarkup markup = openMarkupStack.pop();
        update(markup);
        openMarkupDeque.remove(markup);
      }
    }

    void addTextNode(TAGTextNode textNode) {
      openMarkupDeque.descendingIterator()//
          .forEachRemaining(m -> {
            m.addTextNode(textNode);
            document.associateTextWithMarkup(textNode, m);
          });
      document.addTextNode(textNode);
    }

    private TAGMarkup currentMarkup() {
      return openMarkupDeque.isEmpty() ? null : openMarkupStack.peek();
    }

    void openAnnotation(TAGAnnotation annotation) {
      if (annotationStack.isEmpty()) {
        TAGMarkup markup = currentMarkup();
        if (markup != null) {
          markup.addAnnotation(annotation);
        }
      } else {
        annotationStack.peek().addAnnotation(annotation);
      }
      annotationStack.push(annotation);
    }

    TAGDocument currentAnnotationDocument() {
      Long value = annotationStack.peek().getDocumentId();
      return store.getDocument(value);
    }

    void closeAnnotation() {
      TAGAnnotation annotation = annotationStack.pop();
      update(annotation);
    }
  }

  private class ListenerContext {
    private final Deque<DocumentContext> documentContextStack = new ArrayDeque<>();
    private final TAGMLLexer lexer;
    private final List<String> errors = new ArrayList<>();
    private String methodName;
    private Token currentToken;

    ListenerContext(TAGMLLexer lexer) {
      this.lexer = lexer;
    }

    Token nextToken() {
      currentToken = lexer.nextToken();
      return currentToken;
    }

    Token getCurrentToken() {
      return currentToken;
    }

    String getModeName() {
      return lexer.getModeNames()[lexer._mode];
    }

    String getRuleName() {
      int type = currentToken.getType();
      return type == -1 ? "EOF" : lexer.getRuleNames()[type - 1];
    }

    void pushDocumentContext(TAGDocument document) {
      documentContextStack.push(new DocumentContext(document, this));
    }

    DocumentContext currentDocumentContext() {
      return documentContextStack.peek();
    }

    DocumentContext popDocumentContext() {
      DocumentContext documentContext = documentContextStack.pop();
      update(documentContext.document);
      if (!documentContext.openMarkupDeque.isEmpty()) {
        String openRanges = documentContext.openMarkupDeque.stream()//
            .map(m -> "[" + m.getExtendedTag() + ">")//
            .collect(Collectors.joining(", "));
        errors.add("Unclosed TAGML tag(s): " + openRanges);
      }
      return documentContext;
    }

    TAGMarkup newMarkup(String tagName) {
      TAGMarkup tagMarkup = new TAGMarkup(currentDocumentContext().document.getId(), tagName);
      update(tagMarkup);
      return tagMarkup;
    }

    void openMarkup(TAGMarkup markup) {
      currentDocumentContext().openMarkup(markup);
    }

    void pushOpenMarkup(String rangeName) {
      currentDocumentContext().pushOpenMarkup(rangeName);
    }

    void popOpenMarkup() {
      currentDocumentContext().popOpenMarkup();
    }

    void closeMarkup() {
      currentDocumentContext().closeMarkup();
    }

    void addTextNode(TAGTextNode textNode) {
      currentDocumentContext().addTextNode(textNode);
    }

    void openAnnotation(TAGAnnotation annotation) {
      currentDocumentContext().openAnnotation(annotation);
    }

    TAGDocument currentAnnotationDocument() {
      return currentDocumentContext().currentAnnotationDocument();
    }

    void closeAnnotation() {
      currentDocumentContext().closeAnnotation();
    }

    List<String> getErrors() {
      return errors;
    }

    boolean hasErrors() {
      return !errors.isEmpty();
    }

    String getMethodName() {
      return methodName;
    }

    void setMethodName(String methodName) {
      this.methodName = methodName;
    }
  }

  private MarkupWrapper addMarkup(String extendedTag, List<AnnotationContext> atts) {
    MarkupWrapper markup = store.createMarkupWrapper(document, extendedTag);
    addAttributes(atts, markup);
    document.addMarkup(markup);
    if (markup.hasMarkupId()) {
      identifiedMarkups.put(extendedTag, markup);
      String id = markup.getMarkupId();
      if (idsInUse.containsKey(id)) {
        String message = "id '" + id + "' was already used in markup [" + idsInUse.get(id) + ">.";
        errors.add(message);
      }
      idsInUse.put(id, extendedTag);
    }
    return markup;
  }

  private void addAttributes(List<AnnotationContext> annotationContexts, MarkupWrapper markup) {
    annotationContexts.forEach(actx -> {
      String attrName = actx.ANNOTATION_NAME().getText();
      String quotedAttrValue = actx.annotationValue().getText();
      // TODO: handle recursion, value types
//      String attrValue = quotedAttrValue.substring(1, quotedAttrValue.length() - 1); // remove single||double quotes
      AnnotationWrapper annotation = store.createAnnotationWrapper(attrName, quotedAttrValue);
      markup.addAnnotation(annotation);
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
    String tag = ctx.getText();
    MarkupWrapper markup = removeFromMarkupStack(tag, openMarkup);
    if (markup == null) {
      String message = errorPrefix(ctx) + "Closing tag <" + tag + "] found, which has no corresponding earlier opening tag.";
      errors.add(message);
    }
    return markup;
  }

//  private MarkupWrapper removeFromSuspendedMarkup(ResumeTagContext ctx) {
//    String tag = ctx.gi().getText();
//    MarkupWrapper markup = removeFromMarkupStack(tag, suspendedMarkup);
//    if (markup == null) {
//      String message = errorPrefix(ctx) + "Resuming tag [+" + tag + "> found, which has no corresponding earlier suspending tag <-" + tag + "].";
//      errors.add(message);
//    }
//    return markup;
//  }

  private MarkupWrapper removeFromMarkupStack(String tag, Deque<MarkupWrapper> markupStack) {
    Iterator<MarkupWrapper> descendingIterator = markupStack.descendingIterator();
    MarkupWrapper markup = null;
    while (descendingIterator.hasNext()) {
      markup = descendingIterator.next();
      if (markup.getTag().equals(tag)) {
        break;
      }
    }
    if (markup != null) {
      markupStack.remove(markup);
    }
    return markup;
  }

  private String errorPrefix(ParserRuleContext ctx) {
    Token startToken = ctx.start;
    return format("line %d:%d : ", startToken.getLine(), startToken.getCharPositionInLine());
  }

}
