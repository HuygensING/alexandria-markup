package nl.knaw.huc.di.tag.tagml.importer;

/*-
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

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert.markupSketch;
import static nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert.textNodeSketch;
import static org.junit.Assert.fail;

public class TAGMLImporterTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporterTest.class);

  @Test
  public void testSimpleTAGML() {
    String tagML = "[line>The rain in Spain falls mainly on the plain.<line]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(textNodeSketch("The rain in Spain falls mainly on the plain."));
      assertThat(document).hasMarkupMatching(markupSketch("line"));

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(0);
      assertThat(textNodeWrapper).hasText("The rain in Spain falls mainly on the plain.");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").contains("line");
    });
  }

  @Test
  public void testTAGMLSyntaxError() {
    String tagML = "[line>The rain in Spain falls mainly on the plain.<l]";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = parseTAGML(tagML);
        fail("TAGMLSyntaxError expected!");
      } catch (TAGMLSyntaxError e) {
        assertThat(e).hasMessage("Parsing errors:\n" +
            "Closing tag {l] found without corresponding open tag.\n" +
            "Unclosed TAGML tag(s): [line>");
      }
    });
  }

  @Test
  public void testTAGML2() {
    String tagML = "[line>[a>The rain in [country>Spain<country] [b>falls<a] mainly on the plain.<b]<line]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("The rain in "),
          textNodeSketch("Spain"),
          textNodeSketch(" "),
          textNodeSketch("falls"),
          textNodeSketch(" mainly on the plain.")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("line"),
          markupSketch("a"),
          markupSketch("country"),
          markupSketch("b")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(5);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(1);
      assertThat(textNodeWrapper).hasText("Spain");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(3);
      assertThat(markupForTextNode).extracting("tag").containsExactly("line", "a", "country");
    });
  }

  @Test
  public void testCommentsAreIgnored() {
    String tagML = "[! before !][a>Ah![! within !]<a][! after !]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("Ah!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("a")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(0);
      assertThat(textNodeWrapper).hasText("Ah!");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").containsExactly("a");
    });
  }

  @Test
  public void testNamespaces() {
    String tagML = "[!ns a http://tag.com/a][a:a>Ah!<a:a]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("Ah!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("a:a")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(0);
      assertThat(textNodeWrapper).hasText("Ah!");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").containsExactly("a:a");
    });
  }

  // private
  private DocumentWrapper parseTAGML(final String tagML) {
//    LOG.info("TAGML=\n{}\n", tagML);
    printTokens(tagML);
    DocumentWrapper documentWrapper = new TAGMLImporter(store).importTAGML(tagML);
    LMNLExporter lmnlExporter = new LMNLExporter(store);
    String lmnl = lmnlExporter.toLMNL(documentWrapper);
    LOG.info("\nLMNL:\n{}\n", lmnl);
    return documentWrapper;
  }

}
