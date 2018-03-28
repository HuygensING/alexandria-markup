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

import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.*;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

public class TAGMLImporter {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporter.class);

  private static TAGStore tagStore;

  public TAGMLImporter(final TAGStore store) {
    tagStore = store;
  }

  static class DocumentContext {
    private final TAGDocument document;
    private final Deque<TAGMarkup> openMarkupDeque = new ArrayDeque<>();
    private final Stack<TAGMarkup> openMarkupStack = new Stack<>();
    private final Stack<TAGAnnotation> annotationStack = new Stack<>();
    private final TAGMLImporter.ImporterContext importerContext;

    DocumentContext(TAGDocument document, TAGMLImporter.ImporterContext importerContext) {
      this.document = document;
      this.importerContext = importerContext;
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
        importerContext.errors.add("Closing tag {" + rangeName + "] found without corresponding open tag.");
      }
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
      return tagStore.getDocument(value);
    }

    void closeAnnotation() {
      TAGAnnotation annotation = annotationStack.pop();
      update(annotation);
    }
  }

  static class ImporterContext {
    private final Deque<DocumentContext> documentContextStack = new ArrayDeque<>();
    private final TAGMLLexer lexer;
    private final List<String> errors = new ArrayList<>();

    ImporterContext(TAGMLLexer lexer) {
      this.lexer = lexer;
    }

    Token nextToken() {
      return lexer.nextToken();
    }

    String getModeName() {
      return lexer.getModeNames()[lexer._mode];
    }

    String getRuleName() {
      return lexer.getRuleNames()[lexer.getToken().getType() - 1];
    }

    void pushDocumentContext(TAGDocument document) {
      documentContextStack.push(new DocumentContext(document, this));
    }

    TAGMLImporter.DocumentContext currentDocumentContext() {
      return documentContextStack.peek();
    }

    TAGMLImporter.DocumentContext popDocumentContext() {
      TAGMLImporter.DocumentContext documentContext = documentContextStack.pop();
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
  }

  public DocumentWrapper importTAGML(final String input) throws TAGMLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importTAGML(antlrInputStream);
  }

  public DocumentWrapper importTAGML(InputStream input) throws TAGMLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importTAGML(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private DocumentWrapper importTAGML(CharStream antlrInputStream) throws TAGMLSyntaxError {
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);

    TAGMLImporter.ImporterContext context = new TAGMLImporter.ImporterContext(lexer);
    DocumentWrapper documentWrapper = tagStore.createDocumentWrapper();
    TAGDocument document = documentWrapper.getDocument();
    update(document);
    context.pushDocumentContext(document);
    handleDefaultMode(context);
//    joinDiscontinuedRanges(documentWrapper);
    context.popDocumentContext();

    String errorMsg = "";
    if (context.hasErrors()) {
      String errors = context.getErrors().stream().collect(Collectors.joining("\n"));
      errorMsg = "Parsing errors:\n" + errors;
    }
    if (errorListener.hasErrors()) {
      String errors = errorListener.getErrors().stream().collect(Collectors.joining("\n"));
      errorMsg += "\n\nTokenizing errors:\n" + errors;
    }
    if (!errorMsg.isEmpty()) {
      throw new TAGMLSyntaxError(errorMsg);
    }
    update(document);
    return documentWrapper;
  }

  private void handleDefaultMode(TAGMLImporter.ImporterContext context) {
    String methodName = "defaultMode";
    Token token;
    do {
      token = context.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = context.getRuleName();
        String modeName = context.getModeName();
        log(methodName, ruleName, modeName, token, context);
        switch (token.getType()) {
          case TAGMLLexer.BEGIN_OPEN_MARKUP:
            handleOpenMarkup(context);
            break;

          case TAGMLLexer.BEGIN_CLOSE_MARKUP:
            handleCloseMarkup(context);
            break;

          case TAGMLLexer.BEGIN_TEXT_VARIATION:
            handleTextVariation(context);
            break;

          case TAGMLLexer.TEXT:
            TAGTextNode textNode = new TAGTextNode(token.getText());
            update(textNode);
            context.addTextNode(textNode);
            break;

          case TAGMLLexer.COMMENT:
            handleComment(context);
            break;

          case TAGMLLexer.NAMESPACE:
            handleNamespace(context);
            break;

          default:
            handleUnexpectedToken(methodName, token, ruleName, modeName);
            break;
        }
      }
    } while (token.getType() != Token.EOF);
  }

  private void handleTextVariation(final ImporterContext context) {
    String methodName = "handleTextVariation";
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log(methodName, ruleName, modeName, token, context);
      switch (token.getType()) {
        case TAGMLLexer.END_TEXT_VARIATION:
          // TODO
          goOn = false;
          break;

        case TAGMLLexer.TEXT_VARIATION:
          TAGTextNode textNode = new TAGTextNode(token.getText());
          update(textNode);
          context.addTextNode(textNode);
          break;

        case TAGMLLexer.TextVariationSeparator:
          // TODO
          break;

        default:
          handleUnexpectedToken(methodName, token, ruleName, modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleNamespace(final ImporterContext context) {
    // TODO
  }

  private void handleComment(final ImporterContext context) {
    // TODO
  }

  private void handleOpenMarkup(ImporterContext context) {
    String methodName = "handleOpenMarkup";
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log(methodName, ruleName, modeName, token, context);
      switch (token.getType()) {
        case TAGMLLexer.NameOpenMarkup:
          TAGMarkup markup = context.newMarkup(token.getText());
          context.openMarkup(markup);
          break;
        case TAGMLLexer.END_OPEN_MARKUP:
          context.popOpenMarkup();
          goOn = false;
          break;
//        case TAGMLLexer.END_ANONYMOUS_RANGE:
//          TAGTextNode textNode = new TAGTextNode("");
//          update(textNode);
//          context.addTextNode(textNode);
//          context.closeMarkup();
//          goOn = false;
//          break;
        case TAGMLLexer.Annotation:
          handleAnnotation(context);
          break;

        default:
          handleUnexpectedToken(methodName, token, ruleName, modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleAnnotation(final ImporterContext context) {
    // TODO
  }

  private void handleCloseMarkup(TAGMLImporter.ImporterContext context) {
    String methodName = "handleCloseMarkup";
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log(methodName, ruleName, modeName, token, context);
      switch (token.getType()) {
        case TAGMLLexer.NameCloseMarkup:
          String rangeName = token.getText();
          context.pushOpenMarkup(rangeName);
          break;
//        case TAGMLLexer.BEGIN_OPEN_ANNO_IN_RANGE_CLOSER:
//          handleAnnotation(context);
//          break;
        case TAGMLLexer.END_CLOSE_MARKUP:
          context.closeMarkup();
          goOn = false;
          break;

        default:
          handleUnexpectedToken(methodName, token, ruleName, modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleUnexpectedToken(String methodName, Token token, String ruleName, String modeName) {
    String message = methodName + ": unexpected rule/token: token=" + token + ", ruleName=" + ruleName + ", mode=" + modeName;
    LOG.error(message);
    throw new TAGMLSyntaxError(message);
  }

  private void log(final String methodName, final String ruleName, final String modeName, final Token token, final ImporterContext context) {

  }

  private static Long update(TAGObject tagObject) {
    return tagStore.persist(tagObject);
  }

}
