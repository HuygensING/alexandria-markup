package lmnl_importer;

import data_model.*;
import lmnl_antlr.LMNLLexer;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Stack;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporter {
  Logger LOG = LoggerFactory.getLogger(LMNLImporter.class);

  static class ImporterContext {
    private Stack<Limen> limenStack = new Stack<>();
    private Stack<TextRange> textRangeStack = new Stack<>();
    private Stack<Annotation> annotationStack = new Stack<>();
    private LMNLLexer lexer;

    public ImporterContext(LMNLLexer lexer) {
      this.lexer = lexer;
    }

    public Token nextToken() {
      return lexer.nextToken();
    }

    public String getModeName() {
      return lexer.getModeNames()[lexer._mode];
    }

    public String getRuleName() {
      return lexer.getRuleNames()[lexer.getToken().getType() - 1];
    }

    public void pushLimen(Limen limen) {
      limenStack.push(limen);
    }

    public Limen currentLimen() {
      return limenStack.peek();
    }

    public void pushTextRange(TextRange textRange) {
      textRangeStack.push(textRange);
      currentLimen().addTextRange(textRange);
    }

    private TextRange currentTextRange() {
      return textRangeStack.peek();
    }

//    public TextRange currentTextRange() {
//      return textRangeStack.peek();
//    }

    public void addTextNode(TextNode textNode) {
      currentTextRange().addTextNode(textNode);
      currentLimen().addTextNode(textNode);
    }

    public void addAnnotation(Annotation annotation) {
      currentTextRange().addAnnotation(annotation);
      annotationStack.push(annotation);
    }

    public Limen currentAnnotationLimen() {
      return annotationStack.peek().value();
    }

    public void closeCurrentAnnotation() {
      annotationStack.pop();
    }
  }

  public Document importLMNL(String input) {
    ANTLRInputStream antlrInputStream = new ANTLRInputStream(input);
    return importLMNL(antlrInputStream);
  }

  public Document importLMNL(InputStream input) {
    try {
      ANTLRInputStream antlrInputStream = new ANTLRInputStream(input);
      return importLMNL(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Document importLMNL(ANTLRInputStream antlrInputStream) {
    LMNLLexer lexer = new LMNLLexer(antlrInputStream);
    ImporterContext context = new ImporterContext(lexer);
    Document document = new Document();
    Limen limen = document.value();
    context.pushLimen(limen);
    handleDefaultMode(context);
    return document;
  }

  private void handleDefaultMode(ImporterContext context) {
    Token token;
    do {
      token = context.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = context.getRuleName();
        String modeName = context.getModeName();
        log("defaultMode", ruleName, modeName, token);
        switch (ruleName) {
          case "BEGIN_OPEN_RANGE":
            handleOpenRange(context);
            break;

          case "BEGIN_CLOSE_RANGE":
            handleCloseRange(context);
            break;

          case "TEXT":
            TextNode textNode = new TextNode(token.getText());
            context.addTextNode(textNode);
            break;

          default:
            LOG.error("!unexpected token: " + token + ": " + ruleName + ": " + modeName);
            break;
        }
      }
    } while (token.getType() != Token.EOF);
  }


  private void handleOpenRange(ImporterContext context) {
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log("handleOpenRange", ruleName, modeName, token);
      switch (ruleName) {
        case "Name_Open_Range":
          TextRange textRange = new TextRange(context.currentLimen(), token.getText());
          context.pushTextRange(textRange);
          break;
        case "END_OPEN_RANGE":
          goOn = false;
          break;

        case "BEGIN_OPEN_ANNO":
          handleAnnotation(context);
          break;

        default:
          String handleOpenRange = "handleOpenRange";
          LOG.error(handleOpenRange + ": unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleAnnotation(ImporterContext context) {
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log("handleAnnotation", ruleName, modeName, token);
      switch (ruleName) {
        case "Name_Open_Annotation":
          Annotation annotation = new Annotation(token.getText());
          context.addAnnotation(annotation);
          break;
        case "ANNO_TEXT":
          context.currentAnnotationLimen().addTextNode(new TextNode(token.getText()));
          break;
        case "END_OPEN_ANNO":
          break;
        case "OPEN_ANNO_IN_ANNO":
          handleAnnotation(context);
          break;
        case "BEGIN_CLOSE_ANNO":
          break;
        case "Name_Close_Annotation":
          break;
        case "END_ANONYMOUS_ANNO":
        case "END_CLOSE_ANNO":
          context.closeCurrentAnnotation();
          goOn = false;
          break;
        default:
          LOG.error("handleAnnotation: unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void handleCloseRange(ImporterContext context) {
    boolean goOn = true;
    while (goOn) {
      Token token = context.nextToken();
      String ruleName = context.getRuleName();
      String modeName = context.getModeName();
      log("handleCloseRange", ruleName, modeName, token);
      switch (ruleName) {
        case "Name_Close_Range":
          break;
        case "END_CLOSE_RANGE":
          goOn = false;
          break;
        default:
          String handleCloseRange = "handleCloseRange";
          LOG.error(handleCloseRange + ": unexpected token: " + token + ": " + ruleName + ": " + modeName);
          break;
      }
      goOn = goOn && token.getType() != Token.EOF;
    }
  }

  private void log(String mode, String ruleName, String modeName, Token token) {
    LOG.info("{}:\truleName:{},\tmodeName:{},\ttoken:{}", mode, ruleName, modeName, token);
  }

}
