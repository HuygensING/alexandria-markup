package nl.knaw.huygens.alexandria.lmnl.importer;

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

import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.creole.Basics;
import nl.knaw.huygens.alexandria.creole.Event;
import nl.knaw.huygens.alexandria.creole.events.*;
import nl.knaw.huygens.alexandria.data_model.*;
import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static nl.knaw.huygens.alexandria.creole.events.Events.textEvent;

public class LMNLImporter2 {
  private static final Logger LOG = LoggerFactory.getLogger(LMNLImporter2.class);

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
      // LOG.info("currentLimenContext().openMarkupDeque={}", openMarkupDeque.stream().map(Markup::getTag).collect(Collectors.toList()));
      Optional<Markup> findFirst = openMarkupDeque.stream()//
          .filter(tr -> tr.getExtendedTag().equals(rangeName))//
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
        importerContext.errors.add("Closing tag {" + rangeName + "] found without corresponding open tag.");
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
      openMarkupDeque.descendingIterator()//
          .forEachRemaining(tr -> tr.addTextNode(textNode));
      limen.addTextNode(textNode);
    }

    private Markup currentMarkup() {
      return openMarkupStack.isEmpty() ? null : openMarkupStack.peek();
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

  static class ImporterContext {
    private final Stack<LimenContext> limenContextStack = new Stack<>();
    private final LMNLLexer lexer;
    private final List<String> errors = new ArrayList<>();
    private final List<Event> eventList = new ArrayList<>();

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
        String openRanges = limenContext.openMarkupDeque.stream()//
            .map(m -> "[" + m.getExtendedTag() + "}")//
            .collect(Collectors.joining(", "));
        errors.add("Unclosed LMNL range(s): " + openRanges);
      }
      return limenContext;
    }

    Markup newMarkup(String tagName) {
      return new Markup(currentLimenContext().limen, tagName);
    }

    void openMarkup(Markup markup) {
      currentLimenContext().openMarkup(markup);
      addStartTagOpenEvent(markup);
    }

    void pushOpenMarkup(String rangeName) {
      currentLimenContext().pushOpenMarkup(rangeName);
    }

    void popOpenMarkup() {
      currentLimenContext().popOpenMarkup();
    }

    void closeMarkup() {
      Markup markup = currentLimenContext().currentMarkup();
      addEndTagCloseEvent(markup);
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
      Annotation annotation = currentLimenContext().annotationStack.peek();
      addEndAnnotationCloseEvent(annotation.getTag());
      currentLimenContext().closeAnnotation();
    }

    List<String> getErrors() {
      return errors;
    }

    boolean hasErrors() {
      return !errors.isEmpty();
    }

    List<Event> getEventList() {
      return eventList;
    }

    private void addStartTagOpenEvent(Markup markup) {
      checkNotNull(markup);
      Basics.QName qName = getQName(markup);
      Basics.Id id = getId(markup);
      eventList.add(new StartTagOpenEvent(qName, id));
    }

    void addStartTagCloseEvent(Markup markup) {
      checkNotNull(markup);
      Basics.QName qName = getQName(markup);
      Basics.Id id = getId(markup);
      eventList.add(new StartTagCloseEvent(qName, id));
    }

    void addEndTagOpenEvent(Markup markup) {
      checkNotNull(markup);
      Basics.QName qName = getQName(markup);
      Basics.Id id = getId(markup);
      eventList.add(new EndTagOpenEvent(qName, id));
    }

    void addEndTagCloseEvent(Markup markup) {
      checkNotNull(markup);
      Basics.QName qName = getQName(markup);
      Basics.Id id = getId(markup);
      eventList.add(new EndTagCloseEvent(qName, id));
    }

    private Basics.Id getId(Markup markup) {
      checkNotNull(markup);
      String idString = markup.hasId() ? markup.getId() : "";
      return Basics.id(idString);
    }

    private Basics.QName getQName(Markup markup) {
      checkNotNull(markup);
      return Basics.qName(markup.getTag());
    }

    void addTextEvent(String text) {
      eventList.add(textEvent(text));
    }

    void addStartAnnotationOpenEvent(String tag) {
      Event startAnnotationOpenEvent = Events.startAnnotationOpenEvent(Basics.qName(tag));
      eventList.add(startAnnotationOpenEvent);
    }

    void addStartAnnotationCloseEvent(String tag) {
      Event startAnnotationCloseEvent = Events.startAnnotationCloseEvent(Basics.qName(tag));
      eventList.add(startAnnotationCloseEvent);
    }

    void addEndAnnotationOpenEvent(String tag) {
      Event endAnnotationOpenEvent = Events.endAnnotationOpenEvent(Basics.qName(tag));
      eventList.add(endAnnotationOpenEvent);
    }

    void addEndAnnotationCloseEvent(String tag) {
      Event endAnnotationCloseEvent = Events.endAnnotationCloseEvent(Basics.qName(tag));
      eventList.add(endAnnotationCloseEvent);
    }


  }

  public List<Event> importLMNL(String input) throws LMNLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importLMNL(antlrInputStream);
  }

  public List<Event> importLMNL(InputStream input) throws LMNLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importLMNL(antlrInputStream);

    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private List<Event> importLMNL(CharStream antlrInputStream) throws LMNLSyntaxError {
    LMNLLexer lexer = new LMNLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);

    ImporterContext context = new ImporterContext(lexer);
    Document document = new Document();
    Limen limen = document.value();
    context.pushLimenContext(limen);
    handleDefaultMode(context);
    context.popLimenContext();

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
      throw new LMNLSyntaxError(errorMsg);
    }

    return context.getEventList();
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
            context.addTextEvent(token.getText());
            break;

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
          context.addStartTagCloseEvent(context.currentLimenContext().currentMarkup());
          context.popOpenMarkup();
          goOn = false;
          break;
        case LMNLLexer.END_ANONYMOUS_RANGE:
          TextNode textNode = new TextNode("");
          context.addTextNode(textNode);
          Markup markup2 = context.currentLimenContext().currentMarkup();
          context.addStartTagCloseEvent(markup2);
          context.addEndTagOpenEvent(markup2);
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
          context.addStartAnnotationOpenEvent(annotation.getTag());
          break;

        case LMNLLexer.OPEN_ANNO_IN_ANNO_OPENER:
        case LMNLLexer.OPEN_ANNO_IN_ANNO_CLOSER:
          if (annotation.getTag().isEmpty()) {
            context.addStartAnnotationOpenEvent("");
          }
          handleAnnotation(context);
          break;

        case LMNLLexer.END_OPEN_ANNO:
          context.addStartAnnotationCloseEvent(annotation.getTag());
          context.pushLimenContext(context.currentAnnotationLimen());
          break;

        case LMNLLexer.ANNO_TEXT:
          context.addTextNode(new TextNode(token.getText()));
          context.addTextEvent(token.getText());
          break;

        case LMNLLexer.BEGIN_ANNO_OPEN_RANGE:
          handleOpenRange(context);
          break;

        case LMNLLexer.BEGIN_ANNO_CLOSE_RANGE:
          handleCloseRange(context);
          break;

        case LMNLLexer.BEGIN_CLOSE_ANNO:
          context.addEndAnnotationOpenEvent(annotation.getTag());
          break;

        case LMNLLexer.Name_Close_Annotation:
          String tag = token.getText();
          if (!tag.equals(annotation.getTag())) {
            String message = String.format("Found unexpected annotation close tag {%s], expected {%s]", tag, annotation.getTag());
            throw new LMNLSyntaxError(message);
          }
          break;

        case LMNLLexer.END_CLOSE_ANNO:
          context.popLimenContext();
          context.closeAnnotation();
          goOn = false;
          break;

        case LMNLLexer.END_EMPTY_ANNO:
          context.addStartAnnotationCloseEvent(annotation.getTag());
          context.addEndAnnotationOpenEvent(annotation.getTag());
          context.closeAnnotation();
          goOn = false;
          break;

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
          Markup markup = context.currentLimenContext().currentMarkup();
          if (markup == null) {
            String message = String.format("%s: unexpected token: {%s]", methodName, rangeName);
            throw new LMNLSyntaxError(message);
          }
          context.addEndTagOpenEvent(markup);
          break;

        case LMNLLexer.BEGIN_OPEN_ANNO_IN_RANGE_CLOSER:
          handleAnnotation(context);
          break;

        case LMNLLexer.END_CLOSE_RANGE:
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
    String message = String.format("%s: unexpected rule/token: token=%s, ruleName=%s, mode=%s", methodName, token, ruleName, modeName);
    LOG.error(message);
    throw new LMNLSyntaxError(message);
  }

  private void log(String mode, String ruleName, String modeName, Token token, ImporterContext context) {
    // LOG.info("{}:\tlevel:{}, <{}> :\t{} ->\t{}", //
    // mode, context.limenContextStack.size(), //
    // token.getText().replace("\n", "\\n"), //
    // ruleName, modeName);
  }

}