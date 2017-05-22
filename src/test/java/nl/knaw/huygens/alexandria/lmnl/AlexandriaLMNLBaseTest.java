package nl.knaw.huygens.alexandria.lmnl;

import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.InputStream;

public class AlexandriaLMNLBaseTest {

  protected void printTokens(String input) {
    // This gets all the tokens at once, it does not stop for errors
    // List<? extends Token> allTokens = grammar.getAllTokens();
    // System.out.println(allTokens);
    System.out.println("LMNL:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    LMNLLexer lexer = new LMNLLexer(inputStream);
    Token token;
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        System.out.println(token + ": " + lexer.getRuleNames()[token.getType() - 1] + ": " + lexer.getModeNames()[lexer._mode]);
      }
    } while (token.getType() != Token.EOF);
  }

}
