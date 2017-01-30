package lmnl_importer;

import data_model.*;
import lmnl_antlr.LMNLLexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.Token;

import java.util.Stack;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporter {

  public Document importLMNL(String input) {
    LMNLLexer lexer = new LMNLLexer(new ANTLRInputStream(input));
    Document document = new Document();
    Limen limen = document.value();
    Token token;
    Stack<TextRange> textRangeStack = new Stack<>();
    Stack<Annotation> annotationStack = new Stack<>();
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = getRuleName(lexer, token);
        String modeName = getModeName(lexer);
        System.out.println(token + ": " + ruleName + ": " + modeName);
        switch (ruleName) {
          case "BEGIN_OPEN_RANGE":
            handleOpenRange(lexer, textRangeStack, limen, annotationStack);
            break;

          case "BEGIN_CLOSE_RANGE":
            handleCloseRange(lexer, textRangeStack, limen);
            break;

          case "TEXT":
            TextNode textNode = new TextNode(token.getText());
            textRangeStack.peek().addTextNode(textNode);
            limen.addTextNode(textNode);
            break;

          default:
            System.out.println("!unexpected token: " + token + ": " + ruleName + ": " + modeName);
            break;
        }
      }
    } while (token.getType() != Token.EOF);
    return document;
  }


  private String getModeName(LMNLLexer lexer) {
    return lexer.getModeNames()[lexer._mode];
  }

  private String getRuleName(LMNLLexer lexer, Token token) {
    return lexer.getRuleNames()[token.getType() - 1];
  }

  private void handleOpenRange(LMNLLexer lexer, Stack<TextRange> textRangeStack, Limen limen, Stack<Annotation> annotationStack) {
    boolean goOn = true;
    while (goOn) {
      Token token = lexer.nextToken();
      String ruleName = getRuleName(lexer, token);
      String modeName = getModeName(lexer);
      switch (ruleName) {
        case "Name_Open_Range":
          TextRange textRange = new TextRange(limen, token.getText());
          textRangeStack.push(textRange);
          break;
        case "END_OPEN_RANGE":
          goOn = false;
          break;

        case "BEGIN_OPEN_ANNO":
          handleOpenAnnotation(lexer, textRangeStack, limen, annotationStack);
          break;

        case "BEGIN_CLOSE_ANNO":
          break;
        case "Name_Close_Annotation":
          break;
        case "END_CLOSE_ANNO":
          textRangeStack.peek().addAnnotation(annotationStack.pop());
          break;
        default:
          System.out.println("!unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
    }
  }

  private void handleOpenAnnotation(LMNLLexer lexer, Stack<TextRange> textRangeStack, Limen limen, Stack<Annotation> annotationStack) {
    boolean goOn = true;
    while (goOn) {
      Token token = lexer.nextToken();
      String ruleName = getRuleName(lexer, token);
      String modeName = getModeName(lexer);
      switch (ruleName) {
        case "Name_Open_Annotation":
          Annotation annotation = new Annotation(token.getText());
          annotationStack.push(annotation);
          break;
        case "ANNO_TEXT":
          annotationStack.peek().value().addTextNode(new TextNode(token.getText()));
          break;
        case "END_OPEN_ANNO":
          goOn = false;
          break;
        default:
          System.out.println("!unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
    }
  }

  private void handleCloseRange(LMNLLexer lexer, Stack<TextRange> textRangeStack, Limen limen) {
    boolean goOn = true;
    while (goOn) {
      Token token = lexer.nextToken();
      String ruleName = getRuleName(lexer, token);
      String modeName = getModeName(lexer);
      switch (ruleName) {
        case "Name_Close_Range":
          break;
        case "END_CLOSE_RANGE":
          limen.addTextRange(textRangeStack.pop());
          goOn = false;
          break;
        default:
          System.out.println("!unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
    }
  }

}
