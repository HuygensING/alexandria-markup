package nl.knaw.huygens.alexandria.query;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagql.TAGQLSelectStatement;
import nl.knaw.huc.di.tag.tagql.TAGQLStatement;
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLBaseListener;
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLParser;
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLParser.*;
import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

class TAGQLQueryListener extends TAGQLBaseListener {
  private Logger LOG = LoggerFactory.getLogger(getClass());

  private final List<TAGQLStatement> statements = new ArrayList<>();
  private TAGDocument document;

  TAGQLQueryListener(TAGDocument document) {
    this.document = document;
  }

  public List<TAGQLStatement> getStatements() {
    return statements;
  }

  private String toText(TAGMarkup markup) {
    StringBuilder textBuilder = new StringBuilder();
    document.getTextNodeStreamForMarkup(markup)
        .forEach(textNode -> textBuilder.append(textNode.getText()));
    return textBuilder.toString();
  }

  @Override
  public void exitSelectStmt(TAGQLParser.SelectStmtContext ctx) {
    TAGQLSelectStatement statement = new TAGQLSelectStatement();

    handleSource(statement, ctx.source());
    handleWhereClause(statement, ctx.whereClause());
    handleSelectVariable(statement, ctx.selectVariable());

    getStatements().add(statement);
    super.exitSelectStmt(ctx);
  }

  private void handleSelectVariable(TAGQLSelectStatement statement, TAGQLParser.SelectVariableContext selectVariable) {
    TAGQLParser.PartContext part = selectVariable.part();
    if (part != null) {
      if (part instanceof TAGQLParser.TextPartContext) {
        statement.setMarkupMapper(this::toText);
      } else if (part instanceof TAGQLParser.NamePartContext) {
        statement.setMarkupMapper(TAGMarkup::getExtendedTag);
      } else if (part instanceof TAGQLParser.AnnotationValuePartContext) {
        String annotationIdentifier = getAnnotationName(part);
        statement.setMarkupMapper(toAnnotationTextMapper(annotationIdentifier));
      } else {
        unhandled("selectVariable", selectVariable.getText());
      }
    }
  }

  private Function<? super TAGMarkup, ? super Object> toAnnotationTextMapper(String annotationIdentifier) {
    List<String> annotationTags = Arrays.asList(annotationIdentifier.split(":"));
    return (TAGMarkup markup) -> {
      List<String> annotationTexts = new ArrayList<>();
      int depth = 0;
      List<TAGAnnotation> annotationsToFilter = markup.getAnnotationStream().collect(toList());
      while (depth < annotationTags.size() - 1) {
        String filterTag = annotationTags.get(depth);
        annotationsToFilter = annotationsToFilter.stream()//
            .filter(hasTag(filterTag))//
            .flatMap(TAGAnnotation::getAnnotationStream)//
            .collect(toList());
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

  private Predicate<? super TAGAnnotation> hasTag(String filterTag) {
    return a -> filterTag.equals(a.getTag());
  }

  private void handleSource(TAGQLSelectStatement statement, TAGQLParser.SourceContext source) {
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
      Predicate<TAGMarkup> filter = handleExpression(whereClause.expr());
      if (filter != null) {
        statement.setMarkupFilter(filter);
      } else {
        unhandled("whereClause", whereClause.getText());
      }
    }
  }

  private Predicate<TAGMarkup> handleExpression(ExprContext expr) {
    Predicate<TAGMarkup> filter = null;
    if (expr instanceof EqualityComparisonExpressionContext) {
      EqualityComparisonExpressionContext ecec = (EqualityComparisonExpressionContext) expr;
      ExtendedIdentifierContext extendedIdentifier = ecec.extendedIdentifier();
      String value = stringValue(ecec.literalValue());
      if (extendedIdentifier.part() instanceof NamePartContext) {
        filter = markup -> markup.getExtendedTag().equals(value);

      } else if (extendedIdentifier.part() instanceof AnnotationValuePartContext) {
        String annotationIdentifier = getAnnotationName(extendedIdentifier.part());
        filter = markup -> markup.getAnnotationStream()
            .anyMatch(a -> annotationIdentifier.equals(a.getTag()) && value.equals(toAnnotationText(a)));

      } else {
        unhandled(extendedIdentifier.part().getClass().getName() + " extendedIdentifier.part()", extendedIdentifier.part().getText());
      }

    } else if (expr instanceof JoiningExpressionContext) {
      JoiningExpressionContext jec = (JoiningExpressionContext) expr;
      Predicate<TAGMarkup> predicate0 = handleExpression(jec.expr(0));
      Predicate<TAGMarkup> predicate1 = handleExpression(jec.expr(1));
      filter = predicate0.and(predicate1);

    } else if (expr instanceof CombiningExpressionContext) {
      CombiningExpressionContext context = (CombiningExpressionContext) expr;
      Predicate<TAGMarkup> predicate0 = handleExpression(context.expr(0));
      Predicate<TAGMarkup> predicate1 = handleExpression(context.expr(2));
      filter = predicate0.or(predicate1);

    } else if (expr instanceof TextContainsExpressionContext) {
      TextContainsExpressionContext context = (TextContainsExpressionContext) expr;
      String substring = stringValue(context.STRING_LITERAL());
      filter = tr -> toText(tr).contains(substring);

    } else {
      unhandled(expr.getClass().getName() + " expression", expr.getText());
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

  private String toAnnotationText(TAGAnnotation annotation) {
    return annotation.getDocument().getTextNodeStream()//
        .map(TAGTextNode::getText)//
        .collect(joining());
  }

  private String getAnnotationName(PartContext partContext) {
    AnnotationValuePartContext annotationValuePartContext = (AnnotationValuePartContext) partContext;
    AnnotationIdentifierContext annotationIdentifierContext = annotationValuePartContext.annotationIdentifier();
    return stringValue(annotationIdentifierContext);
  }

}
