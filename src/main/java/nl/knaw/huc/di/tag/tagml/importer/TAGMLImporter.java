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

import com.google.common.collect.ImmutableMap;
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
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class TAGMLImporter {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporter.class);

  private final TAGStore tagStore;

  public TAGMLImporter(final TAGStore store) {
    tagStore = store;
  }

  private class DocumentContext {
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
        importerContext.errors.add(format("%s Closing tag <%s] found without corresponding open tag.",
            errorPrefix(importerContext.getCurrentToken()), rangeName));
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
      return tagStore.getDocument(value);
    }

    void closeAnnotation() {
      TAGAnnotation annotation = annotationStack.pop();
      update(annotation);
    }
  }

  private class ImporterContext {
    private final Deque<DocumentContext> documentContextStack = new ArrayDeque<>();
    private final TAGMLLexer lexer;
    private final List<String> errors = new ArrayList<>();
    private String methodName;
    private Token currentToken;

    ImporterContext(TAGMLLexer lexer) {
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

  private final Map<Integer, Function<ImporterContext, Boolean>> defaultModeHandlers =
      ImmutableMap.<Integer, Function<ImporterContext, Boolean>>builder()
          .put(TAGMLLexer.BEGIN_OPEN_MARKUP, this::handleOpenMarkup)
          .put(TAGMLLexer.BEGIN_CLOSE_MARKUP, this::handleCloseMarkup)
          .put(TAGMLLexer.BEGIN_TEXT_VARIATION, this::handleTextVariation)
          .put(TAGMLLexer.TEXT, this::handleText)
          .put(TAGMLLexer.COMMENT, this::handleComment)
          .put(TAGMLLexer.NAMESPACE, this::handleNamespace)
          .put(TAGMLLexer.EOF, this::handleEOF)
          .build();

  private void handleDefaultMode(TAGMLImporter.ImporterContext context) {
    handle("defaultMode", defaultModeHandlers, context);
  }

  private final Map<Integer, Function<ImporterContext, Boolean>> openMarkupHandlers =
      ImmutableMap.<Integer, Function<ImporterContext, Boolean>>builder()
          .put(TAGMLLexer.NameOpenMarkup, this::handleNameOpenMarkup)
          .put(TAGMLLexer.END_OPEN_MARKUP, this::handleEndOpenMarkup)
          .put(TAGMLLexer.END_ANONYMOUS_MARKUP, this::handleEndAnonymousMarkup)
//          .put(TAGMLLexer.Annotation, this::handleAnnotation)
          .build();


  private Boolean handleOpenMarkup(ImporterContext context) {
    handle("handleOpenMarkup", openMarkupHandlers, context);
    return true;
  }

  private final Map<Integer, Function<ImporterContext, Boolean>> closeMarkupHandlers =
      ImmutableMap.<Integer, Function<ImporterContext, Boolean>>builder()
          .put(TAGMLLexer.NameCloseMarkup, this::handleNameCloseMarkup)
          .put(TAGMLLexer.END_CLOSE_MARKUP, this::handleEndCloseMarkup)
          .build();

  private Boolean handleCloseMarkup(ImporterContext context) {
    handle("handleCloseMarkup", closeMarkupHandlers, context);
    return true;
  }

  private Boolean handleNameCloseMarkup(ImporterContext context) {
    Token token = context.getCurrentToken();
    ExtendedMarkupName eName = ExtendedMarkupName.of(token.getText());
    context.pushOpenMarkup(eName.getExtendedMarkupName());
    return true;
  }

  private Boolean handleEndCloseMarkup(ImporterContext context) {
    context.closeMarkup();
    return false;
  }

  private final Map<Integer, Function<ImporterContext, Boolean>> textVariationHandlers =
      ImmutableMap.<Integer, Function<ImporterContext, Boolean>>builder()
          .put(TAGMLLexer.TV_BEGIN_OPEN_MARKUP, this::handleOpenMarkup)
          .put(TAGMLLexer.TV_BEGIN_CLOSE_MARKUP, this::handleCloseMarkup)
          .put(TAGMLLexer.END_TEXT_VARIATION, this::handleEndTextVariation)
          .put(TAGMLLexer.VARIANT_TEXT, this::handleVariantText)
          .put(TAGMLLexer.TextVariationSeparator, this::handleTextVariationSeparator)
          .build();

  private Boolean handleTextVariation(ImporterContext context) {
    handle("handleTextVariation", textVariationHandlers, context);
    return true;
  }

  private Boolean handleVariantText(ImporterContext context) {
    TAGTextNode textNode = new TAGTextNode(context.getCurrentToken().getText());
    update(textNode);
    context.addTextNode(textNode);
    return true;
  }

  private Boolean handleTextVariationSeparator(ImporterContext context) {
    // TODO
    return true;
  }

  private Boolean handleEndTextVariation(ImporterContext context) {
    // TODO
    return false;
  }

  private Boolean handleNameOpenMarkup(ImporterContext context) {
    Token token = context.getCurrentToken();
    ExtendedMarkupName eName = ExtendedMarkupName.of(token.getText());
    TAGMarkup markup = context.newMarkup(eName.getTagName())
        // TODO: handle id, suspend/resume, optional
        .setMarkupId(eName.getId())
        .setOptional(eName.isOptional());
    context.openMarkup(markup);
    return true;
  }

  private Boolean handleEndAnonymousMarkup(ImporterContext context) {
    TAGTextNode textNode = new TAGTextNode("");
    update(textNode);
    context.addTextNode(textNode);
    context.closeMarkup();
    return false;
  }

  private Boolean handleEndOpenMarkup(ImporterContext context) {
    context.popOpenMarkup();
    return false;
  }

  private Boolean handleText(ImporterContext context) {
    TAGTextNode textNode = new TAGTextNode(context.getCurrentToken().getText());
    update(textNode);
    context.addTextNode(textNode);
    return true;
  }

  private Boolean handleComment(ImporterContext context) {
    // TODO
    return true;
  }

  private Boolean handleNamespace(ImporterContext context) {
    // TODO
    return true;
  }

  private Boolean handleAnnotation(final ImporterContext context) {
    // TODO
    return true;
  }

  private Boolean handleEOF(ImporterContext context) {
    return false;
  }

  private void handle(String methodName, Map<Integer, Function<ImporterContext, Boolean>> handlers, ImporterContext context) {
    context.setMethodName(methodName);
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      goOn = handlers.getOrDefault(token.getType(), this::handleUnexpectedToken).apply(context)
          && token.getType() != Token.EOF;
    }
  }

  private Boolean handleUnexpectedToken(ImporterContext context) {
    String message = format(
        "%s: unexpected rule/token: token=%s, ruleName=%s, mode=%s",
        context.getMethodName(),
        context.getCurrentToken(),
        context.getRuleName(),
        context.getModeName());
    LOG.error(message);
    throw new TAGMLSyntaxError(message);
  }

  private Long update(TAGObject tagObject) {
    return tagStore.persist(tagObject);
  }

}
