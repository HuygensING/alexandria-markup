package nl.knaw.huygens.alexandria.texmecs.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;

public class TexMECSImporterTest {
  final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testExample1() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(10); // 9 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithAttributes() {
    String texMECS = "<s type='test'|<a|John <b|loves|a> Mary|b>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(10); // 9 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithSuffix() {
    String texMECS = "<s~0|<a|John <b|loves|a> Mary|b>|s~0>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(10); // 9 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithSoleTag() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><empty purpose='test'>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(11); // 10 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithSuspendResumeTags() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|-b>, or so he says, <+b|very much|b>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(14); // 13 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right! *>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(11); // 10 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithNestedComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right<*actually...*>!*>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(11); // 10 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  @Test
  public void testExample1WithCData() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><#CDATA<some cdata>#CDATA>|s>";
    ParseTree parseTree = testTexMECS(texMECS);
    assertThat(parseTree.getChildCount()).isEqualTo(11); // 10 chunks + EOF
    assertThat(parseTree.getChild(0)).isNotNull();
  }

  private ParseTree testTexMECS(String texMECS) {
    printTokens(texMECS);

    LOG.info("parsing {}", texMECS);
    CharStream antlrInputStream = CharStreams.fromString(texMECS);
    TexMECSLexer lexer = new TexMECSLexer(antlrInputStream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TexMECSParser parser = new TexMECSParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    TexMECSListener listener = new TexMECSListener();
    parseTreeWalker.walk(listener, parseTree);
    LOG.info("parseTree={}", parseTree.toStringTree(parser));
    assertThat(parseTree).isNotNull();
    assertThat(parser.getNumberOfSyntaxErrors())//
        .withFailMessage("%d Unexpected syntax error(s)", parser.getNumberOfSyntaxErrors())//
        .isEqualTo(0);
    Document doc = listener.getDocument();
    assertThat(doc.value()).isNotNull();
    LMNLExporter ex = new LMNLExporter();
    String lmnl = ex.toLMNL(doc);
    LOG.info("lmnl={}", lmnl);
    return parseTree;
  }

  protected void printTokens(String input) {
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
        System.out.println(token + "\t: " + lexer.getRuleNames()[token.getType() - 1] + "\t -> " + lexer.getModeNames()[lexer._mode]);
      }
    } while (token.getType() != Token.EOF);
  }

}