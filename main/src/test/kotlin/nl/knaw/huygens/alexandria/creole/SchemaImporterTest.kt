package nl.knaw.huygens.alexandria.creole

/*-
* #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
*/

import nl.knaw.huygens.alexandria.creole.Constructors.choice
import nl.knaw.huygens.alexandria.creole.Constructors.concur
import nl.knaw.huygens.alexandria.creole.Constructors.concurOneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.element
import nl.knaw.huygens.alexandria.creole.Constructors.group
import nl.knaw.huygens.alexandria.creole.Constructors.interleave
import nl.knaw.huygens.alexandria.creole.Constructors.mixed
import nl.knaw.huygens.alexandria.creole.Constructors.oneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.range
import nl.knaw.huygens.alexandria.creole.Constructors.text
import nl.knaw.huygens.alexandria.creole.Constructors.zeroOrMore
import nl.knaw.huygens.alexandria.creole.NameClasses.name
import nl.knaw.huygens.alexandria.creole.patterns.Patterns
import org.assertj.core.api.Assertions
import org.junit.Test

class SchemaImporterTest : CreoleTest() {

    @Test
    fun testFromXML() {
        val xml = "<start><text/></start>"

        val schema = SchemaImporter.fromXML(xml)
        assertSchemaIsExpected(schema, Patterns.TEXT)
    }

    @Test
    fun testFromCompactGrammar() {
        val compactGrammar = "start = text"

        val schema = SchemaImporter.fromCompactGrammar(compactGrammar)
        //    assertSchemaIsExpected(schema, Patterns.TEXT);
    }

    @Test
    fun testBiblicalExampleGrammarFromXML() {

        val xml = "<grammar xmlns=\"http://lmnl.net/ns/creole\">\n" +

                "<start>\n" +

                "  <ref name=\"book\" />\n" +

                "</start>\n" +

                "\n" +

                "<define name=\"book\">\n" +

                "  <element name=\"book\">\n" +

                "    <concur>\n" +

                "      <oneOrMore>\n" +

                "        <ref name=\"page\" />\n" +

                "      </oneOrMore>\n" +

                "      <group>\n" +

                "        <ref name=\"title\" />\n" +

                "        <concur>\n" +

                "          <oneOrMore>\n" +

                "            <ref name=\"chapter\" />\n" +

                "          </oneOrMore>\n" +

                "          <oneOrMore>\n" +

                "            <ref name=\"section\" />\n" +

                "          </oneOrMore>\n" +

                "        </concur>\n" +

                "      </group>\n" +

                "    </concur>\n" +

                "  </element>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"page\">\n" +

                "  <range name=\"page\">\n" +

                "    <attribute name=\"no\" />\n" +

                "    <text />\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"title\">\n" +

                "  <element name=\"title\"><text /></element>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"chapter\">\n" +

                "  <range name=\"chapter\">\n" +

                "    <attribute name=\"no\" />\n" +

                "    <oneOrMore>\n" +

                "      <ref name=\"verse\" />\n" +

                "    </oneOrMore>\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"verse\">\n" +

                "  <range name=\"verse\">\n" +

                "    <attribute name=\"no\" />\n" +

                "    <text />\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"section\">\n" +

                "  <range name=\"section\">\n" +

                "    <ref name=\"heading\" />\n" +

                "    <oneOrMore>\n" +

                "      <ref name=\"para\" />\n" +

                "    </oneOrMore>\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"heading\">\n" +

                "  <element name=\"heading\">\n" +

                "    <ref name=\"indexedText\" />\n" +

                "  </element>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"indexedText\">\n" +

                "  <concurOneOrMore>\n" +

                "    <mixed>\n" +

                "      <zeroOrMore>\n" +

                "        <ref name=\"index\" />\n" +

                "      </zeroOrMore>\n" +

                "    </mixed>\n" +

                "  </concurOneOrMore>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"para\">\n" +

                "  <range name=\"para\">\n" +

                "    <concur>\n" +

                "      <oneOrMore>\n" +

                "        <ref name=\"verse\" />\n" +

                "      </oneOrMore>\n" +

                "      <oneOrMore>\n" +

                "        <ref name=\"s\" />\n" +

                "      </oneOrMore>\n" +

                "    </concur>\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"s\">\n" +

                "  <range name=\"s\">\n" +

                "    <ref name=\"indexedText\" />\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "<define name=\"index\">\n" +

                "  <range name=\"index\">\n" +

                "    <attribute name=\"ref\" />\n" +

                "    <text />\n" +

                "  </range>\n" +

                "</define>\n" +

                "\n" +

                "</grammar>"

        val schema = SchemaImporter.fromXML(xml)
        assertSchemaIsExpected(schema, BIBLICAL_EXAMPLE_SCHEMA_PATTERN)
    }


