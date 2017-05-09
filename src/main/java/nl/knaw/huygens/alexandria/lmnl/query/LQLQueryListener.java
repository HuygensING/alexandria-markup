package nl.knaw.huygens.alexandria.lmnl.query;

import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.*;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LQLQueryListener extends LQLBaseListener {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  private List<LQLStatement> statements = new ArrayList<>();

  @Override
  public void exitSelectStmt(SelectStmtContext ctx) {
    LQLSelectStatement statement = new LQLSelectStatement();

    SourceContext source = ctx.source();
    if (source != null && source instanceof ParameterizedMarkupSourceContext) {
      ParameterizedMarkupSourceContext pmsc = (ParameterizedMarkupSourceContext) source;
      String textRangeName = pmsc.markupName().getText().replaceAll("'", "");
      statement.setTextRangeFilter(tr -> tr.getTag().equals(textRangeName));

      if (pmsc.indexValue() != null) {
        int index = Integer.valueOf(pmsc.indexValue().getText());
        statement.setIndex(index);
      }

    } else if (source != null && source instanceof SimpleMarkupSourceContext) {
      SimpleMarkupSourceContext smsc = (SimpleMarkupSourceContext) source;
    }

    SelectVariableContext selectVariable = ctx.selectVariable();
    PartContext part = selectVariable.part();
    if (part != null && "text".equals(part.getText())) {
      statement.setTextRangeMapper(this::toText);
    }

    getStatements().add(statement);
    super.exitSelectStmt(ctx);
  }

  public List<LQLStatement> getStatements() {
    return statements;
  }

  public String toText(TextRange textRange) {
    StringBuilder textBuilder = new StringBuilder();
    textRange.textNodes.forEach(textNode -> textBuilder.append(textNode.getContent()));
    return textBuilder.toString();
  }

}
