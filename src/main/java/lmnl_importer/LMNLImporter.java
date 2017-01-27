package lmnl_importer;

import data_model.Document;
import data_model.Limen;
import data_model.TextNode;
import data_model.TextRange;
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
    Stack<TextRange> textRangeCache = new Stack<>();
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = lexer.getRuleNames()[token.getType() - 1];
        String modeName = lexer.getModeNames()[lexer._mode];
        System.out.println(token + ": " + ruleName + ": " + modeName);
        switch (ruleName) {
          case "BEGIN_OPEN_RANGE":
            break;
          case "Name_Open_Range":
            TextRange textRange = new TextRange(limen, token.getText());
            textRangeCache.push(textRange);
            break;
          case "END_OPEN_RANGE":
            break;
          case "TEXT":
            TextNode textNode = new TextNode(token.getText());
            textRangeCache.peek().addTextNode(textNode);
            limen.addTextNode(textNode);
            break;
          case "BEGIN_CLOSE_RANGE":
            break;
          case "Name_Close_Range":
            break;
          case "END_CLOSE_RANGE":
            textRangeCache.pop();
            break;
          default:
            break;
        }
      }
    } while (token.getType() != Token.EOF);
    return document;
  }

}
