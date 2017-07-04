package nl.knaw.huygens.alexandria.lmnl.query;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLBaseListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.AnnotationValuePartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.CombiningExpressionContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.EqualityComparisonExpressionContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.ExprContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.ExtendedIdentifierContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.JoiningExpressionContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.NamePartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.ParameterizedMarkupSourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.PartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.SelectStmtContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.SelectVariableContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.SimpleMarkupSourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.SourceContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.TextContainsExpressionContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.TextPartContext;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser.WhereClauseContext;
import nl.knaw.huygens.alexandria.lmnl.tagql.TAGQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.tagql.TAGQLStatement;

public class TAGQLQueryListener extends TAGQLBaseListener {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  private List<TAGQLStatement> statements = new ArrayList<>();

  public List<TAGQLStatement> getStatements() {
    return statements;
  }

  public String toText(Markup markup) {
    StringBuilder textBuilder = new StringBuilder();
    markup.textNodes.forEach(textNode -> textBuilder.append(textNode.getContent()));
    return textBuilder.toString();
  }

  @Override
  public void exitSelectStmt(SelectStmtContext ctx) {
    TAGQLSelectStatement statement = new TAGQLSelectStatement();

    handleSource(statement, ctx.source());
    handleWhereClause(statement, ctx.whereClause());
    handleSelectVariable(statement, ctx.selectVariable());

    getStatements().add(statement);
    super.exitSelectStmt(ctx);
  }

  private void handleSelectVariable(TAGQLSelectStatement statement, SelectVariableContext selectVariable) {
    PartContext part = selectVariable.part();
    if (part != null) {
      if (part instanceof TextPartContext) {
        statement.setMarkupMapper(this::toText);
      } else if (part instanceof NamePartContext) {
        statement.setMarkupMapper(Markup::getExtendedTag);
      } else if (part instanceof AnnotationValuePartContext) {
        String annotationIdentifier = stringValue(((AnnotationValuePartContext) part).annotationIdentifier());
        statement.setMarkupMapper(toAnnotationTextMapper(annotationIdentifier));
      } else {
        unhandled("selectVariable", selectVariable.getText());
      }
    }
  }

  private Function<? super Markup, ? super Object> toAnnotationTextMapper(String annotationIdentifier) {
    List<String> annotationTags = Arrays.asList(annotationIdentifier.split(":"));
    return (Markup tr) -> {
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

  private void handleSource(TAGQLSelectStatement statement, SourceContext source) {
    if (source != null) {
      if (source instanceof ParameterizedMarkupSourceContext) {
        ParameterizedMarkupSourceContext pmsc = (ParameterizedMarkupSourceContext) source;
        String markupName = stringValue(pmsc.markupName());
        statement.setMarkupFilter(tr -> tr.getTag().equals(markupName));
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

  private void handleWhereClause(TAGQLSelectStatement statement, WhereClauseContext whereClause) {
    if (whereClause != null) {
      Predicate<Markup> filter = handleExpression(whereClause.expr());
      if (filter != null) {
        statement.setMarkupFilter(filter);
      } else {
        unhandled("whereClause", whereClause.getText());
      }
    }
  }

  private Predicate<Markup> handleExpression(ExprContext expr) {
    Predicate<Markup> filter = null;
    if (expr instanceof EqualityComparisonExpressionContext) {
      EqualityComparisonExpressionContext ecec = (EqualityComparisonExpressionContext) expr;
      ExtendedIdentifierContext extendedIdentifier = ecec.extendedIdentifier();
      String value = stringValue(ecec.literalValue());
      if (extendedIdentifier.part() instanceof NamePartContext) {
        filter = tr -> tr.getExtendedTag().equals(value);
      }

    } else if (expr instanceof JoiningExpressionContext) {
      JoiningExpressionContext jec = (JoiningExpressionContext) expr;
      Predicate<Markup> predicate0 = handleExpression(jec.expr(0));
      Predicate<Markup> predicate1 = handleExpression(jec.expr(1));
      filter = predicate0.and(predicate1);

    } else if (expr instanceof CombiningExpressionContext) {
      CombiningExpressionContext context = (CombiningExpressionContext) expr;
      Predicate<Markup> predicate0 = handleExpression(context.expr(0));
      Predicate<Markup> predicate1 = handleExpression(context.expr(1));
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
