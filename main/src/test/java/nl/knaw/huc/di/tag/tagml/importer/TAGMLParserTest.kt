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

import nl.knaw.huc.di.tag.TAGAssertions
import nl.knaw.huc.di.tag.TAGBaseStoreTest
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser
import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.storage.TAGTextNode
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentAssert
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.ParseTreeWalker
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class TAGMLParserTest : TAGBaseStoreTest() {
    //  private static final TAGMLExporter TAGML_EXPORTER = new TAGMLExporter(store);
    @Test
    fun testAST() {
        val input = "[TEI>[s>It seemed [?del>indeed<?del] as if<s]<TEI]"
        runInStoreTransaction { store: TAGStore -> val document = assertTAGMLParses(input, store) }
    }

    @Test
    fun testTagWithReferenceParses() {
        val input = "[tagml pers->pers01>Some text<tagml]"
        runInStore { store: TAGStore ->
            val pers = store.runInTransaction<AnnotationInfo> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"))
                TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("Some text"))
                assertThat(document.layerNames).containsExactly("")
                val tagmlMarkup = document.markupStream.findFirst().get()
                TAGAssertions.assertThat(tagmlMarkup).hasTag("tagml")
                val persInfo = tagmlMarkup.getAnnotation("pers")
                TAGAssertions.assertThat(persInfo).isReference
                persInfo
            }
            store.runInTransaction {
                val persValue = store.getReferenceValue(pers.nodeId).value
                assertThat(persValue).isEqualTo("pers01")
            }
        }
    }

    @Test // Rd-205
    fun testDefaultLayerIsAlwaysOpen() {
        val input = "[tagml|+A>[x|A>simple<x] [t>text<t] [t>test<t]<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(
                            TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("x"), TAGDocumentAssert.markupSketch("t"), TAGDocumentAssert.markupSketch("t"))
            TAGAssertions.assertThat(document)
                    .hasTextNodesMatching(
                            TAGDocumentAssert.textNodeSketch("simple"),
                            TAGDocumentAssert.textNodeSketch(" "),
                            TAGDocumentAssert.textNodeSketch("text"),
                            TAGDocumentAssert.textNodeSketch(" "),
                            TAGDocumentAssert.textNodeSketch("test"))
            assertThat(document.layerNames).containsExactly("", "A")
        }
    }

    @Test // RD-131
    fun testSimpleTextWithRoot() {
        val input = "[tagml>simple text<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"))
            TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("simple text"))
            assertThat(document.layerNames).containsExactly("")
        }
    }

    @Test // RD-132
    fun testTextWithMultipleLayersOfMarkup() {
        val input = "[text>[some|+L1>[all|+L2>word1<some|L1] word2<all|L2]<text]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(TAGDocumentAssert.markupSketch("text"), TAGDocumentAssert.markupSketch("some"), TAGDocumentAssert.markupSketch("all"))
            TAGAssertions.assertThat(document)
                    .hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("word1"), TAGDocumentAssert.textNodeSketch(" word2"))
            assertThat(document.layerNames).containsExactly("", "L1", "L2")
        }
    }

    @Test // RD-133
    fun testTextWithMultipleLayersAndDiscontinuity() {
        val input = ("[tagml>"
                + "[pre|+L1,+L2>[q|L1>“Man,\"<-q|L1][s|L2> I cried, <s|L2][+q|L1>\"how ignorant art thou in thy pride of wisdom!”<q|L1]<pre|L1,L2]"
                + "― "
                + "[post|+L3>Mary Wollstonecraft Shelley, Frankenstein<post|L3]"
                + "<tagml]")
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(
                            TAGDocumentAssert.markupSketch("tagml"),
                            TAGDocumentAssert.markupSketch("pre"),
                            TAGDocumentAssert.markupSketch("q"),
                            TAGDocumentAssert.markupSketch("s"),
                            TAGDocumentAssert.markupSketch("post"))
            TAGAssertions.assertThat(document)
                    .hasTextNodesMatching(
                            TAGDocumentAssert.textNodeSketch("“Man,\""),
                            TAGDocumentAssert.textNodeSketch(" I cried, "),
                            TAGDocumentAssert.textNodeSketch("\"how ignorant art thou in thy pride of wisdom!”"),
                            TAGDocumentAssert.textNodeSketch("― "),
                            TAGDocumentAssert.textNodeSketch("Mary Wollstonecraft Shelley, Frankenstein"))
            assertThat(document.layerNames)
                    .containsExactly(DEFAULT_LAYER, "L1", "L2", "L3")
            val markups = document.markupStream.filter { m: TAGMarkup -> m.hasTag("q") }.collect(Collectors.toList())
            assertThat(markups).hasSize(2)
            val q = markups[0]
            val qTextNodes = document.getTextNodeStreamForMarkupInLayer(q, "L1").collect(Collectors.toList())
            assertThat(qTextNodes)
                    .extracting("text")
                    .containsExactly("“Man,\"", "\"how ignorant art thou in thy pride of wisdom!”")
        }
    }

    @Disabled
    @Test // RD-134
    fun testTextWithMultipleLayersDiscontinuityAndNonLinearity() {
        val input = ("[tagml>[pre|+L1,+L2>"
                + "[q|L1>“Man,\"<-q|L1][s|L2> I "
                + "<|cried|pleaded|>"
                + ", <s|L2][+q|L1>\"how ignorant art thou in thy pride of wisdom!”<q|L1]"
                + "<pre|L1,L2]― [post|+L3>Mary Wollstonecraft Shelley, Frankenstein<post|L3]<tagml]")
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(
                            TAGDocumentAssert.markupSketch("tagml"),
                            TAGDocumentAssert.markupSketch("pre"),
                            TAGDocumentAssert.markupSketch("q"),
                            TAGDocumentAssert.markupSketch("s"),
                            TAGDocumentAssert.markupSketch("post"))
            TAGAssertions.assertThat(document)
                    .hasTextNodesMatching(
                            TAGDocumentAssert.textNodeSketch("“Man,\""),
                            TAGDocumentAssert.textNodeSketch(" I "),
                            TAGDocumentAssert.textNodeSketch("cried"),
                            TAGDocumentAssert.textNodeSketch("pleaded"),
                            TAGDocumentAssert.textNodeSketch(", "),
                            TAGDocumentAssert.textNodeSketch("\"how ignorant art thou in thy pride of wisdom!”"),
                            TAGDocumentAssert.textNodeSketch("― "),
                            TAGDocumentAssert.textNodeSketch("Mary Wollstonecraft Shelley, Frankenstein"))
            assertThat(document.layerNames).containsExactly("", "L1", "L2", "L3")
            val pleaded = document
                    .textNodeStream
                    .filter { tn: TAGTextNode -> tn.text == "pleaded" }
                    .findFirst()
                    .get()
            val markups = document.getMarkupStreamForTextNode(pleaded).collect(Collectors.toList())
            assertThat(markups)
                    .extracting("tag")
                    .containsExactly(BRANCH, BRANCHES, "s", "pre", "tagml")
            val preMarkups = document.markupStream.filter { m: TAGMarkup -> m.hasTag("pre") }.collect(Collectors.toList())
            assertThat(preMarkups).hasSize(1)
            val l1RootMarkup = preMarkups[0]
            val l1TextNodes = document.getTextNodeStreamForMarkupInLayer(l1RootMarkup, "L1").collect(Collectors.toList())
            assertThat(l1TextNodes)
                    .extracting("text")
                    .containsExactly("“Man,\"", "\"how ignorant art thou in thy pride of wisdom!”")
        }
    }

    @Test
    fun testOptionalMarkup() {
        val input = "[tagml>" + "[?optional>optional<?optional]" + "<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.optionalMarkupSketch("optional"))
            TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("optional"))
        }
    }

    @Test
    fun testDiscontinuity() {
        val input = ("[tagml>"
                + "[discontinuity>yes<-discontinuity]no[+discontinuity>yes<discontinuity]"
                + "<tagml]")
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("discontinuity"))
        }
    }

    @Test
    fun testMilestone() {
        val input = "[tagml>pre " + "[milestone x=4]" + " post<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("milestone"))
        }
    }

    @Test
    fun testMarkupIdentifier() {
        val input = "[tagml>" + "[id~1>identified<id~1]" + "<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("id"))
        }
    }

    @Test
    fun testStringAnnotation() {
        val input = "[text author='somebody'>some text.<text]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("text"))
                TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("some text."))
                TAGAssertions.assertThat(document)
                        .hasMarkupWithTag("text")
                        .withStringAnnotation("author", "somebody")
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test
    fun testStringAnnotation1() {
        val input = "[tagml>" + "[m s=\"string\">text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withStringAnnotation("s", "string")
                document
            }
            assertExportEqualsInput(input.replace("\"", "'"), doc, store)
        }
    }

    @Test
    fun testStringAnnotation2() {
        val input = "[tagml>" + "[m s='string'>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withStringAnnotation("s", "string")
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test
    fun testNumberAnnotation() {
        val input = "[text pi=3.1415926>some text.<text]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("text"))
                TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("some text."))
                TAGAssertions.assertThat(document)
                        .hasMarkupWithTag("text")
                        .withNumberAnnotation("pi", 3.1415926)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test
    fun testNumberAnnotation1() {
        val input = "[text n=1>some text.<text]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("text"))
                TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("some text."))
                TAGAssertions.assertThat(document).hasMarkupWithTag("text").withNumberAnnotation("n", 1.0)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test
    fun testBooleanAnnotation() {
        val input = "[text test=true>some text.<text]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("text"))
                TAGAssertions.assertThat(document).hasTextNodesMatching(TAGDocumentAssert.textNodeSketch("some text."))
                TAGAssertions.assertThat(document)
                        .hasMarkupWithTag("text")
                        .withBooleanAnnotation("test", true)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test
    fun testBooleanAnnotation1() {
        val input = "[tagml>" + "[m b=true>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withBooleanAnnotation("b", true)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test
    fun testBooleanAnnotation2() {
        val input = "[tagml>" + "[m b=FALSE>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withBooleanAnnotation("b", false)
                document
            }
            assertExportEqualsInput(input.replace("FALSE", "false"), doc, store)
        }
    }

    @Test // NLA-468
    fun testStringListAnnotation() {
        val input = "[tagml>" + "[m l=['a','b','c']>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                val expected: List<String> = listOf("a", "b", "c")
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withListAnnotation("l", expected)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test // NLA-468
    fun testNumberListAnnotation() {
        val input = "[tagml>" + "[m l=[3,5,7,11]>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                val expected: List<Float> = listOf(3f, 5f, 7f, 11f)
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withListAnnotation("l", expected)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test // NLA-468
    fun testListAnnotationEntriesShouldAllBeOfTheSameType() {
        val input = "[tagml>" + "[m l=[3,true,'string']>text<m]" + "<tagml]"
        val expectedError = "line 1:13 : All elements of ListAnnotation l should be of the same type."
        runInStoreTransaction { store: TAGStore -> assertTAGMLParsesWithSyntaxError(input, expectedError, store) }
    }

    @Test // RD-206
    fun testListElementSeparatorShouldBeComma() {
        val input = "[tagml>" + "[m l=[3 5 7 11]>text<m]" + "<tagml]"
        val expectedError = "line 1:13 : The elements of ListAnnotation l should be separated by commas."
        runInStoreTransaction { store: TAGStore -> assertTAGMLParsesWithSyntaxError(input, expectedError, store) }
    }

    @Test // NLA-467
    fun testObjectAnnotation0() {
        val input = "[tagml>" + "[m p={valid=false}>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                val expected: MutableMap<String, Any> = mutableMapOf()
                expected["valid"] = false
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withObjectAnnotation("p", expected)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test // NLA-467
    fun testObjectAnnotation1() {
        val input = "[tagml>" + "[m p={x=1 y=2}>text<m]" + "<tagml]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("m"))
                val expected: MutableMap<String, Any> = mutableMapOf()
                expected["x"] = 1.0
                expected["y"] = 2.0
                TAGAssertions.assertThat(document).hasMarkupWithTag("m").withObjectAnnotation("p", expected)
                document
            }
            assertExportEqualsInput(input, doc, store)
        }
    }

    @Test // NLA-467
    fun testNestedObjectAnnotation() {
        val input = """[text meta={
    persons=[
      {:id=huyg0001 name='Constantijn Huygens'}
    ]
  }>[title>De Zee-Straet<title]
  door [author pers->huyg0001>Constantijn Huygens<author]
  .......
<text]"""
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = assertTAGMLParses(input, store)
                TAGAssertions.assertThat(document)
                        .hasMarkupMatching(
                                TAGDocumentAssert.markupSketch("text"), TAGDocumentAssert.markupSketch("title"), TAGDocumentAssert.markupSketch("author"))
                val ch: MutableMap<String, Any> = HashMap()
                ch[":id"] = "huyg001"
                ch["name"] = "Constantijn Huygens"
                val expected: List<Map<String, Any>> = listOf(ch)
                document
            }
            LOG.info("export={}", export(doc, store))
        }
    }

    @Test
    fun testSimpleTextVariation() {
        val input = "[tagml>" + "pre <|to be|not to be|> post" + "<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"))
        }
    }

    @Test
    fun testTextVariationWithMarkup() {
        val input = "[tagml>" + "pre <|[del>to be<del]|[add>not to be<add]|> post" + "<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("del"), TAGDocumentAssert.markupSketch("add"))
        }
    }

    @Test
    fun testNestedTextVariationWithMarkup() {
        val input = ("[tagml>"
                + "pre <|"
                + "[del>to be<del]"
                + "|"
                + "[add>not to <|[del>completely<del]|[add>utterly<add]|> be<add]"
                + "|> post"
                + "<tagml]")
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document)
                    .hasMarkupMatching(
                            TAGDocumentAssert.markupSketch("tagml"),
                            TAGDocumentAssert.markupSketch("del"),
                            TAGDocumentAssert.markupSketch("del"),
                            TAGDocumentAssert.markupSketch("add"),
                            TAGDocumentAssert.markupSketch("add"))
        }
    }

    @Disabled
    @Test
    fun testElementLinking() {
        val input = "[tagml meta={:id=meta01 name='test'}>" + "pre [x ref->meta01>text<x] post" + "<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("tagml"), TAGDocumentAssert.markupSketch("x"))
        }
    }

    @Test
    fun testNestedObjectAnnotation2() {
        val input = "[t meta={:id=meta01 obj={t='test'} n=1}>" + "text" + "<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("t"))
        }
    }

    @Test
    fun testRichTextAnnotation1() {
        val input = "[t note=[>[p>This is a [n>note<n] about this text<p]<]>main text<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("t"))
        }
    }

    @Test
    fun testNamespaceNeedsToBeDefinedBeforeUsage() {
        val input = "[z:t>text<z:t]"
        runInStoreTransaction { store: TAGStore ->
            assertTAGMLParsesWithSyntaxError(
                    input, "line 1:1 : Namespace z has not been defined.", store)
        }
    }

    @Test
    fun testIdentifyingMarkup() {
        val input = "[m :id=m1>" + "pre [x ref->m1>text<x] post" + "<m]"
        runInStoreTransaction { store: TAGStore ->
            val document = assertTAGMLParses(input, store)
            TAGAssertions.assertThat(document).hasMarkupMatching(TAGDocumentAssert.markupSketch("m"), TAGDocumentAssert.markupSketch("x"))
            val m1 = document.markupStream.filter { m: TAGMarkup -> m.hasTag("m") }.findFirst().get()
            TAGAssertions.assertThat(m1).hasMarkupId("m1")
        }
    }

    private fun assertTAGMLParses(input: String, store: TAGStore): TAGDocument {
        printTokens(input)
        val antlrInputStream = CharStreams.fromString(input)
        val lexer = TAGMLLexer(antlrInputStream)
        val errorListener = ErrorListener()
        lexer.addErrorListener(errorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = TAGMLParser(tokens)
        parser.addErrorListener(errorListener)
        parser.buildParseTree = true
        val parseTree: ParseTree = parser.document()
        LOG.info("parsetree: {}", parseTree.toStringTree(parser))
        val numberOfSyntaxErrors = parser.numberOfSyntaxErrors
        LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors)
        assertThat(numberOfSyntaxErrors).isEqualTo(0)
        val listener = TAGMLListener(store, errorListener)
        ParseTreeWalker.DEFAULT.walk(listener, parseTree)
        if (errorListener.hasErrors()) {
            LOG.error("errors: {}", errorListener.errors)
        }
        assertThat(errorListener.hasErrors()).isFalse()
        val document = listener.document
        logDocumentGraph(document, input)

        //    export(document);
        return document
    }

    private fun assertTAGMLParsesWithSyntaxError(
            input: String, expectedSyntaxErrorMessage: String, store: TAGStore) {
        printTokens(input)
        val antlrInputStream = CharStreams.fromString(input)
        val lexer = TAGMLLexer(antlrInputStream)
        val errorListener = ErrorListener()
        lexer.addErrorListener(errorListener)
        val tokens = CommonTokenStream(lexer)
        val parser = TAGMLParser(tokens)
        parser.addErrorListener(errorListener)
        parser.buildParseTree = true
        val parseTree: ParseTree = parser.document()
        LOG.info("parsetree: {}", parseTree.toStringTree(parser))
        val numberOfSyntaxErrors = parser.numberOfSyntaxErrors
        LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors)

        //    TAGMLListener listener = new TAGMLListener(store, errorListener);
        val listener = TAGMLListener(store, errorListener)
        ParseTreeWalker.DEFAULT.walk(listener, parseTree)
        if (errorListener.hasErrors()) {
            LOG.error("errors: {}", errorListener.errors)
        }
        assertThat(errorListener.prefixedErrorMessagesAsString).contains(expectedSyntaxErrorMessage)
    }

    private fun assertExportEqualsInput(input: String, doc: TAGDocument, store: TAGStore) {
        val tagml = export(doc, store)
        assertThat(tagml).isEqualTo(input)
    }

    private fun export(document: TAGDocument, store: TAGStore): String {
        val tagml = store.runInTransaction<String> { TAGMLExporter(store).asTAGML(document) }
        LOG.info("\nTAGML:\n{}\n", tagml)
        return tagml
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TAGMLParserTest::class.java)
    }
}