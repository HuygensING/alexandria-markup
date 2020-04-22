package nl.knaw.huc.di.tag.xml.exporter

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
import nl.knaw.huc.di.tag.TAGViews.getShowAllMarkupView
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huc.di.tag.tagml.xml.exporter.XMLExporter
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.view.TAGView
import org.apache.commons.io.IOUtils
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import org.xml.sax.SAXException
import java.io.IOException
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException

class XMLExporterTest : TAGBaseStoreTest() {
    @Test
    @Throws(Exception::class)
    fun testMilestonesShouldGetSoleIdInTrojanHorse() {
        val tagML = ("""[tagml|+A,+B>[phr|A>Cookie Monster [phr|B>likes<phr|A] [bookmark|B user='Jane']cookies<phr|B]<tagml]""")
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:th="http://www.blackmesatech.com/2017/nss/trojan-horse" th:doc="A B _default">
            <tagml th:doc="A B" th:sId="tagml0"/><phr th:doc="A" th:sId="phr1"/>Cookie Monster <phr th:doc="B" th:sId="phr2"/>likes<phr th:doc="A" th:eId="phr1"/> <bookmark user="Jane" th:doc="B" th:soleId="bookmark3"/>cookies<phr th:doc="B" th:eId="phr2"/><tagml th:doc="A B" th:eId="tagml0"/>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Ignore("TODO: fix default layer handling in view")
    @Test
    fun testXMLExportWithView() {
        val tagML = "[tagml|+A,+B>[phr|A>Cookie Monster [phr|B>likes<phr|A] cookies<phr|B]<tagml]"
        val a: Set<String> = setOf("A")
        runInStore { store: TAGStore ->
            val justA = TAGView(store).withLayersToInclude(a)
            val expectedXML = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xml>
                <tagml><phr>Cookie Monster likes</phr> cookies</tagml>
                </xml>""".trimIndent()
            try {
                assertXMLExportIsAsExpected(tagML, justA, expectedXML, store)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    @Ignore("TODO: fix default layer handling in view")
    @Test
    fun testXMLExportWithViewAndDefaultLayer() {
        val tagML = "[tagml|+A,+B>[phr|A>Cookie Monster [r>really [phr|B>likes<phr|A] [em type=\"CAPS\">cookies<em]<phr|B]<r]<tagml]"
        val a: Set<String> = setOf("A")
        runInStore { store: TAGStore ->
            val justA = TAGView(store).withLayersToInclude(a)
            val expectedXML = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xml>
                <tagml><phr>Cookie Monster really likes</phr> cookies</tagml>
                </xml>""".trimIndent()
            try {
                assertXMLExportIsAsExpected(tagML, justA, expectedXML, store)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    @Test
    @Throws(Exception::class)
    fun testFrostQuote() {
        val tagML = """
            [excerpt|+S,+L source="The Housekeeper" author="Robert Frost">
            [s|S>[l|L n=144>He manages to keep the upper hand<l]
            [l|L n=145>On his own farm.<s] [s|S>He's boss.<s] [s|S>But as to hens:<l]
            [l|L n=146>We fence our flowers in and the hens range.<l]<s]
            <excerpt]""".trimIndent()
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:th="http://www.blackmesatech.com/2017/nss/trojan-horse" th:doc="L S _default">
            <excerpt source="The Housekeeper" author="Robert Frost" th:doc="L S" th:sId="excerpt0"/><s th:doc="S" th:sId="s1"/><l n="144" th:doc="L" th:sId="l2"/>He manages to keep the upper hand<l th:doc="L" th:eId="l2"/>
            <l n="145" th:doc="L" th:sId="l3"/>On his own farm.<s th:doc="S" th:eId="s1"/> <s th:doc="S" th:sId="s4"/>He&apos;s boss.<s th:doc="S" th:eId="s4"/> <s th:doc="S" th:sId="s5"/>But as to hens:<l th:doc="L" th:eId="l3"/>
            <l n="146" th:doc="L" th:sId="l6"/>We fence our flowers in and the hens range.<l th:doc="L" th:eId="l6"/><s th:doc="S" th:eId="s5"/>
            <excerpt th:doc="L S" th:eId="excerpt0"/>
            </xml>
            """.trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testCMLHTS18() {
        val tagML = """
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
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <tagml><page><p><line>1st. Voice from the Springs</line>
            <line>Thrice three hundred thousand years</line>
            <line>We had been stained with bitter blood</line>
            </p>
            </page>
            <page>
            <p>
            <line>And had ran mute &apos;mid shrieks of slaugter</line>
            <line>Thro&apos; a city and a multitude</line>
            </p>
            </page>
            </tagml>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testLayerIdentifiersAreOptionalInEndTags() {
        val tagML = """
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
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:th="http://www.blackmesatech.com/2017/nss/trojan-horse" th:doc="A B _default">
            <tagml th:doc="_default" th:sId="tagml0"/><text th:doc="A B" th:sId="text1"/><line th:doc="_default" th:sId="line2"/><page th:doc="A" th:sId="page3"/><p th:doc="B" th:sId="p4"/>1st. Voice from the Springs<line th:doc="_default" th:eId="line2"/>
            <line th:doc="_default" th:sId="line5"/>Thrice three hundred thousand years<line th:doc="_default" th:eId="line5"/>
            <line th:doc="_default" th:sId="line6"/>We had been stained with bitter blood<line th:doc="_default" th:eId="line6"/>
            <page th:doc="A" th:eId="page3"/>
            <page th:doc="A" th:sId="page7"/>
            <line th:doc="_default" th:sId="line8"/>And had ran mute &apos;mid shrieks of slaugter[sic]<line th:doc="_default" th:eId="line8"/>
            <line th:doc="_default" th:sId="line9"/>Thro&apos; a city &amp; a multitude<line th:doc="_default" th:eId="line9"/>
            <p th:doc="B" th:eId="p4"/>
            <page th:doc="A" th:eId="page7"/>
            <text th:doc="A B" th:eId="text1"/>
            <tagml th:doc="_default" th:eId="tagml0"/>
            </xml>
            """.trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Ignore("TODO: fix default layer handling in view")
    @Test
    @Throws(Exception::class)
    fun testLayerIdentifiersAreOptionalInEndTagWhenNotAmbiguous() {
        val tagML = "[tagml|+A>Some text<tagml]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <tagml>Some text</tagml>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Ignore("TODO: fix default layer handling in view")
    @Test
    @Throws(Exception::class)
    fun testNoLayerInfoOnEndTagWithMultipleStartTagsInSameLayers() {
        val tagML = "[tagml|+A>[p|A>Paragraph starts [p|A>Nested Paragraph<p] paragraph ends<p]<tagml]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <tagml><p>Paragraph starts <p>Nested Paragraph</p> paragraph ends</p></tagml>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testSimpleTAGML() {
        val tagML = "[line>The rain in Spain falls mainly on the plain.<line]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <line>The rain in Spain falls mainly on the plain.</line>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testCharacterEscapingInRegularText() {
        val tagML = "[tagml>In regular text, \\<, \\[ and \\\\ need to be escaped, |, !, \", and ' don't.<tagml]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <tagml>In regular text, &lt;, [ and \ need to be escaped, |, !, &quot;, and &apos; don&apos;t.</tagml>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testCharacterEscapingInTextVariation() {
        val tagML = ("""[t>In text in between textVariation tags, <|\<, \[, \| and \\ need to be escaped|!, " and ' don't|>.<t]""")
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <t>In text in between textVariation tags, <tag:branches><tag:branch>&lt;, [, | and \ need to be escaped</tag:branch><tag:branch>!, &quot; and &apos; don&apos;t</tag:branch></tag:branches>.</t>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testOverlap() {
        val tagML = "[x|+la,+lb>[a|la>J'onn J'onzz [b|lb>likes<a|la] Oreos<b|lb]<x|la,lb]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:th="http://www.blackmesatech.com/2017/nss/trojan-horse" th:doc="_default la lb">
            <x th:doc="la lb" th:sId="x0"/><a th:doc="la" th:sId="a1"/>J&apos;onn J&apos;onzz <b th:doc="lb" th:sId="b2"/>likes<a th:doc="la" th:eId="a1"/> Oreos<b th:doc="lb" th:eId="b2"/><x th:doc="la lb" th:eId="x0"/>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testTAGML2() {
        val tagML = ("""[line|+a,+b>[a|a>The rain in [country>Spain<country] [b|b>falls<a|a] mainly on the plain.<b|b]<line|a,b]""")
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:th="http://www.blackmesatech.com/2017/nss/trojan-horse" th:doc="_default a b">
            <line th:doc="_default a b" th:sId="line0"/><a th:doc="a" th:sId="a1"/>The rain in <country th:doc="_default" th:sId="country2"/>Spain<country th:doc="_default" th:eId="country2"/> <b th:doc="b" th:sId="b3"/>falls<a th:doc="a" th:eId="a1"/> mainly on the plain.<b th:doc="b" th:eId="b3"/><line th:doc="_default a b" th:eId="line0"/>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testCommentsAreIgnored() {
        val tagML = "[! before !][a>Ah![! within !]<a][! after !]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <a>Ah!</a>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testNamespace() {
        val tagML = "[!ns a http://tag.com/a][a:a>Ah!<a:a]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:a="http://tag.com/a">
            <a:a>Ah!</a:a>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testMultipleNamespaces() {
        val tagML = "[!ns a http://tag.com/a]\n[!ns b http://tag.com/b]\n[a:a>[b:b>Ah!<b:b]<a:a]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:a="http://tag.com/a" xmlns:b="http://tag.com/b">
            <a:a><b:b>Ah!</b:b></a:a>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testTextVariation() {
        val tagML = "[t>This is a <|lame|dope|> test!<t]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <t>This is a <tag:branches><tag:branch>lame</tag:branch><tag:branch>dope</tag:branch></tag:branches> test!</t>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testMilestone() {
        val tagML = "[t>This is a [space chars=10] test!<t]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <t>This is a <space chars="10"/> test!</t>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testDiscontinuity() {
        val tagML = "[x>[t>This is<-t], he said, [+t>a test!<t]<x]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <x><t tag:n="1">This is</t>, he said, <t tag:n="1">a test!</t></x>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testAcceptedMarkupDifferenceInNonLinearity() {
        val tagML = "[t>This [x>is <|a failing|an excellent|><x] test<t]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <t>This <x>is <tag:branches><tag:branch>a failing</tag:branch><tag:branch>an excellent</tag:branch></tag:branches></x> test</t>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testCorrectOverlapNonLinearityCombination2() {
        val tagML = ("[text>It is a truth universally acknowledged that every "
                + "<|[add>young [b>woman<b]<add]"
                + "|[b>[del>rich<del]<b]|>"
                + " [b>man<b] is in need of a maid.<text]")
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <text>It is a truth universally acknowledged that every <tag:branches><tag:branch><add>young <b>woman</b></add></tag:branch><tag:branch><b><del>rich</del></b></tag:branch></tag:branches> <b>man</b> is in need of a maid.</text>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Ignore
    @Test
    @Throws(Exception::class)
    fun testCorrectDiscontinuityNonLinearityCombination() {
        val tagML = ("[x>[q>and what is the use of a book"
                + "<|[del>, really,<del]"
                + "|[add|+A>,\"<-q] thought Alice [+q>\"<add|A]|>"
                + "without pictures or conversation?<q]<x]")
        val expectedXML = "TODO!"
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testEscapeSpecialCharactersInTextVariation() {
        val tagML = "[t>bla <|\\||!|> bla<t]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <t>bla <tag:branches><tag:branch>|</tag:branch><tag:branch>!</tag:branch></tag:branches> bla</t>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testOptionalMarkup() {
        val tagML = "[t>this is [?del>always<?del] optional<t]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:tag="http://tag.di.huc.knaw.nl/ns/tag">
            <t>this is <del tag:optional="true">always</del> optional</t>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testContainmentIsDefault() {
        val tagML = "[tag>word1 [phr>word2 [phr>word3<phr] word4<phr] word5<tag]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <tag>word1 <phr>word2 <phr>word3</phr> word4</phr> word5</tag>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testUseLayersForSelfOverlap() {
        val tagML = "[x|+p1,+p2>word1 [phr|p1>word2 [phr|p2>word3<phr|p1] word4<phr|p2] word5<x|p1,p2]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml xmlns:th="http://www.blackmesatech.com/2017/nss/trojan-horse" th:doc="_default p1 p2">
            <x th:doc="p1 p2" th:sId="x0"/>word1 <phr th:doc="p1" th:sId="phr1"/>word2 <phr th:doc="p2" th:sId="phr2"/>word3<phr th:doc="p1" th:eId="phr1"/> word4<phr th:doc="p2" th:eId="phr2"/> word5<x th:doc="p1 p2" th:eId="x0"/>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testStringAnnotations() {
        val tagML = "[markup a='string' b=\"string\">text<markup]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <markup a="string" b="string">text</markup>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testBooleanAnnotations() {
        val tagML = "[markup a=TRUE b=false>text<markup]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <markup a="true" b="false">text</markup>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testNumberAnnotations() {
        val tagML = "[markup a=1 b=3.14>text<markup]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <markup a="1" b="3.14">text</markup>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testListAnnotations() {
        val tagML = "[markup primes=[1,2,3,5,7,11]>text<markup]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <markup primes="[&quot;1&quot;,&quot;2&quot;,&quot;3&quot;,&quot;5&quot;,&quot;7&quot;,&quot;11&quot;]">text</markup>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    @Test
    @Throws(Exception::class)
    fun testMapAnnotations() {
        val tagML = "[markup author={first='Harley' last='Quinn'}>text<markup]"
        val expectedXML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xml>
            <markup author="{first=&quot;Harley&quot;,last=&quot;Quinn&quot;}">text</markup>
            </xml>""".trimIndent()
        assertXMLExportIsAsExpected(tagML, expectedXML)
    }

    private fun parseTAGML(tagML: String, store: TAGStore): TAGDocument {
        log.info("TAGML=\n\n{}\n", tagML)
        //    printTokens(tagML);
        //    logDocumentGraph(document, tagML);
        return TAGMLImporter(store).importTAGML(tagML)
    }

    private fun assertXMLExportIsAsExpected(body: String, expectedXML: String) {
        runInStore { store: TAGStore ->
            try {
                assertXMLExportIsAsExpected(
                        body, getShowAllMarkupView(store), expectedXML, store)
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }

    @Throws(Exception::class)
    private fun assertXMLExportIsAsExpected(
            body: String, view: TAGView, expectedXML: String, store: TAGStore) {
        val tagml = addTAGMLHeader(body)
        val document = store.runInTransaction<TAGDocument> { parseTAGML(tagml, store) }
        assertThat(document).isNotNull

        val xml = store.runInTransaction<String> { XMLExporter(store, view).asXML(document) }
        log.info("XML=\n\n{}\n", xml)
        assertThat(xml).isEqualTo(expectedXML)
        validateXML(xml)
    }

    @Throws(SAXException::class, IOException::class, ParserConfigurationException::class)
    private fun validateXML(xml: String) {
        val inputStream = IOUtils.toInputStream(xml, Charset.defaultCharset())
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream)
    }

    companion object {
        private val log = LoggerFactory.getLogger(XMLExporterTest::class.java)
    }
}
