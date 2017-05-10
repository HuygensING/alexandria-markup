package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser.AnnotationValuePartContext;
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
    textRange.textNodes.forEach(

        textNode -> textBuilder.append(textNode.getContent()));
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
      } else if (part instanceof AnnotationValuePartContext) {
        String annotationIdentifier = stringValue(((AnnotationValuePartContext) part).annotationIdentifier());
        statement.setTextRangeMapper(toAnnotationTextMapper(annotationIdentifier));
      } else {
        unhandled("selectVariable", selectVariable.getText());
      }
    }
  }

  private Function<? super TextRange, ? super Object> toAnnotationTextMapper(String annotationIdentifier) {
    List<String> annotationTags = Arrays.asList(annotationIdentifier.split(":"));
    return (TextRange tr) -> {
      List<String> annotationTexts = new ArrayList<>();
      int depth = 0;
      List<Annotation> annotationsToFilter = tr.getAnnotations();
      while (depth < annotationTags.size() - 1) {
        String filterTag = annotationTags.get(depth);
        List<Annotation> newList = annotationsToFilter.stream()//
            .filter(hasTag(filterTag))//
            .flatMap(a -> a.annotations().stream())//
            .collect(Collectors.toList());
        annotationsToFilter = newList;
        depth += 1;
      }
      String filterTag = annotationTags.get(depth);
      annotationsToFilter.stream()//
          .filter(hasTag(filterTag))//
          .map(this::toAnnotationText)///
          .forEach(annotationTexts::add);
      return annotationTexts;
    };
  }

  private Predicate<? super Annotation> hasTag(String filterTag) {
    return a -> filterTag.equals(a.getTag());
  }

  private void handleSource(LQLSelectStatement statement, SourceContext source) {
    if (source != null) {
      if (source instanceof ParameterizedMarkupSourceContext) {
        ParameterizedMarkupSourceContext pmsc = (ParameterizedMarkupSourceContext) source;
        String textRangeName = stringValue(pmsc.markupName());
        statement.setTextRangeFilter(tr -> tr.getTag().equals(textRangeName));
        if (pmsc.indexValue() != null) {
          int index = toInteger(pmsc.indexValue());
          statement.setIndex(index);
        }

      } else if (source instanceof SimpleMarkupSourceContext) {
        SimpleMarkupSourceContext smsc = (SimpleMarkupSourceContext) source;
        // TODO
      }
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

  private String toAnnotationText(Annotation annotation) {
    return annotation.value().textNodeList.stream()//
        .map(TextNode::getContent)//
        .collect(Collectors.joining());
  }

}