    @Test
    fun testBiblicalExampleFromCompactGrammar() {
        val compactGrammar = "start = book\n" +
                "book = element book { page ~ \n" +
                "                      ( title, ( chapter+ ~ section+ ) ) }\n" +
                "page = range page { attribute no { text }, text }\n" +
                "title = element title { text }\n" +
                "chapter = range chapter { attribute no { text }, verse+ }\n" +
                "verse = range verse { attribute no { text }, text }\n" +
                "section = range section { heading, para+ }\n" +
                "heading = element heading { indexedText }\n" +
                "para = range para { verse+ ~ s+ }\n" +// wo
                "s = range s { indexedText }\n" +
                "indexedText = concurOneOrMore { mixed { index* } }\n" +
                "index = range index { attribute ref { text }, text }"

        val schema = SchemaImporter.fromCompactGrammar(compactGrammar)
        //    assertSchemaIsExpected(schema, BIBLICAL_EXAMPLE_SCHEMA_PATTERN);
    }

    @Test
    fun testInterleaveSimplification() {
        val xml = """
            |<start>
            |<interleave>
            |<range name="a"><text/></range>
            |<range name="b"><text/></range>
            |<range name="c"><text/></range>
            |</interleave>
            |</start>""".trimMargin()
        val schema = SchemaImporter.fromXML(xml)
        val expected = interleave(
                range(name("a"), text()),
                interleave(
                        range(name("b"), text()),
                        range(name("c"), text())
                )
        )
        assertSchemaIsExpected(schema, expected)
    }

    @Test
    fun testGroupSimplification() {
        val xml = """
            |<start>
            |<group>
            |<range name="a"><text/></range>
            |<range name="b"><text/></range>
            |<range name="c"><text/></range>
            |</group>
            |</start>""".trimMargin()
        val schema = SchemaImporter.fromXML(xml)
        val expected = group(
                range(name("a"), text()),
                group(
                        range(name("b"), text()),
                        range(name("c"), text())
                )
        )
        assertSchemaIsExpected(schema, expected)
    }

    @Test
    fun testChoiceSimplification() {
        val xml = """
            |<start>
            |<choice>
            |<range name="a"><text/></range>
            |<range name="b"><text/></range>
            |<range name="c"><text/></range>
            |</choice>
            |</start>""".trimMargin()
        val schema = SchemaImporter.fromXML(xml)
        val expected = choice(
                range(name("a"), text()),
                choice(
                        range(name("b"), text()),
                        range(name("c"), text())
                )
        )
        assertSchemaIsExpected(schema, expected)
    }

    @Test
    fun testConcurSimplification() {
        val xml = """
            |<start><concur>
            |<range name="a"><text/></range>
            |<range name="b"><text/></range>
            |<range name="c"><text/></range>
            |</concur></start>""".trimMargin()
        val schema = SchemaImporter.fromXML(xml)
        val expected = concur(
                range(name("a"), text()),
                concur(
                        range(name("b"), text()),
                        range(name("c"), text())
                )
        )
        assertSchemaIsExpected(schema, expected)
    }

    private fun assertSchemaIsExpected(schema: Pattern, expectedSchema: Pattern) {
        val schemaString = Utilities.patternTreeToDepth(schema, 100)
        val expectedSchemaString = Utilities.patternTreeToDepth(expectedSchema, 100)
        Assertions.assertThat(schemaString).isEqualTo(expectedSchemaString)
        //    assertThat(schema).isEqualTo(BIBLICAL_EXAMPLE_SCHEMA_PATTERN);
    }

    companion object {

        private val BIBLICAL_EXAMPLE_SCHEMA_PATTERN = biblicalExampleSchemaPattern()

        private fun biblicalExampleSchemaPattern(): Pattern {
            val page = range(name("page"), text())
            val title = element("title", text())
            val verse = range(name("verse"), text())
            val chapter = range(name("chapter"), oneOrMore(verse))
            val index = range(name("index"), text())
            val indexedText = concurOneOrMore(mixed(zeroOrMore(index)))
            val heading = element("heading", indexedText)
            val s = range(name("s"), indexedText)
            val para = range(name("para"),
                    concur(
                            oneOrMore(verse),
                            oneOrMore(s)
                    )
            )
            val section = range(name("section"),
                    group(
                            heading,
                            oneOrMore(para)
                    )
            )
            return element("book",
                    concur(
                            oneOrMore(page),
                            group(
                                    title,
                                    concur(
                                            oneOrMore(chapter),
                                            oneOrMore(section)
                                    )
                            )
                    )
            )
        }
    }

}
