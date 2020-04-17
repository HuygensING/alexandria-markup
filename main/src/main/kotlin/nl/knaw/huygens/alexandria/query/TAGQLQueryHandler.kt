package nl.knaw.huygens.alexandria.query

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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
import nl.knaw.huc.di.tag.tagql.TAGQLStatement
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLLexer
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLParser
import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.storage.TAGDocument
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker

class TAGQLQueryHandler(private val document: TAGDocument) {

  fun execute(statement: String): TAGQLResult {
    val stream: CharStream? = CharStreams.fromString(statement)
    val errorListener = ErrorListener()
    val lexer = TAGQLLexer(stream)
    lexer.addErrorListener(errorListener)
    val tokens = CommonTokenStream(lexer)
    val tagqlParser = TAGQLParser(tokens)
    tagqlParser.addErrorListener(errorListener)
    val parseTree: ParseTree? = tagqlParser.query()
    val parseTreeWalker = ParseTreeWalker()
    val listener = TAGQLQueryListener(document)
    parseTreeWalker.walk(listener, parseTree)
    val statements = listener.statements
    val result = TAGQLResult(statement)
    statements
        .map { this.execute(it) }
        .forEach { subResult: TAGQLResult -> result.addResult(subResult) }
    result.errors.addAll(errorListener.errorMessages)
    return result
  }

  private fun execute(statement: TAGQLStatement): TAGQLResult =
      statement
          .limenProcessor
          .apply(document)

}
