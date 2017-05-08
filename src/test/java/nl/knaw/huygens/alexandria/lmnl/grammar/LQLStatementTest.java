package nl.knaw.huygens.alexandria.lmnl.grammar;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class LQLStatementTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  private String statement;
  private Boolean ok;

  public LQLStatementTest(String statement, Boolean ok) {
    this.statement = statement;
    this.ok = ok;
  }

  @Parameters
  public static Collection<Object[]> generateData() throws IOException {
    List<Object[]> data = new ArrayList<>();

    addLines(data, "correct_statements.lql", true);
    addLines(data, "incorrect_statements.lql", false);

    return data;
  }

  private static void addLines(List<Object[]> data, String name, boolean b) throws IOException {
    InputStream inputStream = LQLStatementTest.class.getResourceAsStream(name);
    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
    String line;
    while ((line = reader.readLine()) != null) {
      data.add(new Object[] { line, b });
    }
  }

  @Test
  public void testLQLStatementHasExpectedValidity() {
    LOG.info("LQL={}", this.statement);
    CharStream stream = CharStreams.fromString(this.statement);
    LQLLexer lex = new LQLLexer(stream);
    // List<? extends Token> allTokens = lex.getAllTokens();
    // for (Token token : allTokens) {
    // LOG.info("token: [{}] <<{}>>", lex.getRuleNames()[token.getType() - 1], token.getText());
    // }
    // lex.reset();

    CommonTokenStream tokens = new CommonTokenStream(lex);
    LQLParser parser = new LQLParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree tree = parser.lqlScript();
    LOG.info("tree={}", tree.toStringTree(parser));
    // assertThat(tree.getChildCount()).isEqualTo(2);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("numberOfSyntaxErrors={}", numberOfSyntaxErrors);
    boolean isOk = numberOfSyntaxErrors == 0;
    assertThat(isOk).isEqualTo(this.ok);
  }
}
