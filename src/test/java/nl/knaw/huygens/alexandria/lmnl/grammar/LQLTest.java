package nl.knaw.huygens.alexandria.lmnl.grammar;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LQLTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testCorrectLQLStatement() {
    String statement = "select m.text from markup m where m.name='q' and m.id='a'";
    CharStream stream = new ANTLRInputStream(statement);
    LQLLexer lex = new LQLLexer(stream);
    List<? extends Token> allTokens = lex.getAllTokens();
    for (Token token : allTokens) {
      LOG.info("token: [{}] <<{}>>", lex.getRuleNames()[token.getType() - 1], token.getText());
    }
    lex.reset();

    CommonTokenStream tokens = new CommonTokenStream(lex);
    LQLParser parser = new LQLParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree tree = parser.lql_script();
    LOG.info("tree={}", tree.toStringTree(parser));
    assertThat(tree.getChildCount()).isEqualTo(2);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    assertThat(numberOfSyntaxErrors).isEqualTo(0); // no syntax errors
    LOG.info("numberOfSyntaxErrors={}", numberOfSyntaxErrors);

    for (int i = 0; i < tree.getChildCount(); i++) {
      LOG.info("root.child={}", tree.getChild(i).getText());
    }
    assertThat(allTokens).hasSize(1);
  }

}
