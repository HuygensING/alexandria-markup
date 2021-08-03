package nl.knaw.huygens.alexandria.texmecs.importer;

/*
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.AntlrUtils;
import nl.knaw.huygens.alexandria.data_model.Annotation;
import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.data_model.Limen;
import nl.knaw.huygens.alexandria.data_model.Markup;
import nl.knaw.huygens.alexandria.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporterInMemory;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSLexer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class TexMECSImporterInMemoryTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testExample1() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b>|s>";
    Document document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithAttributes() {
    String texMECS = "<s type='test'|<a|John <b|loves|a> Mary|b>|s>";
    Document document = testTexMECS(texMECS, "[s [type}test{type]}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
    Markup markup0 = document.value().markupList.get(0);
    assertThat(markup0.getTag()).isEqualTo("s");
    Annotation annotation = markup0.getAnnotations().get(0);
    assertThat(annotation.getTag()).isEqualTo("type");
    List<TextNode> textNodeList = annotation.value().textNodeList;
    assertThat(textNodeList).hasSize(1);
    assertThat(textNodeList.get(0).getContent()).isEqualTo("test");
  }

  @Test
  public void testExample1WithSuffix() {
    String texMECS = "<s~0|<a|John <b|loves|a> Mary|b>|s~0>";
    Document document = testTexMECS(texMECS, "[s~0}[a}John [b}loves{a] Mary{b]{s~0]");
    assertThat(document.value()).isNotNull();
    Markup markup0 = document.value().markupList.get(0);
    assertThat(markup0.getTag()).isEqualTo("s");
    assertThat(markup0.getSuffix()).isEqualTo("0");
  }

  @Test
  public void testExample1WithSoleTag() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><empty purpose='test'>|s>";
    Document document =
        testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b][empty [purpose}test{purpose]]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithSuspendResumeTags() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|-b>, or so he says, <+b|very much|b>|s>";
    Document document =
        testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b], or so he says, [b}very much{b]{s]");
    Limen limen = document.value();
    assertThat(limen).isNotNull();
    List<Markup> markupList = limen.markupList;
    assertThat(markupList).hasSize(3); // s, a, b
    Markup markup = markupList.get(2);
    assertThat(markup.getTag()).isEqualTo("b");
    List<TextNode> textNodes = markup.textNodes;
    assertThat(textNodes).hasSize(3);
    List<String> textNodeContents =
        textNodes.stream().map(TextNode::getContent).collect(Collectors.toList());
    assertThat(textNodeContents).containsExactly("loves", " Mary", "very much");
  }

  @Test
  public void testExample1WithComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right! *>|s>";
    Document document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithNestedComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right<*actually...*>!*>|s>";
    Document document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testExample1WithCData() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><#CDATA<some cdata>#CDATA>|s>";
    Document document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testSelfOverlappingElements() {
    String texMECS = "<e~1|Lorem <e~2|Ipsum |e~1>Dolor...|e~2>";
    Document document = testTexMECS(texMECS, "[e~1}Lorem [e~2}Ipsum {e~1]Dolor...{e~2]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testTagSets() {
    String texMECS = "<|choice||<option|A|option><option|B|option>||choice|>";
    Document document = testTexMECS(texMECS, "[choice}[option}A{option][option}B{option]{choice]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testVirtualElement() {
    String texMECS = "<real|<e=e1|Reality|e>|real><virtual|<^e^e1>|virtual>";
    Document document =
        testTexMECS(texMECS, "[real}[e=e1}Reality{e=e1]{real][virtual}[e}Reality{e]{virtual]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testMultipleRoots() {
    String texMECS = "<a|A|a><a|A|a><a|A|a><a|A|a><a|A|a>";
    Document document = testTexMECS(texMECS, "[a}A{a][a}A{a][a}A{a][a}A{a][a}A{a]");
    assertThat(document.value()).isNotNull();
  }

  @Test
  public void testDominance() {
    String texMECS = "<l|This is <i|<b|very|b>|i> important|l>";
    Document document = testTexMECS(texMECS, "[l}This is [i}[b}very{b]{i] important{l]");
    assertThat(document.value()).isNotNull();
    List<Markup> markupList = document.value().markupList;
    Markup markupI = markupList.get(1);
    assertThat(markupI.getExtendedTag()).isEqualTo("i");

    Markup markupB = markupList.get(2);
    assertThat(markupB.getExtendedTag()).isEqualTo("b");
    assertThat(markupI.getDominatedMarkup().get()).isEqualTo(markupB);
    assertThat(markupB.getDominatingMarkup().get()).isEqualTo(markupI);
  }

  @Test
  public void testSyntaxError1() {
    String texMECS = "<tag|opening, but not closing";
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage()).contains("Some markup was not closed: <tag|");
    }
  }

  @Test
  public void testSyntaxError2() {
    String texMECS = "no opening tag|bla>";
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage())
          .contains("Closing tag |bla> found, which has no corresponding earlier opening tag.");
    }
  }

  @Test
  public void testSyntaxError3() {
    String texMECS = "<^v^v12>";
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage())
          .contains(
              "idref 'v12' not found: No <v@v12| tag found that this virtual element refers to.");
    }
  }

  @Test
  public void testSyntaxErrorSuspendWithoutResume() {
    String texMECS = "<tag|Lorem ipsum|-tag> dolores rosetta|tag>";
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage())
          .contains("Closing tag |tag> found, which has no corresponding earlier opening tag.");
    }
  }

  @Test
  public void testSyntaxErrorResumeWithoutSuspend() {
    String texMECS = "<tag|Lorem ipsum <+tag|dolores rosetta|tag>";
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage())
          .contains(
              "Resuming tag <+tag| found, which has no corresponding earlier suspending tag |-tag>.");
    }
  }

  @Test
  public void testDuplicateIdError() {
    String texMECS = "<tag@t1|Lorem ipsum <b@t1|Dolores|b> dulcetto.|tag>";
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage()).contains("id 't1' was already used in markup <tag@t1|.");
    }
  }

  @Test
  public void testSyntaxError4() throws IOException {
    String pathname = "data/texmecs/acrostic-syntax-error.texmecs";
    String texMECS = FileUtils.readFileToString(new File(pathname), StandardCharsets.UTF_8);
    try {
      Document document = testTexMECS(texMECS, "whatever");
      fail();
    } catch (TexMECSSyntaxError se) {
      LOG.warn(se.getMessage());
      assertThat(se.getMessage()).contains("Parsing errors");
    }
  }

  private Document testTexMECS(String texMECS, String expectedLMNL) {
    printTokens(texMECS);

    LOG.info("parsing {}", texMECS);
    TexMECSImporterInMemory importer = new TexMECSImporterInMemory();
    Document doc = importer.importTexMECS(texMECS);
    assertThat(doc.value()).isNotNull();
    LMNLExporterInMemory ex = new LMNLExporterInMemory();
    String lmnl = ex.toLMNL(doc);
    LOG.info("lmnl={}", lmnl);
    assertThat(lmnl).isEqualTo(expectedLMNL);

    return doc;
  }

  private void printTokens(String input) {
    System.out.println("TexMECS:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
    System.out.println(
        "--------------------------------------------------------------------------------");
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    TexMECSLexer lexer = new TexMECSLexer(inputStream);
    String tokenTable = AntlrUtils.makeTokenTable(lexer);
    System.out.println(tokenTable);
  }
}
