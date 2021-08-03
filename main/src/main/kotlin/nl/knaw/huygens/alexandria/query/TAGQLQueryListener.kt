package nl.knaw.huygens.alexandria.query

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo
import nl.knaw.huc.di.tag.tagql.TAGQLSelectStatement
import nl.knaw.huc.di.tag.tagql.TAGQLStatement
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLBaseListener
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLParser.*
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGTextNode
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import org.slf4j.LoggerFactory
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors

internal class TAGQLQueryListener(private val document: TAGDocument) : TAGQLBaseListener() {

    private val log = LoggerFactory.getLogger(javaClass)

    val statements: MutableList<TAGQLStatement> = mutableListOf()

    private fun toText(markup: TAGMarkup): String {
        val textBuilder = StringBuilder()
        document
            .getTextNodeStreamForMarkup(markup)
            .forEach { textNode: TAGTextNode -> textBuilder.append(textNode.text) }
        return textBuilder.toString()
    }

    override fun exitSelectStmt(ctx: SelectStmtContext) {
        val statement = TAGQLSelectStatement()
        handleSource(statement, ctx.source())
        handleWhereClause(statement, ctx.whereClause())
        handleSelectVariable(statement, ctx.selectVariable())
        statements.add(statement)
        super.exitSelectStmt(ctx)
    }

    private fun handleSelectVariable(
        statement: TAGQLSelectStatement, selectVariable: SelectVariableContext
    ) {
        val part = selectVariable.part()
        if (part != null) {
            when (part) {
                is TextPartContext -> {
                    statement.setMarkupMapper { markup: TAGMarkup -> toText(markup) }
                }
                is NamePartContext -> {
                    statement.setMarkupMapper { obj: TAGMarkup -> obj.extendedTag }
                }
                is AnnotationValuePartContext -> {
                    val annotationIdentifier = getAnnotationName(part)
                    statement.setMarkupMapper(toAnnotationTextMapper(annotationIdentifier))
                }
                else -> {
                    unhandled("selectVariable", selectVariable.text)
                }
            }
        }
    }

    private fun toAnnotationTextMapper(
        annotationIdentifier: String
    ): Function<in TAGMarkup, in Any> {
        val annotationTags = listOf(*annotationIdentifier.split(":".toRegex()).toTypedArray())
        return Function { markup: TAGMarkup ->
            val annotationTexts: MutableList<String?> = ArrayList()
            var depth = 0
            var annotationsToFilter = markup.annotationStream.collect(Collectors.toList())
            while (depth < annotationTags.size - 1) {
                val filterTag = annotationTags[depth]
                annotationsToFilter = annotationsToFilter.stream()
                    .filter(hasTag(filterTag)) //            .flatMap(TAGAnnotation::getAnnotationStream)
                    .collect(Collectors.toList())
                depth += 1
            }
            val filterTag = annotationTags[depth]
            annotationsToFilter.stream()
                .filter(hasTag(filterTag))
                .map { annotation: AnnotationInfo -> toAnnotationText(annotation) }
                .forEach { e: String? -> annotationTexts.add(e) }
            annotationTexts
        }
    }

    private fun hasTag(filterTag: String): Predicate<AnnotationInfo> =
        Predicate { a: AnnotationInfo -> filterTag == a.name }

    private fun handleSource(statement: TAGQLSelectStatement, source: SourceContext) {
        when (source) {
            is ParameterizedMarkupSourceContext -> {
                val markupName = stringValue(source.markupName())
                statement.setMarkupFilter { tr: TAGMarkup -> tr.tag == markupName }
                if (source.indexValue() != null) {
                    val index = toInteger(source.indexValue())
                    statement.setIndex(index)
                }
            }
            is SimpleMarkupSourceContext -> {
                val smsc = source
                TODO()
            }
        }
    }

    private fun handleWhereClause(statement: TAGQLSelectStatement, whereClause: WhereClauseContext?) {
        if (whereClause != null) {
            val filter = handleExpression(whereClause.expr())
            if (filter != null) {
                statement.setMarkupFilter(filter)
            } else {
                unhandled("whereClause", whereClause.text)
            }
        }
    }

    private fun handleExpression(expr: ExprContext): Predicate<TAGMarkup> {
        var filter: Predicate<TAGMarkup>? = null
        when (expr) {
            is EqualityComparisonExpressionContext -> {
                val extendedIdentifier = expr.extendedIdentifier()
                val value = stringValue(expr.literalValue())
                if (extendedIdentifier.part() is NamePartContext) {
                    filter = Predicate { markup: TAGMarkup -> markup.extendedTag == value }
                } else if (extendedIdentifier.part() is AnnotationValuePartContext) {
                    val annotationIdentifier = getAnnotationName(extendedIdentifier.part())
                    filter = Predicate { markup: TAGMarkup ->
                        markup
                            .annotationStream
                            .anyMatch { a: AnnotationInfo ->
                                annotationIdentifier == a.name && value == toAnnotationText(
                                    a
                                )
                            }
                    }
                } else {
                    unhandled(
                        extendedIdentifier.part().javaClass.name + " extendedIdentifier.part()",
                        extendedIdentifier.part().text
                    )
                }
            }
            is JoiningExpressionContext -> {
                val predicate0 = handleExpression(expr.expr(0))
                val predicate1 = handleExpression(expr.expr(1))
                filter = predicate0.and(predicate1)
            }
            is CombiningExpressionContext -> {
                val predicate0 = handleExpression(expr.expr(0))
                val predicate1 = handleExpression(expr.expr(2))
                filter = predicate0.or(predicate1)
            }
            is TextContainsExpressionContext -> {
                val substring = stringValue(expr.STRING_LITERAL())
                filter = Predicate { tr: TAGMarkup -> substring in toText(tr) }
            }
            else -> {
                unhandled(expr.javaClass.name + " expression", expr.text)
            }
        }
        return filter!!
    }

    private fun unhandled(variable: String, value: String): Unit =
        throw RuntimeException("unhandled: $variable = $value")

    private fun toInteger(ctx: ParserRuleContext): Int =
        ctx.text.toInt()

    private fun stringLiteral(ctx: ParserRuleContext): String =
        ctx.text

    private fun stringValue(parseTree: ParseTree): String =
        parseTree.text.replace("'".toRegex(), "")

    private fun toAnnotationText(annotation: AnnotationInfo): String {
        TODO()
        //    return annotation.getDocument().getTextNodeStream()
        //        .map(TAGTextNode::getText)
        //        .collect(joining());
    }

    private fun getAnnotationName(partContext: PartContext): String {
        val annotationValuePartContext = partContext as AnnotationValuePartContext
        val annotationIdentifierContext = annotationValuePartContext.annotationIdentifier()
        return stringValue(annotationIdentifierContext)
    }

}
