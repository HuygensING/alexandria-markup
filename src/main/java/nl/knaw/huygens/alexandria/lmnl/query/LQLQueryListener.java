package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.EqualityComparisonExpressionContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.ExprContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.ExtendedIdentifierContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.NamePartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.ParameterizedMarkupSourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.PartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SelectStmtContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SelectVariableContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SimpleMarkupSourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.SourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.TextPartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.WhereClauseContext;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLStatement;

public class LQLQueryListener extends LQLBaseListener {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  private List<LQLStatement> statements = new ArrayList<>();

  public List<LQLStatement> getStatements() {
    return statements;
  }

  public String toText(TextRange textRange) {
    StringBuilder textBuilder = new StringBuilder();
    textRange.textNodes.forEach(textNode -> textBuilder.append(textNode.getContent()));
    return textBuilder.toString();
  }

  @Override
  public void exitSelectStmt(SelectStmtContext ctx) {
    LQLSelectStatement statement = new LQLSelectStatement();

    handleSource(statement, ctx.source());
    handleWhereClause(statement, ctx.whereClause());
    handleSelectVariable(statement, ctx.selectVariable());

    getStatements().add(statement);
    super.exitSelectStmt(ctx);
  }

  private void handleSelectVariable(LQLSelectStatement statement, SelectVariableContext selectVariable) {
    PartContext part = selectVariable.part();
    if (part != null) {
      if (part instanceof TextPartContext) {
        statement.setTextRangeMapper(this::toText);
      } else if (part instanceof NamePartContext) {
        statement.setTextRangeMapper(TextRange::getTag);
      } else {
        unhandled("selectVariable", selectVariable.getText());
      }
    }
  }

  private void handleSource(LQLSelectStatement statement, SourceContext source) {
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
  }

  private void handleWhereClause(LQLSelectStatement statement, WhereClauseContext whereClause) {
    if (whereClause != null) {
      Predicate<? super TextRange> filter = handleExpression(whereClause.expr());
      if (filter != null) {
        statement.setTextRangeFilter(filter);
      } else {
        unhandled("whereClause", whereClause.getText());
      }
    }
  }

  private Predicate<? super TextRange> handleExpression(ExprContext expr) {
    Predicate<? super TextRange> filter = null;
    if (expr instanceof EqualityComparisonExpressionContext) {
      EqualityComparisonExpressionContext ecec = (EqualityComparisonExpressionContext) expr;
      ExtendedIdentifierContext extendedIdentifier = ecec.extendedIdentifier();
      String value = stringValue(ecec.literalValue());
      if (extendedIdentifier.part() instanceof NamePartContext) {
        filter = tr -> tr.getTag().equals(value);
      }
    }
    return filter;
  }

  private void unhandled(String variable, String value) {
    throw new RuntimeException("unhandled: " + variable + " = " + value);
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

}
