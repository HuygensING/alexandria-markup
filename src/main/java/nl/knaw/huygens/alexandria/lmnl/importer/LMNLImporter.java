package nl.knaw.huygens.alexandria.lmnl.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporter {
  static final Logger LOG = LoggerFactory.getLogger(LMNLImporter.class);

  static class LimenContext {
    private Limen limen;
    private final Deque<TextRange> openTextRangeDeque = new ArrayDeque<>();
    private final Stack<TextRange> openTextRangeStack = new Stack<>();
    private final Stack<Annotation> annotationStack = new Stack<>();

    LimenContext(Limen limen) {
      this.limen = limen;
    }

    void openTextRange(TextRange textRange) {
      openTextRangeDeque.push(textRange);
      openTextRangeStack.push(textRange);
      limen.addTextRange(textRange);
    }

    void pushOpenTextRange(String rangeName) {
      // LOG.info("currentLimenContext().openTextRangeDeque={}", openTextRangeDeque.stream().map(TextRange::getTag).collect(Collectors.toList()));
      TextRange textRange = openTextRangeDeque.stream()//
          .filter(tr -> tr.getExtendedTag().equals(rangeName))//
          .findFirst()//
          .get();
      if (textRange.textNodes.isEmpty()) {
        // every textRange should have at least one textNode
        addTextNode(new TextNode(""));
        closeTextRange();
      }
      openTextRangeStack.push(textRange);
    }

    void popOpenTextRange() {
      openTextRangeStack.pop();
    }

    void closeTextRange() {
      if (!openTextRangeStack.isEmpty()) {
        TextRange textrange = openTextRangeStack.pop();
        openTextRangeDeque.remove(textrange);
      }
    }

    void addTextNode(TextNode textNode) {
      openTextRangeDeque.descendingIterator()//
          .forEachRemaining(tr -> tr.addTextNode(textNode));
      limen.addTextNode(textNode);
    }

    private TextRange currentTextRange() {
      return openTextRangeDeque.isEmpty() ? null : openTextRangeStack.peek();
    }

    void openAnnotation(Annotation annotation) {
      if (annotationStack.isEmpty()) {
        TextRange textRange = currentTextRange();
        if (textRange != null) {
          textRange.addAnnotation(annotation);
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
      limenContextStack.push(new LimenContext(limen));
    }

    LimenContext currentLimenContext() {
      return limenContextStack.peek();
    }

    LimenContext popLimenContext() {
      return limenContextStack.pop();
    }

    TextRange newTextRange(String tagName) {
      return new TextRange(currentLimenContext().limen, tagName);
    }

    void openTextRange(TextRange textRange) {
      currentLimenContext().openTextRange(textRange);
    }

    void pushOpenTextRange(String rangeName) {
      currentLimenContext().pushOpenTextRange(rangeName);
    }

    void popOpenTextRange() {
      currentLimenContext().popOpenTextRange();
    }

    void closeTextRange() {
      currentLimenContext().closeTextRange();
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

  }

  public Document importLMNL(String input) {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importLMNL(antlrInputStream);
  }

  public Document importLMNL(InputStream input) {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importLMNL(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Document importLMNL(CharStream antlrInputStream) {
    LMNLLexer lexer = new LMNLLexer(antlrInputStream);
    ImporterContext context = new ImporterContext(lexer);
    Document document = new Document();
    Limen limen = document.value();
    context.pushLimenContext(limen);
    handleDefaultMode(context);
    joinDiscontinuedRanges(document);
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
        TextRange textRange = context.newTextRange(token.getText());
        context.openTextRange(textRange);
        break;
      case LMNLLexer.BEGIN_OPEN_ANNO:
        handleAnnotation(context);
        break;
      case LMNLLexer.END_OPEN_RANGE:
        context.popOpenTextRange();
        goOn = false;
        break;
      case LMNLLexer.END_ANONYMOUS_RANGE:
        TextNode textNode = new TextNode("");
        context.addTextNode(textNode);
        context.closeTextRange();
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
        context.pushOpenTextRange(rangeName);
        break;
      case LMNLLexer.BEGIN_OPEN_ANNO_IN_RANGE_CLOSER:
        handleAnnotation(context);
        break;
      case LMNLLexer.END_CLOSE_RANGE:
        context.closeTextRange();
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
    throw new RuntimeException(message);
  }

  private static void joinDiscontinuedRanges(Document document) {
    joinDiscontinuedRanges(document.value());
  }

  public static void joinDiscontinuedRanges(Limen limen) {
    Map<String, TextRange> textRangesToJoin = new HashMap<>();
    List<TextRange> textRangesToRemove = new ArrayList<>();
    limen.textRangeList.stream()//
        .filter(TextRange::hasId)//
        .forEach(textRange -> {
          String tag = textRange.getExtendedTag();
          if (textRangesToJoin.containsKey(tag)) {
            TextRange originalTextRange = textRangesToJoin.get(tag);
            originalTextRange.joinWith(textRange);
            textRangesToRemove.add(textRange);
          } else {
            textRangesToJoin.put(tag, textRange);
          }
        });

    limen.textRangeList.removeAll(textRangesToRemove);
    limen.textRangeList.stream()//
        .map(TextRange::getAnnotations)//
        .flatMap(List::stream)//
        .map(Annotation::value)//
        .forEach(LMNLImporter::joinDiscontinuedRanges);
  }

  private void log(String mode, String ruleName, String modeName, Token token, ImporterContext context) {
    // LOG.info("{}:\tlevel:{}, <{}> :\t{} ->\t{}", //
    // mode, context.limenContextStack.size(), //
    // token.getText().replace("\n", "\\n"), //
    // ruleName, modeName);
  }

}
