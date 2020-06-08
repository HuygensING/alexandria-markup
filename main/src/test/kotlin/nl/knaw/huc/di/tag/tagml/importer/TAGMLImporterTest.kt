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
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES
import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError
import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter
import nl.knaw.huc.di.tag.tagml.rdf.DotFactory
import nl.knaw.huc.di.tag.tagml.rdf.RDFFactory
import nl.knaw.huygens.alexandria.ErrorListener.CustomError
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentAssert.*
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO
import nl.knaw.huygens.alexandria.view.TAGViewFactory
import org.apache.commons.io.FileUtils
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.stream.Collectors.toList

class TAGMLImporterTest : TAGBaseStoreTest() {

    @Test
    fun test_minimal_header() {
        val tagML = ("""
            |[!{}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assertThat(document.rawHeader).isEqualTo("[!{}!]")
        }
    }

    @Test
    fun test_namespaces_in_header() {
        val tagML = ("""
            |[!{
            |    ":namespaces": {
            |       "a": "http://example.org/a",
            |       "b": "http://example.org/b"
            |    }
            |}!]
            |[a:tagml>[b:b>body<b:b]<a:tagml]
            |""".trimMargin())
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assertThat(document.namespaces).isEqualTo(mapOf(
                    "a" to "http://example.org/a",
                    "b" to "http://example.org/b"
            ))
        }
    }

    @Test
    fun test_medium_header() {
        val tagML = ("""
            |[!{
            |  ":authors": [ "me", "you", "them" ],
            |  "title": "test",
            |  "version": 0.1,
            |  "tags": {
            |    "tagml": "this is the root markup"
            |  }
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
        }
    }

    @Test
    fun test_faulty_header() {
        val tagML = ("""
            |[!{
            |  you can't just write stuff here!
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        runInStoreTransaction {
            parseWithExpectedErrors(tagML, """
                line 2:2 : syntax error: unexpected token: 'y'
                line 2:3 : syntax error: unexpected token: 'o'
                line 2:4 : syntax error: unexpected token: 'u'
                line 2:6 : syntax error: unexpected token: 'c'
                line 2:7 : syntax error: unexpected token: 'a'
                line 2:8 : syntax error: unexpected token: 'n''
                line 2:10 : syntax error: unexpected token: 't '
                line 2:12 : syntax error: unexpected token: 'j'
                line 2:13 : syntax error: unexpected token: 'u'
                line 2:14 : syntax error: unexpected token: 's'
                line 2:15 : syntax error: unexpected token: 't '
                line 2:17 : syntax error: unexpected token: 'w'
                line 2:18 : syntax error: unexpected token: 'r'
                line 2:19 : syntax error: unexpected token: 'i'
                line 2:20 : syntax error: unexpected token: 'te'
                line 2:23 : syntax error: unexpected token: 's'
                line 2:24 : syntax error: unexpected token: 'tu'
                line 2:26 : syntax error: unexpected token: 'ff'
                line 2:29 : syntax error: unexpected token: 'h'
                line 2:30 : syntax error: unexpected token: 'e'
                line 2:31 : syntax error: unexpected token: 'r'
                line 2:32 : syntax error: unexpected token: 'e'
                line 2:33 : syntax error: unexpected token: '!'
                """.trimIndent())
        }
    }

    @Test
    fun test_large_header() {
        val tagML = ("""
            |[!{
            |  ":ontology": {
            |    "root": "excerpt",
            |    "elements": {
            |      "excerpt": {
            |        "description": "A short extract from a text",
            |        "attributes": [
            |          "type",
            |          "title",
            |          "author",
            |          "year",
            |          "page",
            |          "persons",
            |          "id"
            |        ]
            |      },
            |      "img": {
            |        "description": "Image; the (external) representation of the document containing the text",
            |        "properties": [ "milestone" ],
            |        "attributes": [ "source" ]
            |      },
            |      "chapter": {
            |        "description": "Main division of a text",
            |        "attributes": [ "n" ]
            |      },
            |      "par": {
            |        "description": "A distinct section in a text, indicated by a new line or an indentation",
            |        "attributes": [ "n" ]
            |      },
            |      "s": {
            |        "description": "contains a sentence-like division of a text",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-s.html"
            |      },
            |      "sic": {
            |        "description": "contains text reproduced although apparently incorrect or inaccurate",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-sic.html"
            |      },
            |      "corr": {
            |        "description": "contains the correct form of a passage apparently erroneous in the copy text",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-corr.html"
            |      },
            |      "said": {
            |        "description": "(speech or thought) indicates passages thought or spoken aloud",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-said.html",
            |        "attributes": [ "who" ],
            |        "properties": [ "discontinuous" ]
            |      },
            |      "persName": {
            |        "description": "personal name: contains a proper noun or proper noun phrase referring to a person",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-persName.html",
            |        "attributes": [ "id" ]
            |      },
            |      "emph": {
            |        "description": "(emphasized) marks words or phrases which are stressed or emphasized for linguistic or rhetorical effect",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-emph.html"
            |      }
            |    },
            |    "attributes": {
            |      "type": {
            |        "description": "used to classify the source of the text in the document",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-att.typed.html",
            |        "dataType": "String"
            |      },
            |      "title": {
            |        "description": "used to indicate the title of the text in the document",
            |        "dataType": "String"
            |      },
            |      "source": {
            |        "description": "refers to the source of the (external) representation of the document containing the text",
            |        "dataType": "URI"
            |      },
            |      "author": {
            |        "description": "refers to the name of the author(s) of the text in the document",
            |        "dataType": "Pointer"
            |      },
            |      "year": {
            |        "description": "refers to the year of publication of the text in the document",
            |        "dataType": "Integer"
            |      },
            |      "page": {
            |        "description": "indicates the page(s) of the text in the document",
            |        "dataType": "IntegerList"
            |      },
            |      "persons": {
            |        "description": "(fictional) persons mentioned in the document",
            |        "dataType": "StringList"
            |      },
            |      "id": {
            |        "description": "points to a unique identifier for the element bearing the attribute",
            |        "dataType": "ID"
            |      },
            |      "who": {
            |        "description": "points to the unique identifier of a person in the document",
            |        "dataType": "Pointer"
            |      },
            |      "n": {
            |        "description": "gives a number for an element which is not necessarily unique in the document",
            |        "ref": "https://www.tei-c.org/release/doc/tei-p5-doc/en/html/ref-att.global.html",
            |        "dataType": "Integer"
            |      }
            |    },
            |    "rules": [
            |      "excerpt > chapter+, img+",
            |      "chapter > par+, said+",
            |      "par > s+",
            |      "s > (sic, corr)*",
            |      "said > persName+, emph+, said*",
            |      "said may-not-overlap-with s",
            |      "sic is-non-linear-with corr"
            |    ]
            |  },
            |  ":authors": [ "me", "you", "them" ],
            |  "title": "test",
            |  "version": 0.2
            |}!]
            |[tagml>body<tagml]
            |""".trimMargin())
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test
    fun testReturnedError() {
        val tagML = "$DUMMY_HEADER[a>\n[a1 pi=3.14>AAAAA\n<wrong_closing_tag]\n<a]"
        runInStoreTransaction { store: TAGStore ->
            try {
                val document = parseTAGML(tagML, store)
                logDocumentGraph(document, tagML)
                fail("TAGMLSyntaxError expected!")
            } catch (e: TAGMLSyntaxError) {
                val errors = e.errors
                assertThat(errors).hasSize(4)

                val tagError = errors[2]
                assertThat(tagError).isInstanceOf(CustomError::class.java)

                val customError = tagError as CustomError
                assertThat(customError.message)
                        .isEqualTo("Close tag <wrong_closing_tag] found without corresponding open tag.")
                assertThat(customError.range)
                        .isEqualTo(Range(Position(4, 1), Position(4, 20)))
            }
        }
    }

    @Test
    fun test() {
        val tagML = (DUMMY_HEADER
                + "[root>"
                + "[s><|[del>Dit kwam van een<del]|[del>[add>Gevolg van een<add]<del]|[add>De<add]|>"
                + " te streng doorgedreven rationalisatie van zijne "
                + "<|[del>opvoeding<del]|[del>[add>prinselijke jeugd<add]<del]|[add>prinsenjeugd [?del>bracht<?del] had dit met zich meegebracht<add]|><s]"
                + "<root]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            testRDFConversion(document)
        }
    }

    @Test
    fun test1() {
        val tagML = (DUMMY_HEADER
                + "[a>"
                + "<|[del>We [?del>feel<?del]<del]|[add>How many in England<add]|>"
                + "<a]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            testRDFConversion(document)
        }
    }

    @Test // RD-206
    fun testRD206_1() {
        val tagML = """
            $DUMMY_HEADER
            [root metadata={stages=[{:id=stage1 medium={tool="typewriter" color="black"} desc="xxx"}, {:id=stage2 medium={tool="pencil" color="grey"} desc="xxxx"}, {:id=stage3 medium={tool="pen" color="blue"} desc="xxx"}]}>
            [text> 
            [del|+gen ref->stage3>  [! some text here !] <del]
            <text]
            <root]""".trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test // RD-206
    fun testRD206_2() {
        val tagML = """
            $DUMMY_HEADER
            [root metadata={persons=[{:id=rou001 name='Gustave Roud'}, {:id=doe002 name='Jane Doe'}]}>
            [excerpt source={author->rou001 editor->doe002 work="requiem" ts-id="CRLR_GR_MS1H16d_1r_1"} lang="fr" encoding="UTF-8">
            <excerpt]
            <root]""".trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test // RD-207
    @Throws(IOException::class)
    fun testRD207() {
        val tagML = FileUtils.readFileToString(File("data/tagml/CRLR_GR_MS1H16d_ES.tagml"), "UTF-8")
        val view = FileUtils.readFileToString(File("data/tagml/view-stage2-layer.json"), "UTF-8")
        runInStore { store: TAGStore ->
            val tvf = TAGViewFactory(store)
            val tagView = tvf.fromJsonString(view)
            val document = store.runInTransaction<TAGDocument> { parseTAGML(tagML, store) }
            assert(document != null)

            store.runInTransaction {
                val exporter = TAGMLExporter(store, tagView)
                val tagmlView = exporter.asTAGML(document)
                assertThat(tagmlView).isNotNull()
                LOG.info("view=\n{}", tagmlView)
            }
        }
    }

    @Test
    fun testFrostQuote() {
        val tagML = """
            $DUMMY_HEADER
            [excerpt|+S,+L source="The Housekeeper" author="Robert Frost">
            [s|S>[l|L n=144>He manages to keep the upper hand<l]
            [l|L n=145>On his own farm.<s] [s|S>He's boss.<s] [s|S>But as to hens:<l]
            [l|L n=146>We fence our flowers in and the hens range.<l]<s]
            <excerpt]""".trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test
    fun testCMLHTS18() {
        val tagML = """
            $DUMMY_HEADER
            [tagml>
            [page>
            [p>
            [line>1st. Voice from the Springs<line]
            [line>Thrice three hundred thousand years<line]
            [line>We had been stained with bitter blood<line]
            <p]
            <page]
            [page>
            [p>
            [line>And had ran mute 'mid shrieks of slaugter<line]
            [line>Thro' a city and a multitude<line]
            <p]
            <page]
            <tagml]
            
            """.trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test
    fun testLayerIdentifiersAreOptionalInEndTags() {
        val tagML = """
            $DUMMY_HEADER
            [tagml>
            [text|+A,+B>
            [page|A>
            [p|B>
            [line>1st. Voice from the Springs<line]
            [line>Thrice three hundred thousand years<line]
            [line>We had been stained with bitter blood<line]
            <page]
            [page|A>
            [line>And had ran mute 'mid shrieks of slaugter\[sic]<line]
            [line>Thro' a city & a multitude<line]
            <p]
            <page]
            <text]
            <tagml]
            """.trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test
    fun testLayerIdentifiersAreRequiredInEndTagsWhenThereIsAmbiguity() {
        val tagML = """
            $DUMMY_HEADER
            [tagml>
            [text|+A,+B,+C>
            [page|A>
            [p|B>
            [p|C>
            [line>1st. Voice from the Springs<line]
            [line>Thrice three hundred thousand years<line]
            [line>We had been stained with bitter blood<line]
            <p]
            <page]
            [page|A>
            [p|C>
            [line>And had ran mute 'mid shrieks of slaugter<line]
            [line>Thro' a city and a multitude<line]
            <p]
            <p]
            <page]
            <text]
            <tagml]
            """.trimIndent()
        val expectedErrors = """
            line 11:1 : There are multiple start-tags that can correspond with end-tag <p]; add layer information to the end-tag to solve this ambiguity.
            parsing aborted!
            """.trimIndent()
        runInStoreTransaction { parseWithExpectedErrors(tagML, expectedErrors) }
    }

    @Test
    fun testMissingOpenTagLeadsToError() {
        val tagML = "$DUMMY_HEADER[tagml>Some text<t]<tagml]"
        val expectedErrors = "line 2:17 : Close tag <t] found without corresponding open tag."
        runInStoreTransaction { parseWithExpectedErrors(tagML, expectedErrors) }
    }

    @Test
    fun testLayerIdentifiersAreOptionalInEndTagWhenNotAmbiguous() {
        val tagML = "$DUMMY_HEADER[tagml|+A>Some text<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test
    fun testNoLayerInfoOnEndTagWithMultipleStartTagsInDifferentLayers() {
        val tagML = "$DUMMY_HEADER[tagml|+A,+B>[p|A>[p|B>Some text<p]<p]<tagml]"
        val expectedErrors = """
            line 2:33 : There are multiple start-tags that can correspond with end-tag <p]; add layer information to the end-tag to solve this ambiguity.
            parsing aborted!
            """.trimIndent()
        runInStoreTransaction { parseWithExpectedErrors(tagML, expectedErrors) }
    }

    @Test
    fun testNoLayerInfoOnEndTagWithMultipleStartTagsInSameLayers() {
        val tagML = "$DUMMY_HEADER[tagml|+A>[p|A>[p|A>Some text<p]<p]<tagml]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
        }
    }

    @Test
    fun testSimpleTAGML() {
        val tagML = "$DUMMY_HEADER[line>The rain in Spain falls mainly on the plain.<line]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(textNodeSketch("The rain in Spain falls mainly on the plain."))
            assertThat(document).hasMarkupMatching(markupSketch("line"))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(1)

            val textNode = tagTextNodes[0]
            assertThat(textNode).hasText("The rain in Spain falls mainly on the plain.")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(1)
            assertThat(markupForTextNode).extracting("tag").contains("line")
            testRDFConversion(document)
        }
    }

    @Test
    fun testCharacterEscapingInRegularText() {
        val tagML = (DUMMY_HEADER
                + "[tagml>In regular text, \\<, \\[ and \\\\ need to be escaped, |, !, \", and ' don't.<tagml]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document).hasTextNodesMatching(
                    textNodeSketch(
                            "In regular text, <, [ and \\ need to be escaped, |, !, \", and ' don't."))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(1)
        }
    }

    @Test
    fun testCharacterEscapingInTextVariation() {
        val tagML = (DUMMY_HEADER
                + "[t>In text in between textVariation tags, <|\\<, \\[, \\| and \\\\ need to be escaped|!, \" and ' don't|>.<t]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("In text in between textVariation tags, "),  //          textDivergenceSketch(),
                            textNodeSketch("<, [, | and \\ need to be escaped"),
                            textNodeSketch("""!, " and ' don't"""),  //          textConvergenceSketch(),
                            textNodeSketch("."))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(4)
        }
    }

    @Test
    fun testMissingEndTagThrowsTAGMLSyntaxError() {
        val tagML = "$DUMMY_HEADER[line>The rain"
        val expectedErrors = "line 2:1 : Missing close tag(s) for: [line>"
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testMissingOpenTagThrowsTAGMLSyntaxError() {
        val tagML = DUMMY_HEADER + "on the plain.<line]"
        val expectedErrors = """
            line 1:7 : No text allowed here, the root markup must be started first.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testDifferentOpenAndCloseTAGSThrowsTAGMLSyntaxError() {
        val tagML = "$DUMMY_HEADER[line>The Spanish rain.<paragraph]"
        val expectedErrors = """
            line 2:1 : Missing close tag(s) for: [line>
            line 2:24 : Close tag <paragraph] found without corresponding open tag.
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testNamelessTagsThrowsTAGMLSyntaxError() {
        val tagML = "$DUMMY_HEADER[>The Spanish rain.<]"
        val expectedErrors = """
            line 2:1 : syntax error: no viable alternative at input '[>'
            line 2:3 : No text allowed here, the root markup must be started first.
            line 2:20 : syntax error: mismatched input ']' expecting {IMO_Prefix, IMO_Name, IMC_Prefix, IMC_Name}
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testOverlap() {
        val tagML = "$DUMMY_HEADER[x|+la,+lb>[a|la>J'onn J'onzz [b|lb>likes<a|la] Oreos<b|lb]<x|la,lb]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assertThat(document)
                    .hasMarkupWithTag("a")
                    .inLayer("la")
                    .withTextNodesWithText("J'onn J'onzz ", "likes")
            assertThat(document)
                    .hasMarkupWithTag("b")
                    .inLayer("lb")
                    .withTextNodesWithText("likes", " Oreos")
        }
    }

    @Test
    fun testTAGML2() {
        val tagML = (DUMMY_HEADER
                + "[line|+a,+b>[a|a>The rain in [country>Spain<country] [b|b>falls<a|a] mainly on the plain.<b|b]<line|a,b]")
        //    String tagML = "[line|+A,+B,+N>[a|A>[name|N>Trump<name|N] [b|B>likes<a|A]
        // [name|N>Kim<name|N]<b|B]<line|A,B,N]";
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("The rain in "),
                            textNodeSketch("Spain"),
                            textNodeSketch(" "),
                            textNodeSketch("falls"),
                            textNodeSketch(" mainly on the plain."))
            assertThat(document)
                    .hasMarkupMatching(
                            markupSketch("line"),
                            markupSketch("a"),
                            markupSketch("country"),
                            markupSketch("b"))

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(5)

            val textNode = textNodes[1]
            assertThat(textNode).hasText("Spain")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(3)
            assertThat(markupForTextNode).extracting("tag").containsExactly("line", "a", "country")

            val textSegments = document
                    .dto.textGraph
                    .textNodeIdStream
                    .map { textNodeId: Long? -> store.getTextNodeDTO(textNodeId) }
                    .map { obj: TAGTextNodeDTO -> obj.text }
                    .collect(toList())
            assertThat(textSegments)
                    .containsExactly("The rain in ", "Spain", " ", "falls", " mainly on the plain.")
        }
    }

    @Test
    fun testCommentsAreIgnored() {
        val tagML = "$DUMMY_HEADER[! before !][a>Ah![! within !]<a][! after !]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document).hasTextNodesMatching(textNodeSketch("Ah!"))
            assertThat(document).hasMarkupMatching(markupSketch("a"))

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(1)

            val textNode = textNodes[0]
            assertThat(textNode).hasText("Ah!")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(1)
            assertThat(markupForTextNode).extracting("tag").containsExactly("a")
        }
    }

    @Ignore("TODO: refactor to new header")
    @Test
    fun testNamespace() {
        val tagML = "[!ns a http://tag.com/a][a:a>Ah!<a:a]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document).hasTextNodesMatching(textNodeSketch("Ah!"))
            assertThat(document).hasMarkupMatching(markupSketch("a:a"))

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(1)

            val textNode = textNodes[0]
            assertThat(textNode).hasText("Ah!")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(1)
            assertThat(markupForTextNode).extracting("tag").containsExactly("a:a")
        }
    }

    @Ignore("TODO: refactor to new header")
    @Test
    fun testMultipleNamespaces() {
        val tagML = "[!ns a http://tag.com/a]\n[!ns b http://tag.com/b]\n[a:a>[b:b>Ah!<b:b]<a:a]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document).hasTextNodesMatching(textNodeSketch("Ah!"))
            assertThat(document).hasMarkupMatching(markupSketch("a:a"), markupSketch("b:b"))

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(1)

            val textNode = textNodes[0]
            assertThat(textNode).hasText("Ah!")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(2)
            assertThat(markupForTextNode).extracting("tag").containsExactly("a:a", "b:b")
        }
    }

    @Test
    fun testTextVariation() {
        val tagML = "$DUMMY_HEADER[t>This is a <|lame|dope|> test!<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("This is a "),
                            textNodeSketch("lame"),
                            textNodeSketch("dope"),
                            textNodeSketch(" test!"))
            assertThat(document).hasMarkupMatching(markupSketch("t"))

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(4)

            val textNode = textNodes[0]
            assertThat(textNode).hasText("This is a ")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(1)
            assertThat(markupForTextNode).extracting("tag").containsExactly("t")
        }
    }

    @Test
    fun testMilestone() {
        // TODO: check the graph: has an extra edge between <t> and the milestone content text node
        val tagML = "$DUMMY_HEADER[t>This is a [space chars=10] test!<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("This is a "),
                            textNodeSketch(""),
                            textNodeSketch(" test!")
                    )
            assertThat(document).hasMarkupMatching(
                    markupSketch("t"),
                    markupSketch("space")
            )

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(3)

            val textNode = textNodes[1]
            assertThat(textNode).hasText("")

            val markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList())
            assertThat(markupForTextNode).hasSize(2)
            assertThat(markupForTextNode).extracting("tag").containsExactly("t", "space")
        }
    }

    @Test
    fun testDiscontinuity() {
        val tagML = "$DUMMY_HEADER[x>[t>This is<-t], he said, [+t>a test!<t]<x]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("This is"),
                            textNodeSketch(", he said, "),
                            textNodeSketch("a test!"))
            assertThat(document).hasMarkupMatching(markupSketch("t"))

            val textNodes = document.textNodeStream.collect(toList())
            assertThat(textNodes).hasSize(3)

            val markups = document.markupStream.collect(toList())
            assertThat(markups).hasSize(3)

            val t = markups[1]
            assertThat(t).hasTag("t")

            val tTAGTextNodes = t.textNodeStream.collect(toList())
            assertThat(tTAGTextNodes).extracting("text").containsExactly("This is", "a test!")

            val t2 = markups[2]
            assertThat(t2).hasTag("t")

            val t2TAGTextNodes = t2.textNodeStream.collect(toList())
            assertThat(t2TAGTextNodes).extracting("text").containsExactly("This is", "a test!")
        }
    }

    @Test
    fun testDiscontinuity2() {
        val tagML = (DUMMY_HEADER
                + "[x>When [t>Could<-t] can [+t>you<-t] I [+t>stop<-t] say [+t>interrupting<-t] something? [+t>me?<t]<x]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document).hasMarkupMatching(markupSketch("t"))

            val markups = document.markupStream.collect(toList())
            assertThat(markups).hasSize(6)
            for (i in intArrayOf(1, 2, 3, 4, 5)) {
                val t = markups[i]
                assertThat(t).hasTag("t")

                val tTAGTextNodes = t.textNodeStream.collect(toList())
                assertThat(tTAGTextNodes)
                        .extracting("text")
                        .containsExactly("Could", "you", "stop", "interrupting", "me?")
            }
        }
    }

    @Test
    fun testUnclosedDiscontinuityLeadsToError() {
        val tagML = "$DUMMY_HEADER[t>This is<-t], he said..."
        val expectedErrors = """
            line 2:11 : The root markup [t] cannot be suspended.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testFalseDiscontinuityLeadsToError() {
        // There must be text between a pause and a resume tag, so the following example is not allowed:
        val tagML = "$DUMMY_HEADER[x>[markup>Cookie <-markup][+markup> Monster<markup]<x]"
        val expectedErrors = "line 2:28 : There is no text between this resume tag: [+markup> and its corresponding suspend tag: <-markup]. This is not allowed."
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testResumeInInnerDocumentLeadsToError() {
        val tagML = (DUMMY_HEADER
                + "[text> [q>Hello my name is "
                + "[gloss addition=[>that's<-q] [qualifier>mrs.<qualifier] to you<]>"
                + "Doubtfire, [+q>how do you do?<q]<gloss]<text] ")
        val expectedErrors = """
            line 2:46 : No text allowed here, the root markup must be started first.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testAcceptedMarkupDifferenceInNonLinearity() {
        val tagML = "$DUMMY_HEADER[t>This [x>is <|a failing|an excellent|><x] test<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes)
                    .extracting("text")
                    .containsExactly(
                            "This ",
                            "is ",  //          "", // <|
                            "a failing",
                            "an excellent",  //          "", // |>
                            " test")

            val tagMarkups = document.markupStream.collect(toList())
            assertThat(tagMarkups)
                    .extracting("tag")
                    .containsExactly("t", "x", BRANCHES, BRANCH, BRANCH)

            val t = tagMarkups[0]
            assertThat(t.tag).isEqualTo("t")

            val tTAGTextNodes = t.textNodeStream.collect(toList())
            assertThat(tTAGTextNodes).hasSize(5)

            val x = tagMarkups[1]
            assertThat(x.tag).isEqualTo("x")

            val xTAGTextNodes = x.textNodeStream.collect(toList())
            assertThat(xTAGTextNodes)
                    .extracting("text")
                    .containsExactly("is ", "a failing", "an excellent")
        }
    }

    @Test
    fun testIllegalMarkupDifferenceInNonLinearity() {
        val tagML = "$DUMMY_HEADER[t>This [x>is <|a [y>failing|an<x] [y>excellent|> test<y]<t]"
        val expectedErrors = """
            line 2:29 : Markup [y> opened in branch 1 must be closed before starting a new branch.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testOpenMarkupInNonLinearAnnotatedTextThrowsError() {
        val tagML = "$DUMMY_HEADER[t>[l>I'm <|done.<l][l>|ready.|finished.|> Let's go!.<l]<t]"
        val expectedErrors = """
            line 2:18 : Markup [l> opened before branch 1, should not be closed in a branch.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testIncorrectOverlapNonLinearityCombination() {
        val tagML = (DUMMY_HEADER
                + "[text|+w>It is a truth universally acknowledged that every "
                + "<|"
                + "[add>young [b|w>woman<add]"
                + "|"
                + "[del>rich<del]"
                + "|>"
                + " man<b|w] is in need of a maid.<text] ")
        val expectedErrors = """
            line 2:88 : Markup [b|w> opened in branch 1 must be closed before starting a new branch.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    //  @Test
    //  public void testCorrectOverlapNonLinearityCombination1() {
    //    String tagML = "[text>It is a truth universally acknowledged that every " +
    //        "<|[add>young [b>woman<b]<add]" +
    //        "|[b>[del>rich<del]|>" +
    //        " man <b] is in need of a maid.<text]";
    //    runInStoreTransaction(store -> {
    //      TAGDocument document = parseTAGML(tagML);
    //      assert(document != null)();
    //      assertThat(document).hasTextNodesMatching(
    //          textNodeSketch("It is a truth universally acknowledged that every "),
    //          textNodeSketch("young "),
    //          textNodeSketch("woman"),
    //          textNodeSketch("rich"),
    //          textNodeSketch(" man "),
    //          textNodeSketch(" is in need of a maid.")
    //      );
    //      assertThat(document).hasMarkupMatching(
    //          markupSketch("text"),
    //          markupSketch("add"),
    //          markupSketch("del"),
    //          markupSketch("b")
    //      );
    //    });
    //  }

    @Test
    fun testCorrectOverlapNonLinearityCombination2() {
        val tagML = (DUMMY_HEADER
                + "[text>It is a truth universally acknowledged that every "
                + "<|[add>young [b>woman<b]<add]"
                + "|[b>[del>rich<del]<b]|>"
                + " [b>man<b] is in need of a maid.<text]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("It is a truth universally acknowledged that every "),
                            textNodeSketch("young "),
                            textNodeSketch("woman"),
                            textNodeSketch("man"),
                            textNodeSketch(" is in need of a maid."))
            assertThat(document)
                    .hasMarkupMatching(
                            markupSketch("text"),
                            markupSketch("add"),
                            markupSketch("del"),
                            markupSketch("b"))
        }
    }

    @Test
    fun testIncorrectDiscontinuityNonLinearityCombination() {
        val tagML = (DUMMY_HEADER
                + "[x>[q>and what is the use of a "
                + "<|[del>book,<del]<-q]"
                + "| [add>thought Alice<add]|>"
                + " [+q>without pictures or conversation?<q]<x]")
        val expectedErrors = """
            line 2:53 : Markup [q> opened before branch 1, should not be closed in a branch.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Ignore
    @Test
    fun testCorrectDiscontinuityNonLinearityCombination() {
        val tagML = (DUMMY_HEADER
                + "[x>[q>and what is the use of a book"
                + "<|[del>, really,<del]"
                + "|[add|+A>,\"<-q] thought Alice [+q>\"<add|A]|>"
                + "without pictures or conversation?<q]<x]")
        //    String tagML = "[x>[q>and what is the use of a book<q]" +
        //        "<|[q>[del>, really,<del]<q]" +
        //        "|[add|+A>[q>,\"<q] thought Alice [q>\"<add|A]<q]|>" +
        //        "[q>without pictures or conversation?<q]<x]";

        //    String tagML = "[tagml|+Q>" +
        //        "[s>[q|Q>\"And what is the use of a book, without pictures or conversation?\"<q|Q]<s]"
        // +
        //        "<tagml|Q]";

        //    String tagML = "[x>[q>and what is the use of a book<-q]" +
        //        "<|[+q>[del>, really,<del]<-q]" +
        //        "|[add|+A>[+q>,\"<-q] thought Alice [+q>\"<add|A]<-q]|>" +
        //        "[+q>without pictures or conversation?<q]<x]";

        //    String tagML = "[x>[q>and what is the use of a book" +
        //        "[del>, really,<del]" +
        //        "[add|+A>,\"<q] thought Alice [q>\"<add|A]" +
        //        "without pictures or conversation?<q]<x]";
        //    String tagML = "[x>[q>and what is the use of a book[del>, really,<del][add|+A>,\"<q]
        // thought Alice [q>\"<add|A]without pictures or conversation?<q]<x]";
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("and what is the use of a book"),
                            textNodeSketch(", really,"),
                            textNodeSketch("\""),
                            textNodeSketch(" thought Alice "),
                            textNodeSketch("\""),
                            textNodeSketch("without pictures or conversation?"))
            assertThat(document).hasMarkupMatching(markupSketch("q"))
        }
    }

    @Test
    fun testEscapeSpecialCharactersInTextVariation() {
        val tagML = "$DUMMY_HEADER[t>bla <|\\||!|> bla<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("bla "),
                            textNodeSketch("|"),
                            textNodeSketch("!"),
                            textNodeSketch(" bla"))
            assertThat(document).hasMarkupMatching(markupSketch("t"))
        }
    }

    @Test
    fun testOptionalMarkup() {
        val tagML = "$DUMMY_HEADER[t>this is [?del>always<?del] optional<t]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("this is "),
                            textNodeSketch("always"),
                            textNodeSketch(" optional"))
            assertThat(document).hasMarkupMatching(markupSketch("t"), optionalMarkupSketch("del"))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(3)

            val always = tagTextNodes[1]
            val tagMarkups = document.getMarkupStreamForTextNode(always).collect(toList())
            assertThat(tagMarkups).hasSize(2)

            val del = tagMarkups[1]
            assertThat(del).hasTag("del")
            assertThat(del).isOptional
        }
    }

    @Test
    fun testContainmentIsDefault() {
        val tagML = "$DUMMY_HEADER[tag>word1 [phr>word2 [phr>word3<phr] word4<phr] word5<tag]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("word1 "),
                            textNodeSketch("word2 "),
                            textNodeSketch("word3"),
                            textNodeSketch(" word4"),
                            textNodeSketch(" word5"))
            assertThat(document).hasMarkupMatching(markupSketch("phr"), markupSketch("phr"))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(5)

            val tagMarkups = document.markupStream.collect(toList())
            val phr0 = tagMarkups[1]
            assertThat(phr0).hasTag("phr")

            val textNodes0 = phr0.textNodeStream.collect(toList())
            assertThat(textNodes0)
                    .extracting("text")
                    .containsExactlyInAnyOrder("word2 ", "word3", " word4")

            val phr1 = tagMarkups[2]
            assertThat(phr1).hasTag("phr")

            val textNodes1 = phr1.textNodeStream.collect(toList())
            assertThat(textNodes1).extracting("text").containsExactly("word3")
        }
    }

    @Test
    fun testUseLayersForSelfOverlap() {
        val tagML = (DUMMY_HEADER
                + "[x|+p1,+p2>word1 [phr|p1>word2 [phr|p2>word3<phr|p1] word4<phr|p2] word5<x|p1,p2]")
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document)
                    .hasTextNodesMatching(
                            textNodeSketch("word1 "),
                            textNodeSketch("word2 "),
                            textNodeSketch("word3"),
                            textNodeSketch(" word4"),
                            textNodeSketch(" word5"))
            assertThat(document).hasMarkupMatching(markupSketch("phr"), markupSketch("phr"))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(5)

            val tagMarkups = document.markupStream.collect(toList())
            val phr0 = tagMarkups[1]
            assertThat(phr0).hasTag("phr")

            val textNodes0 = document.getTextNodeStreamForMarkupInLayer(phr0, "p1").collect(toList())
            assertThat(textNodes0).extracting("text").containsExactly("word2 ", "word3")

            val phr1 = tagMarkups[2]
            assertThat(phr0).hasTag("phr")

            val textNodes1 = document.getTextNodeStreamForMarkupInLayer(phr1, "p2").collect(toList())
            assertThat(textNodes1).extracting("text").containsExactly("word3", " word4")
        }
    }

    @Test
    fun testStringAnnotations() {
        val tagML = "$DUMMY_HEADER[markup a='string' b=\"string\">text<markup]"
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assert(document != null)
            assertThat(document).hasTextNodesMatching(textNodeSketch("text"))
            assertThat(document).hasMarkupMatching(markupSketch("markup"))

            val tagTextNodes = document.textNodeStream.collect(toList())
            assertThat(tagTextNodes).hasSize(1)

            val tagMarkups = document.markupStream.collect(toList())
            val markup = tagMarkups[0]
            val annotationInfos = markup.annotationStream.collect(toList())
            assertThat(annotationInfos).hasSize(2)

            val annotationA = annotationInfos[0]
            assertThat(annotationA).hasTag("a")

            val annotationB = annotationInfos[1]
            assertThat(annotationB).hasTag("b")
        }
    }

    @Test
    fun testListAnnotations() {
        val tagML = "$DUMMY_HEADER[markup primes=[1,2,3,5,7,11]>text<markup]"
        runInStore { store: TAGStore ->
            val doc = store.runInTransaction<TAGDocument> {
                val document = parseTAGML(tagML, store)
                assert(document != null)
                assertThat(document).hasTextNodesMatching(textNodeSketch("text"))
                assertThat(document).hasMarkupMatching(markupSketch("markup"))

                val tagTextNodes = document.textNodeStream.collect(toList())
                assertThat(tagTextNodes).hasSize(1)

                val tagMarkups = document.markupStream.collect(toList())
                val markup = tagMarkups[0]
                val annotationInfos = markup.annotationStream.collect(toList())
                assertThat(annotationInfos).hasSize(1)

                val annotationPrimes = annotationInfos[0]
                assertThat(annotationPrimes).hasTag("primes")
                document
            }
            val export = export(doc, store)
            assertThat(export.replace(".0", "")).isEqualTo(tagML)
        }
    }

    @Test
    fun testUnclosedTextVariationThrowsSyntaxError() {
        val tagML = "$DUMMY_HEADER[t>This is <|good|bad.<t]"
        val expectedErrors = """
            line 2:23 : Markup [t> opened before branch 2, should not be closed in a branch.
            line 2:25 : syntax error: extraneous input '<EOF>' expecting {ITV_EndTextVariation, TextVariationSeparator}
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testJustTextIsNotValidTAGML() {
        val tagML = DUMMY_HEADER + "This is not valid TAGML"
        val expectedErrors = """
            line 1:7 : No text allowed here, the root markup must be started first.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testJustAMilestoneIsNotValidTAGML() {
        val tagML = "$DUMMY_HEADER[miles stone='rock']"
        val expectedErrors = "line 2:1 : The root markup cannot be a milestone tag."
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testOpeningMarkupShouldBeClosedLast() {
        val tagML = "$DUMMY_HEADER[a|+A>AAA AA [b|+B>BBBAAA<a]BBBB<b]"
        val expectedErrors = """
            line 2:29 : No text or markup allowed after the root markup [a] has been ended.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testRootMarkupMayNotBeSuspended() {
        val tagML = "$DUMMY_HEADER[m>foo<-m] fie [+m>bar<m]"
        val expectedErrors = """
            line 2:7 : The root markup [m] cannot be suspended.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    @Test
    fun testLayerNeedsToBeAddedBeforeUse() {
        val tagML = "$DUMMY_HEADER[text|W>Lorem Ipsum [w|W>Dolor<w] Dulcet<text]"
        val expectedErrors = """
            line 2:6 : Layer W has not been added at this point, use +W to add a layer.
            parsing aborted!
            """.trimIndent()
        parseWithExpectedErrors(tagML, expectedErrors)
    }

    // private methods
    private fun parseWithExpectedErrors(tagML: String, expectedErrors: String) {
        runInStoreTransaction { store: TAGStore ->
            try {
                val document = parseTAGML(tagML, store)
                logDocumentGraph(document, tagML)
                fail("TAGMLSyntaxError expected!")
            } catch (e: TAGMLSyntaxError) {
                assertThat(e).hasMessage("Parsing errors:\n$expectedErrors")
            }
        }
    }

    private fun parseTAGML(tagML: String, store: TAGStore): TAGDocument {
        //    LOG.info("TAGML=\n{}\n", tagML);
        val trimmedTagML = tagML.trim { it <= ' ' }
        printTokens(trimmedTagML)
        val document = TAGMLImporter(store).importTAGML(trimmedTagML)
        logDocumentGraph(document, trimmedTagML)
        return document
    }

    private fun export(document: TAGDocument, store: TAGStore): String {
        val tagml = store.runInTransaction<String> {
            val tagmlExporter = TAGMLExporter(store)
            tagmlExporter.asTAGML(document)
        }
        LOG.info("\n\nTAGML:\n{}\n", tagml)
        return tagml
    }

    private fun testRDFConversion(document: TAGDocument) {
        val model = RDFFactory.fromDocument(document)
        val dot = DotFactory.fromModel(model)
        println(
                "\n------------TTL------------------------------------------------------------------------------------\n")
        model.write(System.out, "TURTLE")
        println(
                "\n------------TTL------------------------------------------------------------------------------------\n")
        println(
                "\n------------8<------------------------------------------------------------------------------------\n")
        println(dot)
        println(
                "\n------------8<------------------------------------------------------------------------------------\n")
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(TAGMLImporterTest::class.java)
    }
}
