package nl.knaw.huygens.alexandria.creole;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.NameClasses.name;
import org.junit.Test;

public class SchemaImporterTest {

  private static final Pattern BIBLICAL_EXAMPLE_SCHEMA_PATTERN = biblicalExampleSchemaPattern();

  @Test
  public void testFromXML() {
    String xml = "<start><text/></start>";

    Pattern schema = SchemaImporter.fromXML(xml);
    assertSchemaIsExpected(schema, Patterns.TEXT);
  }

  @Test
  public void testFromCompactGrammar() {
    String compactGrammar = "start = text";

    Pattern schema = SchemaImporter.fromCompactGrammar(compactGrammar);
//    assertSchemaIsExpected(schema, Patterns.TEXT);
  }

  @Test
  public void testBiblicalExampleGrammarFromXML() {
    String xml = "<grammar xmlns=\"http://lmnl.net/ns/creole\">\n" +//
        "<start>\n" +//
        "  <ref name=\"book\" />\n" +//
        "</start>\n" +//
        "\n" +//
        "<define name=\"book\">\n" +//
        "  <element name=\"book\">\n" +//
        "    <concur>\n" +//
        "      <oneOrMore>\n" +//
        "        <ref name=\"page\" />\n" +//
        "      </oneOrMore>\n" +//
        "      <group>\n" +//
        "        <ref name=\"title\" />\n" +//
        "        <concur>\n" +//
        "          <oneOrMore>\n" +//
        "            <ref name=\"chapter\" />\n" +//
        "          </oneOrMore>\n" +//
        "          <oneOrMore>\n" +//
        "            <ref name=\"section\" />\n" +//
        "          </oneOrMore>\n" +//
        "        </concur>\n" +//
        "      </group>\n" +//
        "    </concur>\n" +//
        "  </element>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"page\">\n" +//
        "  <range name=\"page\">\n" +//
        "    <attribute name=\"no\" />\n" +//
        "    <text />\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"title\">\n" +//
        "  <element name=\"title\"><text /></element>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"chapter\">\n" +//
        "  <range name=\"chapter\">\n" +//
        "    <attribute name=\"no\" />\n" +//
        "    <oneOrMore>\n" +//
        "      <ref name=\"verse\" />\n" +//
        "    </oneOrMore>\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"verse\">\n" +//
        "  <range name=\"verse\">\n" +//
        "    <attribute name=\"no\" />\n" +//
        "    <text />\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"section\">\n" +//
        "  <range name=\"section\">\n" +//
        "    <ref name=\"heading\" />\n" +//
        "    <oneOrMore>\n" +//
        "      <ref name=\"para\" />\n" +//
        "    </oneOrMore>\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"heading\">\n" +//
        "  <element name=\"heading\">\n" +//
        "    <ref name=\"indexedText\" />\n" +//
        "  </element>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"indexedText\">\n" +//
        "  <concurOneOrMore>\n" +//
        "    <mixed>\n" +//
        "      <zeroOrMore>\n" +//
        "        <ref name=\"index\" />\n" +//
        "      </zeroOrMore>\n" +//
        "    </mixed>\n" +//
        "  </concurOneOrMore>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"para\">\n" +//
        "  <range name=\"para\">\n" +//
        "    <concur>\n" +//
        "      <oneOrMore>\n" +//
        "        <ref name=\"verse\" />\n" +//
        "      </oneOrMore>\n" +//
        "      <oneOrMore>\n" +//
        "        <ref name=\"s\" />\n" +//
        "      </oneOrMore>\n" +//
        "    </concur>\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"s\">\n" +//
        "  <range name=\"s\">\n" +//
        "    <ref name=\"indexedText\" />\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "<define name=\"index\">\n" +//
        "  <range name=\"index\">\n" +//
        "    <attribute name=\"ref\" />\n" +//
        "    <text />\n" +//
        "  </range>\n" +//
        "</define>\n" +//
        "\n" +//
        "</grammar>";

    Pattern schema = SchemaImporter.fromXML(xml);
    assertSchemaIsExpected(schema, BIBLICAL_EXAMPLE_SCHEMA_PATTERN);
  }


  @Test
  public void testBiblicalExampleFromCompactGrammar() {
    String compactGrammar = "start = book\n" +//
        "book = element book { page ~ \n" +//
        "                      ( title, ( chapter+ ~ section+ ) ) }\n" +//
        "page = range page { attribute no { text }, text }\n" +//
        "title = element title { text }\n" +//
        "chapter = range chapter { attribute no { text }, verse+ }\n" +//
        "verse = range verse { attribute no { text }, text }\n" +//
        "section = range section { heading, para+ }\n" +//
        "heading = element heading { indexedText }\n" +//
        "para = range para { verse+ ~ s+ }\n" +// wo
        "s = range s { indexedText }\n" +//
        "indexedText = concurOneOrMore { mixed { index* } }\n" +//
        "index = range index { attribute ref { text }, text }";

    Pattern schema = SchemaImporter.fromCompactGrammar(compactGrammar);
//    assertSchemaIsExpected(schema, BIBLICAL_EXAMPLE_SCHEMA_PATTERN);
  }

  private static Pattern biblicalExampleSchemaPattern() {
    Pattern page = range(name("page"), text());
    Pattern title = element("title", text());
    Pattern verse = range(name("verse"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));
    Pattern index = range(name("index"), text());
    Pattern indexedText = concurOneOrMore(mixed(zeroOrMore(index)));
    Pattern heading = element("heading", indexedText);
    Pattern s = range(name("s"), indexedText);
    Pattern para = range(name("para"),//
        concur(//
            oneOrMore(verse),//
            oneOrMore(s)
        )//
    );
    Pattern section = range(name("section"),//
        group(//
            heading,//
            oneOrMore(para)//
        )//
    );
    return element("book",//
        concur(//
            oneOrMore(page),//
            group(//
                title,//
                concur(//
                    oneOrMore(chapter),//
                    oneOrMore(section)//
                )//
            )//
        )//
    );
  }

  private void assertSchemaIsExpected(Pattern schema, Pattern expectedSchema) {
    String schemaString = Utilities.patternTreeToDepth(schema, 100);
    String expectedSchemaString = Utilities.patternTreeToDepth(expectedSchema, 100);
    assertThat(schemaString).isEqualTo(expectedSchemaString);
    //    assertThat(schema).isEqualTo(BIBLICAL_EXAMPLE_SCHEMA_PATTERN);
  }

}
