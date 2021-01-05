package nl.knaw.huygens.alexandria.lmnl.importer;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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
import nl.knaw.huygens.alexandria.data_model.*;
import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

import static java.util.stream.Collectors.joining;

public class LMNLImporterInMemory {
  private static final Logger LOG = LoggerFactory.getLogger(LMNLImporterInMemory.class);

  private static void joinDiscontinuedRanges(Limen limen) {
    Map<String, Markup> markupsToJoin = new HashMap<>();
    List<Markup> markupsToRemove = new ArrayList<>();
    limen.markupList.stream()
        .filter(Markup::hasN)
        .forEach(
            markup -> {
              String tag = markup.getTag();
              Annotation nAnnotation =
                  markup
                      .getAnnotations()
                      .parallelStream()
                      .filter(a -> a.getTag().equals("n"))
                      .findFirst()
                      .get();
              String key = tag + "-" + annotationText(nAnnotation);
              if (markupsToJoin.containsKey(key)) {
                Markup originalMarkup = markupsToJoin.get(key);
                markup.getAnnotations().remove(nAnnotation);
                originalMarkup.joinWith(markup);
                markupsToRemove.add(markup);
              } else {
                markupsToJoin.put(key, markup);
              }
            });

    limen.markupList.removeAll(markupsToRemove);
    limen.markupList.stream()
        .map(Markup::getAnnotations)
        .flatMap(List::stream)
        .map(Annotation::value)
        .forEach(LMNLImporterInMemory::joinDiscontinuedRanges);
  }

  private Document importLMNL(CharStream antlrInputStream) throws LMNLSyntaxError {
    LMNLLexer lexer = new LMNLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);

    ImporterContext context = new ImporterContext(lexer);
    Document document = new Document();
    Limen limen = document.value();
    context.pushLimenContext(limen);
    handleDefaultMode(context);
    joinDiscontinuedRanges(document);
    context.popLimenContext();

    String errorMsg = "";
    if (context.hasErrors()) {
      String errors = String.join("\n", context.getErrors());
      errorMsg = "Parsing errors:\n" + errors;
    }
    if (errorListener.hasErrors()) {
      errorMsg += "\n\nTokenizing errors:\n" + errorListener.getPrefixedErrorMessagesAsString();
    }
    if (!errorMsg.isEmpty()) {
      throw new LMNLSyntaxError(errorMsg);
    }

