package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.ArrayList;
import java.util.List;

import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.EqualityComparisonExpressionContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.ExprContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.ExtendedIdentifierContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.ParameterizedMarkupSourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.PartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SelectStmtContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SelectVariableContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SimpleMarkupSourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.WhereClauseContext;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLStatement;

public class LQLQueryListener extends LQLBaseListener {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  private List<LQLStatement> statements = new ArrayList<>();

  @Override
  public void exitSelectStmt(SelectStmtContext ctx) {
    LQLSelectStatement statement = new LQLSelectStatement();

    SourceContext source = ctx.source();
    if (source != null && source instanceof ParameterizedMarkupSourceContext) {
      ParameterizedMarkupSourceContext pmsc = (ParameterizedMarkupSourceContext) source;
      String textRangeName = stringValue(pmsc.markupName());
      statement.setTextRangeFilter(tr -> tr.getTag().equals(textRangeName));

      if (pmsc.indexValue() != null) {
        int index = toInteger(pmsc.indexValue());
        statement.setIndex(index);
      }

    } else if (source != null && source instanceof SimpleMarkupSourceContext) {
      SimpleMarkupSourceContext smsc = (SimpleMarkupSourceContext) source;
    }

    WhereClauseContext whereClause = ctx.whereClause();
    if (whereClause != null) {
      ExprContext expr = whereClause.expr();
      if (expr instanceof EqualityComparisonExpressionContext) {
        EqualityComparisonExpressionContext ecec = (EqualityComparisonExpressionContext) expr;
        ExtendedIdentifierContext extendedIdentifier = ecec.extendedIdentifier();
        String part = stringLiteral(extendedIdentifier.part());
        String value = stringValue(ecec.literalValue());
        if ("name".equals(part)) {
          statement.setTextRangeFilter(tr -> tr.getTag().equals(value));
        }
      }
    }

    SelectVariableContext selectVariable = ctx.selectVariable();
    PartContext part = selectVariable.part();
    if (part != null && "text".equals(part.getText())) {
      statement.setTextRangeMapper(this::toText);
    }

    getStatements().add(statement);
    super.exitSelectStmt(ctx);
  }

  private int toInteger(ParserRuleContext ctx) {
    return Integer.valueOf(ctx.getText());
  }

  private String stringLiteral(ParserRuleContext ctx) {
    return ctx.getText();
  }

  private String stringValue(ParserRuleContext ctx) {
    return ctx.getText().replaceAll("'", "");
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
