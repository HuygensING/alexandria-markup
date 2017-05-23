package nl.knaw.huygens.alexandria.texmecs.importer;

import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TexMECSImporterTest {
  final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testImport1() {
    String texMECS = "<s|<a| John <b| loves |a> Mary |b>|s>";
    LOG.info("parsing {}", texMECS);
    CharStream antlrInputStream = CharStreams.fromString(texMECS);
    TexMECSLexer lexer = new TexMECSLexer(antlrInputStream);
//    lexer.getAllTokens().forEach(t -> LOG.info("token={}:{}", lexer.getRuleNames()[t.getType() - 1], t));
    CommonTokenStream tokens = new CommonTokenStream(lexer);
//    tokens.getTokens().stream().forEach(t -> LOG.info("token={}", t));
    ParseTree parseTree = new TexMECSParser(tokens).document();
    LOG.info("parseTree={}", parseTree);
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();

  }

}