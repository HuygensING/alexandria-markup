package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.ArrayList;
import java.util.List;

import nl.knaw.huygens.alexandria.lmnl.grammar.LQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.Select_stmtContext;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLStatement;

public class LQLQueryListener extends LQLBaseListener {
  private List<LQLStatement> statements = new ArrayList<>();

  @Override
  public void enterSelect_stmt(Select_stmtContext ctx) {
    LQLSelectStatement statement = new LQLSelectStatement();
    getStatements().add(statement);
  }

  public List<LQLStatement> getStatements() {
    return statements;
  }

}
