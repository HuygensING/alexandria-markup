package nl.knaw.huygens.alexandria.texmecs.importer;

import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;

public class TexMECSImporterTest {
  final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testImport1() {
    String texMECS = "<s id='s-1'|<a~1| John <b| loves |a~1> Mary |b>|s>";
    printTokens(texMECS);

    LOG.info("parsing {}", texMECS);
    CharStream antlrInputStream = CharStreams.fromString(texMECS);
    TexMECSLexer lexer = new TexMECSLexer(antlrInputStream);
    // lexer.getAllTokens().forEach(t -> LOG.info("token={}:{}", lexer.getRuleNames()[t.getType() - 1], t));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    // tokens.getTokens().stream().forEach(t -> LOG.info("token={}", t));
    TexMECSParser parser = new TexMECSParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    LOG.info("parseTree={}", parseTree.toStringTree(parser));
    // ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
  }

  protected void printTokens(String input) {
    // This gets all the tokens at once, it does not stop for errors
    // List<? extends Token> allTokens = grammar.getAllTokens();
    // System.out.println(allTokens);
    System.out.println("TexMECS:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
    System.out.println("--------------------------------------------------------------------------------");
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    TexMECSLexer lexer = new TexMECSLexer(inputStream);
    Token token;
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        System.out.println(token + ": " + lexer.getRuleNames()[token.getType() - 1] + " -> " + lexer.getModeNames()[lexer._mode]);
      }
    } while (token.getType() != Token.EOF);
  }

}