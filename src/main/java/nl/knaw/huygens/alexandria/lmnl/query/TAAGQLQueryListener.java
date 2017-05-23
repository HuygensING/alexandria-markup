package nl.knaw.huygens.alexandria.lmnl.query;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAAGQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAAGQLParser.*;
import nl.knaw.huygens.alexandria.lmnl.taagql.TAAGQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.taagql.TAAGQLStatement;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class TAAGQLQueryListener extends TAAGQLBaseListener {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  private List<TAAGQLStatement> statements = new ArrayList<>();

  public List<TAAGQLStatement> getStatements() {
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
    TAAGQLSelectStatement statement = new TAAGQLSelectStatement();

    handleSource(statement, ctx.source());
    handleWhereClause(statement, ctx.whereClause());
    handleSelectVariable(statement, ctx.selectVariable());

    getStatements().add(statement);
    super.exitSelectStmt(ctx);
  }

  private void handleSelectVariable(TAAGQLSelectStatement statement, SelectVariableContext selectVariable) {
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

  private void handleSource(TAAGQLSelectStatement statement, SourceContext source) {
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

  private void handleWhereClause(TAAGQLSelectStatement statement, WhereClauseContext whereClause) {
    if (whereClause != null) {
      Predicate<TextRange> filter = handleExpression(whereClause.expr());
      if (filter != null) {
        statement.setTextRangeFilter(filter);
      } else {
        unhandled("whereClause", whereClause.getText());
      }
    }
  }

  private Predicate<TextRange> handleExpression(ExprContext expr) {
    Predicate<TextRange> filter = null;
    if (expr instanceof EqualityComparisonExpressionContext) {
      EqualityComparisonExpressionContext ecec = (EqualityComparisonExpressionContext) expr;
      ExtendedIdentifierContext extendedIdentifier = ecec.extendedIdentifier();
      String value = stringValue(ecec.literalValue());
      if (extendedIdentifier.part() instanceof NamePartContext) {
        filter = tr -> tr.getTag().equals(value);
      }

    } else if (expr instanceof JoiningExpressionContext) {
      JoiningExpressionContext jec = (JoiningExpressionContext) expr;
      Predicate<TextRange> predicate0 = handleExpression(jec.expr(0));
      Predicate<TextRange> predicate1 = handleExpression(jec.expr(1));
      filter = predicate0.and(predicate1);

    } else if (expr instanceof CombiningExpressionContext) {
      CombiningExpressionContext context = (CombiningExpressionContext) expr;
      Predicate<TextRange> predicate0 = handleExpression(context.expr(0));
      Predicate<TextRange> predicate1 = handleExpression(context.expr(1));
      filter = predicate0.or(predicate1);

    } else if (expr instanceof TextContainsExpressionContext) {
      TextContainsExpressionContext context = (TextContainsExpressionContext) expr;
      String substring = stringValue(context.STRING_LITERAL());
      filter = tr -> toText(tr).contains(substring);

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

  private String stringValue(ParseTree parseTree) {
    return parseTree.getText().replaceAll("'", "");
  }

  private String toAnnotationText(Annotation annotation) {
    return annotation.value().textNodeList.stream()//
        .map(TextNode::getContent)//
        .collect(Collectors.joining());
  }

}
