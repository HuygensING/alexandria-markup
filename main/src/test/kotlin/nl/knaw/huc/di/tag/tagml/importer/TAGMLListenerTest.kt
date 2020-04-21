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

import nl.knaw.huc.di.tag.TAGAssertions.assertThat
import nl.knaw.huc.di.tag.TAGBaseStoreTest
import nl.knaw.huc.di.tag.tagml.TAGMLBreakingError
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.assertj.core.util.Files
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.nio.charset.Charset
import java.util.stream.Collectors

class TAGMLListenerTest : TAGBaseStoreTest() {

    @Test
    fun testSnarkParses() {
        val input = Files.contentOf(File("data/tagml/snark81.tagml"), Charset.defaultCharset())
        runInStoreTransaction { store: TAGStore -> val document = assertTAGMLParses(input, store) }
    }

    @Test
    fun testSchemaLocation() {
        val input = """
            [!schema http://tag.com/schemas/test-schema.yaml]
            [tagml>[a>a<a] [b>b<b]<tagml]
            """.trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            val url: URL
            try {
                url = URL("http://tag.com/schemas/test-schema.yaml")
                assertThat(document).hasSchemaLocation(url)
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            }
        }
    }

    @Test
    fun testNonOverlappingMarkupWithoutLayerInfo() {
        val input = addTAGMLHeader("[tagml>" + "[a>a<a] [b>b<b]" + "<tagml]")
        runInStoreTransaction { store: TAGStore -> val document = assertTAGMLParses(input, store) }
    }

    @Test
    fun testUnresumedMarkup() {
        val input = addTAGMLHeader("[tagml>[q>That<-q], she said, [q>is amazing!<q]<tagml]")
        val expectedSyntaxErrorMessage = "line 2:15 : Some suspended markup was not resumed: <-q]"
        assertTAGMLParsesWithSyntaxError(input, expectedSyntaxErrorMessage)
    }

    @Test
    fun testOverlappingMarkupWithoutLayerInfo() {
        val input = addTAGMLHeader("[tagml>" + "[a>a [b>b<a]<b]" + "<tagml]")
        val expectedSyntaxErrorMessage = """
            line 2:1 : Missing close tag(s) for: [tagml>
            line 2:8 : Missing close tag(s) for: [a>
            line 2:17 : Close tag <a] found, expected <b]. Use separate layers to allow for overlap.
            line 2:23 : Close tag <tagml] found, expected <a]. Use separate layers to allow for overlap.
            """.trimIndent()
        assertTAGMLParsesWithSyntaxError(input, expectedSyntaxErrorMessage)
    }

    @Test
    fun testNonOverlappingMarkupWithLayerInfo() {
        val input = addTAGMLHeader("[tagml|+a>" + "[a|a>a<a|a] [b|a>b<b|a]" + "<tagml|a]")
        runInStoreTransaction { store: TAGStore -> val document = assertTAGMLParses(input, store) }
    }

    @Test
    fun testOverlappingMarkupWithLayerInfo() {
        val input = addTAGMLHeader("[tagml|+a,+b>" + "[a|a>a [b|b>b<a|a]<b|b]" + "<tagml|a,b]")
        runInStoreTransaction { store: TAGStore -> val document = assertTAGMLParses(input, store) }
    }

    @Test
    fun testBranchError() {
        val input = addTAGMLHeader(
                "[tagml>[layerdef|+sem,+gen>"
                        + "[l|sem>a <|[add|gen>added<add]|[del|gen>del<del]|> line<l]"
                        + "<layerdef]<tagml]")
        runInStoreTransaction { store: TAGStore -> val document = assertTAGMLParses(input, store) }
    }

    @Test
    fun testLayerShouldBeHierarchical() {
        val input = addTAGMLHeader(
                "[tagml|+a,+b>"
                        + "[page|b>"
                        + "[book|a>book title"
                        + "[chapter|a>chapter title"
                        + "[para|a>paragraph text"
                        + "<page|b]"
                        + "<chapter|a]<book|a]"
                        + "[! para should close before chapter !]<para|a]"
                        + "<tagml|a,b]")
        val expectedSyntaxErrorMessage = """
            line 2:1 : Missing close tag(s) for: [tagml|a,b>
            line 2:22 : Missing close tag(s) for: [book|a>
            line 2:40 : Missing close tag(s) for: [chapter|a>
            line 2:94 : Close tag <chapter|a] found, expected <para|a].
            line 2:105 : Close tag <book|a] found, expected <para|a].
            line 2:159 : Close tag <tagml|a,b] found, expected <chapter|a].
            """.trimIndent()
        assertTAGMLParsesWithSyntaxError(input, expectedSyntaxErrorMessage)
    }

    @Test
    fun testNonlinearText() {
        val input = addTAGMLHeader("[o>Ice cream is <|tasty|cold|sweet|>!<o]")
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            logDocumentGraph(document, input)
            val textGraph = document.dto.textGraph
            val textNodes = document.textNodeStream.collect(Collectors.toList())
            assertThat(textNodes).hasSize(5)
            val textNode1 = textNodes[0]
            assertThat(textNode1).hasText("Ice cream is ")

            //      TAGTextNode textNode2 = textNodes.get(1);
            //      assertThat(textNode2).isDivergence();
            //      Long divergenceNode = textNode2.getResourceId();
            //      assertThat(incomingTextEdges(textGraph, divergenceNode)).hasSize(1);
            //      assertThat(outgoingTextEdges(textGraph, divergenceNode)).hasSize(3);
            val textNode3 = textNodes[1]
            assertThat(textNode3).hasText("tasty")
            val textNode4 = textNodes[2]
            assertThat(textNode4).hasText("cold")
            val textNode5 = textNodes[3]
            assertThat(textNode5).hasText("sweet")

            //      TAGTextNode textNode6 = textNodes.get(5);
            //      assertThat(textNode6).isConvergence();
            //      Long convergenceNode = textNode6.getResourceId();
            //      assertThat(incomingTextEdges(textGraph, convergenceNode)).hasSize(3);
            //      assertThat(outgoingTextEdges(textGraph, convergenceNode)).hasSize(1);
            val textNode7 = textNodes[4]
            assertThat(textNode7).hasText("!")
        }
    }

    // private methods
    private fun assertTAGMLParses(body: String, store: TAGStore): TAGDocument {
        val errorListener = ErrorListener()
        val parser = setupParser(body, errorListener)
        val parseTree: ParseTree = parser.document()
        LOG.info("parsetree: {}", parseTree.toStringTree(parser))
        val numberOfSyntaxErrors = parser.numberOfSyntaxErrors
        LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors)
        assertThat(numberOfSyntaxErrors).isEqualTo(0)
        val listener = walkParseTree(errorListener, parseTree, store)
        assertThat(errorListener.hasErrors()).isFalse()
        val document = listener.document
        logDocumentGraph(document, body)
        val markupRanges = document.markupRangeMap
        LOG.info("markup={}", markupRanges)
        //    String lmnl = new LMNLExporter(store).toLMNL(document);
        //    LOG.info("\nLMNL:\n{}\n", lmnl);
        return document
    }

    private fun assertTAGMLParsesWithSyntaxError(body: String, expectedSyntaxErrorMessage: String) {
        runInStoreTransaction { store: TAGStore ->
            val errorListener = ErrorListener()
            val parser = setupParser(body, errorListener)
            val parseTree: ParseTree = parser.document()
            LOG.info("parsetree: {}", parseTree.toStringTree(parser))
            val numberOfSyntaxErrors = parser.numberOfSyntaxErrors
            LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors)
            try {
                val listener = walkParseTree(errorListener, parseTree, store)
                val document = listener.document
                logDocumentGraph(document, body)
                //              fail("expected TAGMLBreakingError");
            } catch (e: TAGMLBreakingError) {
            }
            assertThat(errorListener.hasErrors()).isTrue()
            val errors = errorListener.prefixedErrorMessagesAsString
            assertThat(errors).isEqualTo(expectedSyntaxErrorMessage)
        }
    }

    private fun setupParser(input: String, errorListener: ErrorListener): TAGMLParser {
        printTokens(input)
        val antlrInputStream = CharStreams.fromString(input)
        val lexer = TAGMLLexer(antlrInputStream)
        lexer.addErrorListener(errorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = TAGMLParser(tokens)
        parser.addErrorListener(errorListener)
        parser.buildParseTree = true
        return parser
    }

    private fun walkParseTree(
            errorListener: ErrorListener, parseTree: ParseTree, store: TAGStore): TAGMLListener {
        val listener = TAGMLListener(store, errorListener)
        ParseTreeWalker.DEFAULT.walk(listener, parseTree)
        if (errorListener.hasErrors()) {
            LOG.error("errors: {}", errorListener.prefixedErrorMessagesAsString)
        }
        return listener
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TAGMLListenerTest::class.java)
    }
}
