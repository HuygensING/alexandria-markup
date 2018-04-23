package nl.knaw.huygens.alexandria.texmecs.importer;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.AntlrUtils;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class TexMECSImporterTest extends AlexandriaBaseStoreTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testExample1() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testExample1WithAttributes() {
    String texMECS = "<s type='test'|<a|John <b|loves|a> Mary|b>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s [type}test{type]}[a}John [b}loves{a] Mary{b]{s]");
      assertThat(document).isNotNull();
      MarkupWrapper markup0 = document.getMarkupStream().findFirst().get();
      assertThat(markup0.getTag()).isEqualTo("s");
      AnnotationWrapper annotation = markup0.getAnnotationStream().findFirst().get();
      assertThat(annotation.getTag()).isEqualTo("type");
      List<TextNodeWrapper> textNodeList = annotation.getDocument().getTextNodeStream().collect(toList());
      assertThat(textNodeList).hasSize(1);
      assertThat(textNodeList.get(0).getText()).isEqualTo("test");
    });
  }

  @Test
  public void testExample1WithSuffix() {
    String texMECS = "<s~0|<a|John <b|loves|a> Mary|b>|s~0>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s~0}[a}John [b}loves{a] Mary{b]{s~0]");
      assertThat(document).isNotNull();
      MarkupWrapper markup0 = document.getMarkupStream().findFirst().get();
      assertThat(markup0.getTag()).isEqualTo("s");
      assertThat(markup0.getSuffix()).isEqualTo("0");
    });
  }

  @Test
  public void testExample1WithSoleTag() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><empty purpose='test'>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b][empty [purpose}test{purpose]]{s]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testExample1WithSuspendResumeTags() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|-b>, or so he says, <+b|very much|b>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b], or so he says, [b}very much{b]{s]");
      assertThat(document).isNotNull();
      List<MarkupWrapper> markupList = document.getMarkupStream().collect(toList());
      assertThat(markupList).hasSize(3); // s, a, b
      MarkupWrapper markup = markupList.get(2);
      assertThat(markup.getTag()).isEqualTo("b");
      List<TextNodeWrapper> textNodes = markup.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(3);
      List<String> textNodeContents = textNodes.stream().map(TextNodeWrapper::getText).collect(toList());
      assertThat(textNodeContents).containsExactly("loves", " Mary", "very much");
    });
  }

  @Test
  public void testExample1WithComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right! *>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testExample1WithNestedComment() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><* Yeah, right<*actually...*>!*>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testExample1WithCData() {
    String texMECS = "<s|<a|John <b|loves|a> Mary|b><#CDATA<some cdata>#CDATA>|s>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[s}[a}John [b}loves{a] Mary{b]{s]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testSelfOverlappingElements() {
    String texMECS = "<e~1|Lorem <e~2|Ipsum |e~1>Dolor...|e~2>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[e~1}Lorem [e~2}Ipsum {e~1]Dolor...{e~2]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testTagSets() {
    String texMECS = "<|choice||<option|A|option><option|B|option>||choice|>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[choice}[option}A{option][option}B{option]{choice]");
      assertThat(document).isNotNull();
    });
  }

  //  @Ignore
  @Test
  public void testVirtualElement() {
    String texMECS = "<real|<e=e1|Reality|e>|real><virtual|<^e^e1>|virtual>";
    store.runInTransaction(() -> {
//      DocumentWrapper document = testTexMECS(texMECS, "[real}[e=e1}Reality{e=e1]{real][virtual}[e}Reality{e]{virtual]");
      DocumentWrapper document = testTexMECS(texMECS, "[real}[e}Reality{e]{real][virtual}[e}Reality{e]{virtual]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testMultipleRoots() {
    String texMECS = "<a|A|a><a|A|a><a|A|a><a|A|a><a|A|a>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[a}A{a][a}A{a][a}A{a][a}A{a][a}A{a]");
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testDominance() {
    String texMECS = "<l|This is <i|<b|very|b>|i> important|l>";
    store.runInTransaction(() -> {
      DocumentWrapper document = testTexMECS(texMECS, "[l}This is [i}[b}very{b]{i] important{l]");
      assertThat(document).isNotNull();
      List<MarkupWrapper> markupList = document.getMarkupStream().collect(toList());
      MarkupWrapper markupI = markupList.get(1);
      assertThat(markupI.getExtendedTag()).isEqualTo("i");

      MarkupWrapper markupB = markupList.get(2);
      assertThat(markupB.getExtendedTag()).isEqualTo("b");
      assertThat(markupI.getDominatedMarkup().get().getId()).isEqualTo(markupB.getId());
      assertThat(markupB.getDominatingMarkup().get().getId()).isEqualTo(markupI.getId());
    });
  }

  @Test
  public void testSyntaxError1() {
    String texMECS = "<tag|opening, but not closing";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("Some markup was not closed: <tag|");
      }
    });
  }

  @Test
  public void testSyntaxError2() {
    String texMECS = "no opening tag|bla>";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("Closing tag |bla> found, which has no corresponding earlier opening tag.");
      }
    });
  }

  @Test
  public void testSyntaxError3() {
    String texMECS = "<^v^v12>";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("idref 'v12' not found: No <v@v12| tag found that this virtual element refers to.");
      }
    });
  }

  @Test
  public void testSyntaxErrorSuspendWithoutResume() {
    String texMECS = "<tag|Lorem ipsum|-tag> dolores rosetta|tag>";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("Closing tag |tag> found, which has no corresponding earlier opening tag.");
      }
    });
  }

  @Test
  public void testSyntaxErrorResumeWithoutSuspend() {
    String texMECS = "<tag|Lorem ipsum <+tag|dolores rosetta|tag>";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("Resuming tag <+tag| found, which has no corresponding earlier suspending tag |-tag>.");
      }
    });
  }

  @Test
  public void testDuplicateIdError() {
    String texMECS = "<tag@t1|Lorem ipsum <b@t1|Dolores|b> dulcetto.|tag>";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("id 't1' was already used in markup <tag@t1|.");
      }
    });
  }

  @Test
  public void testSyntaxError4() throws IOException {
    String pathname = "data/texmecs/acrostic-syntax-error.texmecs";
    String texMECS = FileUtils.readFileToString(new File(pathname), StandardCharsets.UTF_8);
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = testTexMECS(texMECS, "whatever");
        fail();
      } catch (TexMECSSyntaxError se) {
        LOG.warn(se.getMessage());
        assertThat(se.getMessage()).contains("Parsing errors");
      }
    });
  }

  private DocumentWrapper testTexMECS(String texMECS, String expectedLMNL) {
    printTokens(texMECS);

    LOG.info("parsing {}", texMECS);
    TexMECSImporter importer = new TexMECSImporter(store);
    DocumentWrapper doc = importer.importTexMECS(texMECS);
    LMNLExporter ex = new LMNLExporter(store);
    String lmnl = ex.toLMNL(doc);
    LOG.info("lmnl={}", lmnl);
    assertThat(lmnl).isEqualTo(expectedLMNL);

    return doc;
  }

  protected void printTokens(String input) {
    System.out.println("TexMECS:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
    System.out.println("--------------------------------------------------------------------------------");
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    TexMECSLexer lexer = new TexMECSLexer(inputStream);
    String table = AntlrUtils.makeTokenTable(lexer);
    LOG.info("\nTokens:\n{}\n", table);
  }

}
