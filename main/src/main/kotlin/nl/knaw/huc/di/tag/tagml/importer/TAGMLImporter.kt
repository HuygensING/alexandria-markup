package nl.knaw.huc.di.tag.tagml.importer

/*-
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

import nl.knaw.huc.di.tag.model.graph.DotFactory
import nl.knaw.huc.di.tag.tagml.TAGMLBreakingError
import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException

class TAGMLImporter(private val tagStore: TAGStore) {
    @Throws(TAGMLSyntaxError::class)
    fun importTAGML(input: String): TAGDocument {
        val antlrInputStream: CharStream = CharStreams.fromString(input)
        return importTAGML(antlrInputStream)
    }

    @Throws(TAGMLSyntaxError::class)
    fun importTAGML(input: InputStream): TAGDocument =
            try {
                val antlrInputStream = CharStreams.fromStream(input)
                importTAGML(antlrInputStream)
            } catch (e: IOException) {
                e.printStackTrace()
                throw UncheckedIOException(e)
            }

    @Throws(TAGMLSyntaxError::class)
    private fun importTAGML(antlrInputStream: CharStream): TAGDocument {
        val errorListener = ErrorListener()
        val lexer = TAGMLLexer(antlrInputStream).apply {
            addErrorListener(errorListener)
        }
        val tokens = CommonTokenStream(lexer)
        val parser = TAGMLParser(tokens).apply {
            addErrorListener(errorListener)
        }
        val document = usingListener(parser, errorListener)
        //    DocumentWrapper documentWrapper = usingVisitor(parser, errorListener);
        val numberOfSyntaxErrors = parser.numberOfSyntaxErrors
        //    LOG.info("parsed with {} parser syntax errors", numberOfSyntaxErrors);
        var errorMsg = ""
        if (errorListener.hasErrors()) {
            //      logDocumentGraph(document,"");
            errorMsg = """
                |Parsing errors:
                |${errorListener.prefixedErrorMessagesAsString}
                """.trimMargin()
            if (errorListener.hasBreakingError()) {
                errorMsg += "\nparsing aborted!"
            }
            throw TAGMLSyntaxError(errorMsg, errorListener.errors)
        }
        update(document.dto)
        return document
    }

    private fun usingListener(parser: TAGMLParser, errorListener: ErrorListener): TAGDocument {
        parser.buildParseTree = true
        val parseTree: ParseTree = parser.document()
        //    LOG.debug("parsetree: {}", parseTree.toStringTree(parser));
        val listener = TAGMLListener(tagStore, errorListener)
        try {
            ParseTreeWalker.DEFAULT.walk(listener, parseTree)
        } catch (ignored: TAGMLBreakingError) {
        }
        return listener.document
    }

    private fun usingVisitor(parser: TAGMLParser, errorListener: ErrorListener): TAGDocument {
        val documentContext = parser.document()
        val visitor = TAGMLVisitor(tagStore, errorListener).also {
            it.visit(documentContext)
        }
        return visitor.document
    }

    private fun update(tagdto: TAGDTO): Long = tagStore.persist(tagdto)

    private fun logDocumentGraph(document: TAGDocument, input: String) {
        println("\n------------8<------------------------------------------------------------------------------------\n")
        println(DotFactory().toDot(document, input))
        println("\n------------8<------------------------------------------------------------------------------------\n")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TAGMLImporter::class.java)
    }

}