    return document;
  }

  public Document importLMNL(String input) throws LMNLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importLMNL(antlrInputStream);
  }

  public Document importLMNL(InputStream input) throws LMNLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importLMNL(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
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
            TextNode textNode = new TextNode(token.getText());
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
          Markup markup = context.newMarkup(token.getText());
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
          TextNode textNode = new TextNode("");
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
    Annotation annotation = new Annotation("");
    context.openAnnotation(annotation);
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log(methodName, ruleName, modeName, token, context);
      switch (token.getType()) {
        case LMNLLexer.Name_Open_Annotation:
          annotation.setTag(token.getText());
          break;
        case LMNLLexer.OPEN_ANNO_IN_ANNO_OPENER:
        case LMNLLexer.OPEN_ANNO_IN_ANNO_CLOSER:
          handleAnnotation(context);
          break;
        case LMNLLexer.END_OPEN_ANNO:
          context.pushLimenContext(context.currentAnnotationLimen());
          break;

        case LMNLLexer.ANNO_TEXT:
          context.addTextNode(new TextNode(token.getText()));
          break;

        case LMNLLexer.BEGIN_ANNO_OPEN_RANGE:
          handleOpenRange(context);
          break;

        case LMNLLexer.BEGIN_ANNO_CLOSE_RANGE:
          handleCloseRange(context);
          break;

        case LMNLLexer.BEGIN_CLOSE_ANNO:
        case LMNLLexer.Name_Close_Annotation:
          break;
        case LMNLLexer.END_CLOSE_ANNO:
          context.popLimenContext();
        case LMNLLexer.END_EMPTY_ANNO:
          context.closeAnnotation();
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

  private void handleUnexpectedToken(
      String methodName, Token token, String ruleName, String modeName) {
    String message =
        methodName
            + ": unexpected rule/token: token="
            + token
            + ", ruleName="
            + ruleName
            + ", mode="
            + modeName;
    LOG.error(message);
    throw new LMNLSyntaxError(message);
  }

  private void log(
      String mode, String ruleName, String modeName, Token token, ImporterContext context) {
    // LOG.info("{}:\tlevel:{}, <{}> :\t{} ->\t{}",
    // mode, context.limenContextStack.size(),
    // token.getText().replace("\n", "\\n"),
    // ruleName, modeName);
  }

  private static void joinDiscontinuedRanges(Document document) {
    joinDiscontinuedRanges(document.value());
  }

  static class LimenContext {
    private final Limen limen;
    private final Deque<Markup> openMarkupDeque = new ArrayDeque<>();
    private final Stack<Markup> openMarkupStack = new Stack<>();
    private final Stack<Annotation> annotationStack = new Stack<>();
    private final ImporterContext importerContext;

    LimenContext(Limen limen, ImporterContext importerContext) {
      this.limen = limen;
      this.importerContext = importerContext;
    }

    void openMarkup(Markup markup) {
      openMarkupDeque.push(markup);
      openMarkupStack.push(markup);
      limen.addMarkup(markup);
    }

    void pushOpenMarkup(String rangeName) {
      // LOG.info("currentDocumentContext().openMarkupDeque={}",
      // openMarkupDeque.stream().map(Markup::getKey).collect(Collectors.toList()));
      Optional<Markup> findFirst =
          openMarkupDeque.stream()
              .filter(tr -> tr.getExtendedTag().equals(rangeName))
              .findFirst();
      if (findFirst.isPresent()) {
        Markup markup = findFirst.get();
        if (markup.textNodes.isEmpty()) {
          // every markup should have at least one textNode
          addTextNode(new TextNode(""));
          closeMarkup();
        }
        openMarkupStack.push(markup);
      } else {
        importerContext.errors.add(
            "Closing tag {" + rangeName + "] found without corresponding open tag.");
      }
    }

    void popOpenMarkup() {
      openMarkupStack.pop();
    }

    void closeMarkup() {
      if (!openMarkupStack.isEmpty()) {
        Markup markup = openMarkupStack.pop();
        openMarkupDeque.remove(markup);
      }
    }

    void addTextNode(TextNode textNode) {
      openMarkupDeque
          .descendingIterator()
          .forEachRemaining(tr -> tr.addTextNode(textNode));
      limen.addTextNode(textNode);
    }

    private Markup currentMarkup() {
      return openMarkupDeque.isEmpty() ? null : openMarkupStack.peek();
    }

    void openAnnotation(Annotation annotation) {
      if (annotationStack.isEmpty()) {
        Markup markup = currentMarkup();
        if (markup != null) {
          markup.addAnnotation(annotation);
        }
      } else {
        annotationStack.peek().addAnnotation(annotation);
      }
      annotationStack.push(annotation);
    }

    Limen currentAnnotationLimen() {
      return annotationStack.peek().value();
    }

    void closeAnnotation() {
      annotationStack.pop();
    }
  }

  private static String annotationText(Annotation annotation) {
    return annotation.value().textNodeList.stream().map(TextNode::getContent).collect(joining());
  }

  static class ImporterContext {
    private final Deque<LimenContext> limenContextStack = new ArrayDeque<>();
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

    void pushLimenContext(Limen limen) {
      limenContextStack.push(new LimenContext(limen, this));
    }

    LimenContext currentLimenContext() {
      return limenContextStack.peek();
    }

    LimenContext popLimenContext() {
      LimenContext limenContext = limenContextStack.pop();
      if (!limenContext.openMarkupDeque.isEmpty()) {
        String openRanges =
            limenContext.openMarkupDeque.stream()
                .map(m -> "[" + m.getExtendedTag() + "}")
                .collect(joining(", "));
        errors.add("Unclosed LMNL range(s): " + openRanges);
      }
      return limenContext;
    }

    Markup newMarkup(String tagName) {
      return new Markup(currentLimenContext().limen, tagName);
    }

    void openMarkup(Markup markup) {
      currentLimenContext().openMarkup(markup);
    }

    void pushOpenMarkup(String rangeName) {
      currentLimenContext().pushOpenMarkup(rangeName);
    }

    void popOpenMarkup() {
      currentLimenContext().popOpenMarkup();
    }

    void closeMarkup() {
      currentLimenContext().closeMarkup();
    }

    void addTextNode(TextNode textNode) {
      currentLimenContext().addTextNode(textNode);
    }

    void openAnnotation(Annotation annotation) {
      currentLimenContext().openAnnotation(annotation);
    }

    Limen currentAnnotationLimen() {
      return currentLimenContext().currentAnnotationLimen();
    }

    void closeAnnotation() {
      currentLimenContext().closeAnnotation();
    }

    List<String> getErrors() {
      return errors;
    }

    boolean hasErrors() {
      return !errors.isEmpty();
    }
  }
}
