package nl.knaw.huygens.alexandria.lmnl.grammar;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TAGQLTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testCorrectTAGQLStatement() {
    String statement = "select m.text from markup m where m.name='q' and m.id='a'";
    CharStream stream = CharStreams.fromString(statement);
    TAGQLLexer lex = new TAGQLLexer(stream);
    List<? extends Token> allTokens = lex.getAllTokens();
    for (Token token : allTokens) {
      LOG.info("token: [{}] <<{}>>", lex.getRuleNames()[token.getType() - 1], token.getText());
    }
    lex.reset();

    CommonTokenStream tokens = new CommonTokenStream(lex);
    TAGQLParser parser = new TAGQLParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree tree = parser.query();
    LOG.info("tree={}", tree.toStringTree(parser));
    assertThat(tree.getChildCount()).isEqualTo(2);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    assertThat(numberOfSyntaxErrors).isEqualTo(0); // no syntax errors
    LOG.info("numberOfSyntaxErrors={}", numberOfSyntaxErrors);

    for (int i = 0; i < tree.getChildCount(); i++) {
      LOG.info("root.child={}", tree.getChild(i).getText());
    }
    assertThat(allTokens).hasSize(19);

    // MyListener listener = new MyListener();
    // parser.TAGQL_script().enterRule(listener);
    //
    // MyVisitor visitor = new MyVisitor();
    // Object result = visitor.visit(tree);
    // LOG.info("result={}",result);

  }

}
