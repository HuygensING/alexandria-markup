package nl.knaw.huc.di.tag.sparql

/*-
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

import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huc.di.tag.tagml.importer2.TAG
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.RDF
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.*

class SPARQLQueryHandlerTest : AlexandriaBaseStoreTest() {

    private val LOG = LoggerFactory.getLogger(javaClass)

    @Test
    fun testSPARQLQuerySelect() {
        val tagml = "[x>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<x]"
        runInStoreTransaction { store: TAGStore ->
            val alice = TAGMLImporter(store).importTAGML(tagml)
            val h = SPARQLQueryHandler(alice)
            val statement = """prefix tag: <${TAG.getURI()}>
                | prefix rdf: <${RDF.getURI()}>
                | select ?markup (count(?markup) as ?count)
                |   where { [] tag:markup_name ?markup . } 
                |   group by ?markup 
                |   order by ?markup
                """.trimMargin()
            LOG.info(statement)
            val result = h.execute(statement)
            LOG.info("result={}", result)
            assertQuerySucceeded(result)

            val expected: MutableList<String> = ArrayList()
            expected.add(normalizeLineEndings("""
                ------------------
                | markup | count |
                ==================
                | "q"    | 2     |
                | "x"    | 1     |
                ------------------
                
                """.trimIndent()))
            assertThat(result.getValues()).containsExactlyElementsOf(expected)
        }
    }

    @Test
    fun testSPARQLQueryAsk() {
        val tagml = "[x>some text<x]"
        runInStoreTransaction { store: TAGStore ->
            val alice = TAGMLImporter(store).importTAGML(tagml)
            val h = SPARQLQueryHandler(alice)
            val statement = "prefix tag: <" + TAG.getURI() + "> " +
                    "prefix rdf: <" + RDF.getURI() + "> " +
                    "ask {" +
                    "  ?m tag:markup_name 'x' ." +  // markup has name 'x'
                    "  ?m tag:elements ?list ." +  // markup has elements ?list = rdf:list
                    "  ?list rdf:rest*/rdf:first ?t ." +  // the list has a textnode ?t
                    "  ?t tag:content 'some text' . " +  // textnode has content 'some text'
                    "}"
            val result = h.execute(statement)
            LOG.info("result={}", result)
            assertQuerySucceeded(result)

            val expected: MutableList<Boolean> = ArrayList()
            expected.add(true)
            assertThat(result.getValues()).containsExactlyElementsOf(expected)
        }
    }

    @Test
    fun testSPARQLQueryDescribe() {
        val tagml = "[l>[w>Just<w] [w>some<w] [w>words<w]<l]"
        runInStoreTransaction { store: TAGStore ->
            val alice = TAGMLImporter(store).importTAGML(tagml)
            val h = SPARQLQueryHandler(alice)
            val statement = "prefix tag: <" + TAG.getURI() + "> " +
                    "prefix rdf: <" + RDF.getURI() + "> " +
                    "describe ?x where { ?x tag:markup_name 'w' }"
            val result = h.execute(statement)
            LOG.info("result={}", result)
            assertQuerySucceeded(result)

            val expected: MutableList<String> = ArrayList()
            expected.add("""
                |@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .
                |@prefix tag:   <https://huygensing.github.io/TAG/TAGML/ontology/tagml.ttl#> .
                |
                |tag:markup9  a           tag:MarkupNode ;
                |        tag:elements     ( tag:text10 ) ;
                |        tag:layer        tag:layer_ ;
                |        tag:markup_name  "w" .
                |
                |tag:markup3  a           tag:MarkupNode ;
                |        tag:elements     ( tag:text4 ) ;
                |        tag:layer        tag:layer_ ;
                |        tag:markup_name  "w" .
                |
                |tag:markup6  a           tag:MarkupNode ;
                |        tag:elements     ( tag:text7 ) ;
                |        tag:layer        tag:layer_ ;
                |        tag:markup_name  "w" .
                """.trimMargin())
        }
    }

    @Test
    fun testSPARQLQueryConstruct() {
        val tagml = "[l>[person>John<person] went to [country>Spain<country], [person>Rachel<person] went to [country>Peru<country]<l]"
        runInStoreTransaction { store: TAGStore ->
            val alice = TAGMLImporter(store).importTAGML(tagml)
            val h = SPARQLQueryHandler(alice)
            val statement = "prefix tag: <" + TAG.getURI() + "> " +
                    "prefix rdf: <" + RDF.getURI() + "> " +
                    "prefix foaf: <" + FOAF.getURI() + "> " +
                    "construct {" +
                    " ?m rdf:type  foaf:Person;" +
                    "    foaf:name ?name . " +
                    "} where {" +
                    "  ?m  tag:markup_name                               'person' ;" +
                    "      tag:elements/rdf:rest*/rdf:first/tag:content  ?name . " +
                    "}"
            val result = h.execute(statement)
            assertQuerySucceeded(result)
            println(statement)
            println(result.getValues())
        }
    }

    private fun normalizeLineEndings(string: String): String =
            string.replace("\\n".toRegex(), System.lineSeparator())

    private fun assertQuerySucceeded(result: SPARQLResult) {
        if (!result.isOk) {
            LOG.error("errors: {}", result.errors)
        }
        assertThat(result).isNotNull
        assertThat(result.isOk).isTrue()
    }
}
