package nl.knaw.huygens.alexandria.lmnl.importer;

/*
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

import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer;
import nl.knaw.huygens.alexandria.storage.TAGAnnotationDAO;
import nl.knaw.huygens.alexandria.storage.TAGDocumentDAO;
import nl.knaw.huygens.alexandria.storage.TAGMarkupDAO;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.dto.*;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Collectors;

public class LMNLImporter {
  private static final Logger LOG = LoggerFactory.getLogger(LMNLImporter.class);

  private static TAGStore tagStore;

  public LMNLImporter(TAGStore tagStore) {
    LMNLImporter.tagStore = tagStore;
  }

  static class DocumentContext {
    private final TAGDocument document;
    private final Deque<TAGMarkup> openMarkupDeque = new ArrayDeque<>();
    private final Stack<TAGMarkup> openMarkupStack = new Stack<>();
    private final Stack<TAGAnnotation> annotationStack = new Stack<>();
    private final ImporterContext importerContext;

    DocumentContext(TAGDocument document, ImporterContext importerContext) {
      this.document = document;
      this.importerContext = importerContext;
    }

    void openMarkup(TAGMarkup markup) {
      openMarkupDeque.push(markup);
      openMarkupStack.push(markup);
      document.getMarkupIds().add(markup.getDbId());
    }

    void pushOpenMarkup(String rangeName) {
      // LOG.info("currentDocumentContext().openMarkupDeque={}", openMarkupDeque.stream().map(Markup::getKey).collect(Collectors.toList()));
      Optional<TAGMarkupDAO> findFirst = openMarkupDeque.stream()//
          .map(dto -> new TAGMarkupDAO(tagStore, dto))
          .filter(m -> m.getExtendedTag().equals(rangeName))//
          .findFirst();
      if (findFirst.isPresent()) {
        TAGMarkupDAO markup = findFirst.get();
        if (!document.markupHasTextNodes(markup)) {
          // every markup should have at least one textNode
          TAGTextNode emptyTextNode = new TAGTextNode("");
          update(emptyTextNode);
          addTextNode(emptyTextNode);
          closeMarkup();
        }
        openMarkupStack.push(markup.getDTO());
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
//            m.addTextNode(textNode);
            document.associateTextWithMarkupForLayer(textNode, m, "");
          });
      if (!document.hasTextNodes()) {
        document.setFirstTextNodeId(textNode.getDbId());
      }
      document.addTextNode(textNode);
    }

    private TAGMarkup currentMarkup() {
      return openMarkupDeque.isEmpty() ? null : openMarkupStack.peek();
    }

    void openAnnotation(TAGAnnotation annotation) {
      if (annotationStack.isEmpty()) {
        TAGMarkup markup = currentMarkup();
        if (markup != null) {
//          markup.addAnnotation(annotation);
        }
      } else {
//        annotationStack.peek().addAnnotation(annotation);
      }
      annotationStack.push(annotation);
    }

    TAGDocument currentAnnotationDocument() {
      return null;
//      Long value = annotationStack.peek().getDocumentId();
//      return tagStore.getDocumentDTO(value);
    }

    void closeAnnotation() {
      TAGAnnotation annotation = annotationStack.pop();
      update(annotation);
    }
  }

  static class ImporterContext {
    private final Deque<DocumentContext> documentContextStack = new ArrayDeque<>();
    private final LMNLLexer lexer;
    private final List<String> errors = new ArrayList<>();

    ImporterContext(LMNLLexer lexer) {
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

    DocumentContext currentDocumentContext() {
      return documentContextStack.peek();
    }

    DocumentContext popDocumentContext() {
      DocumentContext documentContext = documentContextStack.pop();
      update(documentContext.document);
      if (!documentContext.openMarkupDeque.isEmpty()) {
        String openRanges = documentContext.openMarkupDeque.stream()//
            .map(dto -> new TAGMarkupDAO(tagStore, dto))
            .map(m -> "[" + m.getExtendedTag() + "}")//
            .collect(Collectors.joining(", "));
        errors.add("Unclosed LMNL range(s): " + openRanges);
      }
      return documentContext;
    }

    TAGMarkup newMarkup(String tagName) {
      TAGMarkup tagMarkup = new TAGMarkup(currentDocumentContext().document.getDbId(), tagName);
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

  public TAGDocumentDAO importLMNL(String input) throws LMNLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importLMNL(antlrInputStream);
  }

  public TAGDocumentDAO importLMNL(InputStream input) throws LMNLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importLMNL(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private TAGDocumentDAO importLMNL(CharStream antlrInputStream) throws LMNLSyntaxError {
    LMNLLexer lexer = new LMNLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);

    ImporterContext context = new ImporterContext(lexer);
    TAGDocumentDAO document = tagStore.createDocument();
    TAGDocument dto = document.getDTO();
    update(dto);
    context.pushDocumentContext(dto);
    handleDefaultMode(context);
    joinDiscontinuedRanges(document);
    context.popDocumentContext();

    String errorMsg = "";
    if (context.hasErrors()) {
      String errors = String.join("\n", context.getErrors());
      errorMsg = "Parsing errors:\n" + errors;
    }
    if (errorListener.hasErrors()) {
      String errors = String.join("\n", errorListener.getErrors());
      errorMsg += "\n\nTokenizing errors:\n" + errors;
    }
    if (!errorMsg.isEmpty()) {
      throw new LMNLSyntaxError(errorMsg);
    }
    update(dto);
    return document;
  }

  private void handleDefaultMode(ImporterContext context) {
    String methodName = "defaultMode";
    Token token;
    do {
      token = context.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = context.getRuleName();
        String modeName = context.getModeName();
        log(methodName, ruleName, modeName, token, context);
        switch (token.getType()) {
          case LMNLLexer.BEGIN_OPEN_RANGE:
            handleOpenRange(context);
            break;

          case LMNLLexer.BEGIN_CLOSE_RANGE:
            handleCloseRange(context);
            break;

          case LMNLLexer.TEXT:
            TAGTextNode textNode = new TAGTextNode(token.getText());
            update(textNode);
            context.addTextNode(textNode);
            break;

          // case LMNLLexer.TagOpenStartChar:
          // case LMNLLexer.TagOpenEndChar:
          // case LMNLLexer.TagCloseStartChar:
          // case LMNLLexer.TagCloseEndChar:
          // break;

          default:
            handleUnexpectedToken(methodName, token, ruleName, modeName);
            break;
        }
      }
    } while (token.getType() != Token.EOF);
  }

  private void handleOpenRange(ImporterContext context) {
    String methodName = "handleOpenRange";
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log(methodName, ruleName, modeName, token, context);
      switch (token.getType()) {
        case LMNLLexer.Name_Open_Range:
          TAGMarkup markup = context.newMarkup(token.getText());
          context.openMarkup(markup);
          break;
        case LMNLLexer.BEGIN_OPEN_ANNO:
          handleAnnotation(context);
          break;
        case LMNLLexer.END_OPEN_RANGE:
          context.popOpenMarkup();
          goOn = false;
          break;
        case LMNLLexer.END_ANONYMOUS_RANGE:
          TAGTextNode textNode = new TAGTextNode("");
          update(textNode);
          context.addTextNode(textNode);
          context.closeMarkup();
          goOn = false;
          break;

        // case LMNLLexer.TagOpenStartChar:
        // case LMNLLexer.TagOpenEndChar:
        // case LMNLLexer.TagCloseStartChar:
        // case LMNLLexer.TagCloseEndChar:
        // break;

        default:
          handleUnexpectedToken(methodName, token, ruleName, modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleAnnotation(ImporterContext context) {
    String methodName = "handleAnnotation";
//    TAGAnnotationDTO annotation = tagStore.createAnnotationDTO("");
//    context.openAnnotation(annotation);
//    boolean goOn = true;
//    while (goOn) {
//      Token token = context.nextToken();
//      String ruleName = context.getRuleName();
//      String modeName = context.getModeName();
//      log(methodName, ruleName, modeName, token, context);
//      switch (token.getType()) {
//        case LMNLLexer.Name_Open_Annotation:
//          annotation.setKey(token.getText());
//          break;
//        case LMNLLexer.OPEN_ANNO_IN_ANNO_OPENER:
//        case LMNLLexer.OPEN_ANNO_IN_ANNO_CLOSER:
//          handleAnnotation(context);
//          break;
//        case LMNLLexer.END_OPEN_ANNO:
//          context.pushDocumentContext(context.currentAnnotationDocument());
//          break;
//
//        case LMNLLexer.ANNO_TEXT:
//          TAGTextNodeDTO textNode = new TAGTextNodeDTO(token.getText());
//          update(textNode);
//          context.addTextNode(textNode);
//          break;
//
//        case LMNLLexer.BEGIN_ANNO_OPEN_RANGE:
//          handleOpenRange(context);
//          break;
//
//        case LMNLLexer.BEGIN_ANNO_CLOSE_RANGE:
//          handleCloseRange(context);
//          break;
//
//        case LMNLLexer.BEGIN_CLOSE_ANNO:
//        case LMNLLexer.Name_Close_Annotation:
//          break;
//        case LMNLLexer.END_CLOSE_ANNO:
//          context.popDocumentContext();
//        case LMNLLexer.END_EMPTY_ANNO:
//          context.closeAnnotation();
//          goOn = false;
//          break;
//
//        // case LMNLLexer.TagOpenStartChar:
//        // case LMNLLexer.TagOpenEndChar:
//        // case LMNLLexer.TagCloseStartChar:
//        // case LMNLLexer.TagCloseEndChar:
//        // break;
//
//        default:
//          handleUnexpectedToken(methodName, token, ruleName, modeName);
//          break;
//      }
//      goOn = goOn && token.getType() != Token.EOF;
//    }
  }

  private void handleCloseRange(ImporterContext context) {
    String methodName = "handleCloseRange";
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log(methodName, ruleName, modeName, token, context);
      switch (token.getType()) {
        case LMNLLexer.Name_Close_Range:
          String rangeName = token.getText();
          context.pushOpenMarkup(rangeName);
          break;
        case LMNLLexer.BEGIN_OPEN_ANNO_IN_RANGE_CLOSER:
          handleAnnotation(context);
          break;
        case LMNLLexer.END_CLOSE_RANGE:
          context.closeMarkup();
          goOn = false;
          break;

        // case LMNLLexer.TagOpenStartChar:
        // case LMNLLexer.TagOpenEndChar:
        // case LMNLLexer.TagCloseStartChar:
        // case LMNLLexer.TagCloseEndChar:
        // break;

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
    throw new LMNLSyntaxError(message);
  }

//  private static void joinDiscontinuedRanges(Document document) {
//    joinDiscontinuedRanges(document.getDocumentId());
//  }

  private static void joinDiscontinuedRanges(TAGDocumentDAO document) {
//    Map<String, TAGMarkupDTO> markupsToJoin = new HashMap<>();
//    List<Long> markupIdsToRemove = new ArrayList<>();
//    document.getMarkupStream()//
//        .filter(TAGMarkup::hasN)//
//        .forEach(markup -> {
//          String tag = markup.getTag();
//          AnnotationInfo annotation = markup.getAnnotationStream()//
//              .filter(a -> a.getName().equals("n"))//
//              .findFirst()//
//              .get();
//          String key = tag + "-" + annotationText(annotation);
//          if (markupsToJoin.containsKey(key)) {
//            TAGMarkupDTO originalMarkup = markupsToJoin.get(key);
//            markup.getDTO().getAnnotationIds().remove(annotation.getDbId());
////            document.joinMarkup(originalMarkup, markup);
//            markupIdsToRemove.add(markup.getDbId());
//          } else {
//            markupsToJoin.put(key, markup.getDTO());
//          }
//        });
//
//    document.getDTO().getMarkupIds().removeAll(markupIdsToRemove);
////    document.getMarkupStream()//
////        .map(TAGMarkup::getAnnotationStream)//
////        .flatMap(Function.identity())//
////        .map(TAGAnnotation::getDocument)//
////        .forEach(LMNLImporter::joinDiscontinuedRanges);
  }

  private static String annotationText(TAGAnnotationDAO annotation) {
    return "TODO";
//    return annotation.getDocument().getTextNodeStream()//
//        .map(TAGTextNode::getText)//
//        .collect(joining());
  }

  private void log(String mode, String ruleName, String modeName, Token token, ImporterContext context) {
    // LOG.info("{}:\tlevel:{}, <{}> :\t{} ->\t{}", //
    // mode, context.limenContextStack.size(), //
    // token.getText().replace("\n", "\\n"), //
    // ruleName, modeName);
  }

  private static Long update(TAGDTO tagdto) {
    return tagStore.persist(tagdto);
  }

}
