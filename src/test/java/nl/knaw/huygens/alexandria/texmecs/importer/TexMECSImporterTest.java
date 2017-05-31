package nl.knaw.huygens.alexandria.texmecs.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;

public class TexMECSImporterTest {
  final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testExample1() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b>|s>";
    Document document = testTexMECS(texMECS, 10, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithAttributes() {
    String texMECS = "<s type='test'|<a|John <b|loves|a> Mary|b>|s>";
    Document document = testTexMECS(texMECS, 10, "[s [type}test{type]}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
    TextRange textRange0 = document.value().textRangeList.get(0);
    assertThat(textRange0.getTag()).isEqualTo("s");
    Annotation annotation = textRange0.getAnnotations().get(0);
    assertThat(annotation.getTag()).isEqualTo("type");
    List<TextNode> textNodeList = annotation.value().textNodeList;
    assertThat(textNodeList).hasSize(1);
    assertThat(textNodeList.get(0).getContent()).isEqualTo("test");
  }

  @Test
  public void testExample1WithSuffix() {
    String texMECS = "<s~0|<a|John <b|loves|a> Mary|b>|s~0>";
    Document document = testTexMECS(texMECS, 10, "[s~0}[a}John [b}loves{a] Mary{b]{s~0]");
    assertThat(document.value()).isNotNull();
    TextRange textRange0 = document.value().textRangeList.get(0);
    assertThat(textRange0.getTag()).isEqualTo("s");
    assertThat(textRange0.getSuffix()).isEqualTo("0");
  }

  @Test
  public void testExample1WithSoleTag() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><empty purpose='test'>|s>";
    Document document = testTexMECS(texMECS, 11, "[s}[a}John [b}loves{a] Mary{b][empty [purpose}test{purpose]]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithSuspendResumeTags() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|-b>, or so he says, <+b|very much|b>|s>";
    Document document = testTexMECS(texMECS, 14, "[s}[a}John [b}loves{a] Mary{b], or so he says, [b}very much{b]{s]");
    Limen limen = document.value();
    assertThat(limen).isNotNull();
    List<TextRange> textRangeList = limen.textRangeList;
    assertThat(textRangeList).hasSize(3); // s, a, b
    TextRange textRange = textRangeList.get(2);
    assertThat(textRange.getTag()).isEqualTo("b");
    List<TextNode> textNodes = textRange.textNodes;
    assertThat(textNodes).hasSize(3);
    List<String> textNodeContents = textNodes.stream().map(TextNode::getContent).collect(Collectors.toList());
    assertThat(textNodeContents).containsExactly("loves", " Mary", "very much");
  }

  @Test
  public void testExample1WithComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right! *>|s>";
    Document document = testTexMECS(texMECS, 11, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithNestedComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right<*actually...*>!*>|s>";
    Document document = testTexMECS(texMECS, 11, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithCData() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><#CDATA<some cdata>#CDATA>|s>";
    Document document = testTexMECS(texMECS, 11, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testSelfOverlappingElements() {
    String texMECS = "<e~1|Lorem <e~2|Ipsum |e~1>Dolor...|e~2>";
    Document document = testTexMECS(texMECS, 8, "[e~1}Lorem [e~2}Ipsum {e~1]Dolor...{e~2]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testTagSets() {
    String texMECS = "<|choice||<option|A|option><option|B|option>||choice|>";
    Document document = testTexMECS(texMECS, 9, "[choice}[option}A{option][option}B{option]{choice]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testVirtualElement() {
    String texMECS = "<real|<e=e1|Reality|e>|real><virtual|<^e^e1>|virtual>";
    Document document = testTexMECS(texMECS, 9, "[real}[e=e1}Reality{e=e1]{real][virtual}[e}Reality{e]{virtual]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testMultipleRoots() {
    String texMECS = "<a|A|a><a|A|a><a|A|a><a|A|a><a|A|a>";
    Document document = testTexMECS(texMECS, 16, "[a}A{a][a}A{a][a}A{a][a}A{a][a}A{a]");
    assertThat(document.value()).isNotNull();
  }

  private Document testTexMECS(String texMECS, int expectedChunkCount, String expectedLMNL) {
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
    assertThat(lmnl).isEqualTo(expectedLMNL);

    assertThat(parseTree.getChildCount()).isEqualTo(expectedChunkCount);
    assertThat(parseTree.getChild(0)).isNotNull();
    return doc;
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