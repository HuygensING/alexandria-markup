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
    fun testSPARQLQueryWithDiscontinuity0() {
        val tagml = "[q>[s>'What do you mean,<-s] he gasped [+s>poisonous?<s]<q]"
        runInStoreTransaction { store: TAGStore ->
            val fineDay = TAGMLImporter(store).importTAGML(tagml)
            val h = SPARQLQueryHandler(fineDay)
            val statement = """
                prefix tag: <https://huygensing.github.io/TAG/TAGML/ontology/tagml.ttl#>
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                # Select all markup nodes labeled 'del'
                # Return the content of the text nodes that are associated with these markup nodes
                SELECT ?element ?content
                WHERE {
                  {
                    ?element tag:markup_name                              's' ;
                             tag:elements/rdf:rest*/rdf:first/tag:content ?content 
                    FILTER NOT EXISTS { ?m tag:continued ?element }
                  }
                  UNION 
                  {
                    ?element tag:markup_name                              's' ;
                             tag:continued                                ?c.
                    ?c       tag:elements/rdf:rest*/rdf:first/tag:content ?content .
                  }  
                }
                ORDER BY ?element ?content
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
    fun testSPARQLQuery() {
        val tagml = "[TEI|+P,+L>[page|P>[chapter|L n=1>[p|L>[l|P>[s|L n=1>[q|L>If it's a fine day tomorrow<q]" +
                " said Mrs. Ramsay.<s] [s|L n=2>[q|L>But you'll have to be up with the lark,<q] she added.<s]<l]<p]" +
                "[p|L>[l|P>[s|L>To her son <|[del|P>these<del]|[del|P>[add|P>her<add]<del]|[add|P>these<add]|> words" +
                " conveyed an extraordinary impression of<l][l|P>joy.<s] [s|L n=3>It seemed [del|P>indeed<del]" +
                " as if it were now settled, and the expedition<l][l|P> [del|P>to which he<del] [del|P>with all its<del]" +
                " certain to take place, and [del|P>the<del]<l] [l|P>the wonders to which he had looked forward " +
                "[del|P>th<del] [del|P>br<del] [del|P>brought<-del] within touch<l][l|P>[+del|P>so near — " +
                "only a night & a sail<del] [add|P>with<add] a dazzling, uneasy<l][l|P>disquietude, " +
                "[del|P>a glittering<del] a night[add|P>'s pains<add], & then a day's sail, between.<l]" +
                "<s]<p]<chapter]<page|P]<TEI]"
        runInStoreTransaction { store: TAGStore ->
            val fineDay = TAGMLImporter(store).importTAGML(tagml)
            val h = SPARQLQueryHandler(fineDay)
            val statement = """
                prefix tag: <https://huygensing.github.io/TAG/TAGML/ontology/tagml.ttl#>
                prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
                # Select all markup nodes labeled 'del'
                # Return the content of the text nodes that are associated with these markup nodes
               SELECT ?content
               WHERE {
                  ?element tag:markup_name 'del' ;
                  ?element tag:elements ?text ;
                  ?text tag:content ?content .
                  }
                  ORDER BY ?content
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
