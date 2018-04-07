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
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert.*;
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
            "line 1:51 : Closing tag <l] found without corresponding open tag.\n" +
            "Unclosed TAGML tag(s): [line>");
      }
    });
  }

  @Test
  public void testOverlap() {
    String tagML = "[a>J'onn J'onzz [b>likes<a] Oreos<b]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).hasMarkupWithTag("a").withTextNodesWithText("J'onn J'onzz ", "likes");
      assertThat(document).hasMarkupWithTag("b").withTextNodesWithText("likes", " Oreos");
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

  @Test
  public void testTextVariation() {
    String tagML = "[t>This is a <|lame|dope|> test!<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("This is a "),
          textNodeSketch("lame"),
          textNodeSketch("dope"),
          textNodeSketch(" test!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(4);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(0);
      assertThat(textNodeWrapper).hasText("This is a ");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").containsExactly("t");
    });
  }

  @Test
  public void testMilestone() {
    String tagML = "[t>This is a [space chars=10] test!<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("This is a "),
          textNodeSketch(""),
          textNodeSketch(" test!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t"),
          markupSketch("space")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(3);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(1);
      assertThat(textNodeWrapper).hasText("");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(2);
      assertThat(markupForTextNode).extracting("tag").containsExactly("t", "space");
    });
  }

  @Ignore
  @Test
  public void testDiscontinuity() {
    String tagML = "[t>This is<-t], he said, [+t>a test!<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("This is"),
          textNodeSketch(", he said, "),
          textNodeSketch("a test!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(3);

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      assertThat(markupWrappers).hasSize(1);

      MarkupWrapper t = markupWrappers.get(0);
      List<TextNodeWrapper> tTextNodeWrappers = t.getTextNodeStream().collect(toList());
      assertThat(tTextNodeWrappers).hasSize(2);

      TextNodeWrapper t0 = tTextNodeWrappers.get(0);
      assertThat(t0).hasText("This is ");

      TextNodeWrapper t1 = tTextNodeWrappers.get(1);
      assertThat(t1).hasText("a test!");
    });
  }

  @Ignore
  @Test
  public void testAcceptedMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is |>a<x] [y>failing|an<x] [y>excellent<| test<y]<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("This "),
          textNodeSketch("is "),
          textNodeSketch("a"),
          textNodeSketch(" "),
          textNodeSketch("failing"),
          textNodeSketch("an"),
          textNodeSketch("excellent"),
          textNodeSketch(" test")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t"),
          markupSketch("x"),
          markupSketch("y")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(8);

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      assertThat(markupWrappers).hasSize(3);

      MarkupWrapper t = markupWrappers.get(0);
      List<TextNodeWrapper> tTextNodeWrappers = t.getTextNodeStream().collect(toList());
      assertThat(tTextNodeWrappers).hasSize(8);
    });
  }

  @Ignore
  @Test
  public void testIllegalMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is |>a [y>failing|an<x] [y>excellent<| test<y]<t]";
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = parseTAGML(tagML);
        fail();
      } catch (TAGMLSyntaxError e) {
        assertThat(e).hasMessage("markup [x> not closed!");
      }
    });
  }

  @Test
  public void testEscapeSpecialCharactersInTextVariation() {
    String tagML = "[t>bla <|\\||!|> bla<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("bla "),
          textNodeSketch("\\|"),
          textNodeSketch("!"),
          textNodeSketch(" bla")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );
    });
  }

  @Test
  public void testOptionalMarkup() {
    String tagML = "[t>this is [?del>always<?del] optional<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("this is "),
          textNodeSketch("always"),
          textNodeSketch(" optional")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t"),
          optionalMarkupSketch("del")
      );
      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(3);

      TextNodeWrapper always = textNodeWrappers.get(1);
      List<MarkupWrapper> markupWrappers = document.getMarkupStreamForTextNode(always).collect(toList());
      assertThat(markupWrappers).hasSize(2);

      MarkupWrapper del = markupWrappers.get(1);
      assertThat(del).isOptional();
    });
  }

  @Test
  public void testContainmentIsDefault() {
    String tagML = "word1 [phr>word2 [phr>word3<phr] word4<phr] word5";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("word1 "),
          textNodeSketch("word2 "),
          textNodeSketch("word3"),
          textNodeSketch(" word4"),
          textNodeSketch(" word5")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("phr"),
          markupSketch("phr")
      );
      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(5);

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      MarkupWrapper phr0 = markupWrappers.get(0);
      List<TextNodeWrapper> textNodes0 = phr0.getTextNodeStream().collect(toList());
      assertThat(textNodes0).extracting("text")
          .containsExactly("word2 ", "word3", " word4");

      MarkupWrapper phr1 = markupWrappers.get(1);
      List<TextNodeWrapper> textNodes1 = phr1.getTextNodeStream().collect(toList());
      assertThat(textNodes1).extracting("text")
          .containsExactly("word3");
    });
  }

  @Test
  public void testUseIdForSelfOverlap() {
    String tagML = "word1 [phr~1>word2 [phr~2>word3<phr~1] word4<phr~2] word5";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("word1 "),
          textNodeSketch("word2 "),
          textNodeSketch("word3"),
          textNodeSketch(" word4"),
          textNodeSketch(" word5")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("phr"),
          markupSketch("phr")
      );
      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(5);

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      MarkupWrapper phr0 = markupWrappers.get(0);
      List<TextNodeWrapper> textNodes0 = phr0.getTextNodeStream().collect(toList());
      assertThat(textNodes0).extracting("text")
          .containsExactly("word2 ", "word3");

      MarkupWrapper phr1 = markupWrappers.get(1);
      List<TextNodeWrapper> textNodes1 = phr1.getTextNodeStream().collect(toList());
      assertThat(textNodes1).extracting("text")
          .containsExactly("word3", " word4");
    });
  }

  @Test
  public void testStringAnnotations() {
    String tagML = "[markup a='string' b=\"string\">text<markup]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("text")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("markup")
      );
      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      MarkupWrapper markup = markupWrappers.get(0);
      List<AnnotationWrapper> annotationWrappers = markup.getAnnotationStream().collect(toList());
      assertThat(annotationWrappers).hasSize(2);

      AnnotationWrapper annotationA = annotationWrappers.get(0);
      assertThat(annotationA).hasTag("a");

      AnnotationWrapper annotationB = annotationWrappers.get(1);
      assertThat(annotationB).hasTag("b");
    });
  }

  @Test
  public void testListAnnotations() {
    String tagML = "[markup primes=[1,2,3,5,7,11]>text<markup]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("text")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("markup")
      );
      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      MarkupWrapper markup = markupWrappers.get(0);
      List<AnnotationWrapper> annotationWrappers = markup.getAnnotationStream().collect(toList());
      assertThat(annotationWrappers).hasSize(1);

      AnnotationWrapper annotationPrimes = annotationWrappers.get(0);
      assertThat(annotationPrimes).hasTag("primes");
      List<TextNodeWrapper> annotationTextNodes = annotationPrimes.getDocument().getTextNodeStream().collect(toList());
      assertThat(annotationTextNodes).hasSize(1);
      assertThat(annotationTextNodes).extracting("text").containsExactly("[1,2,3,5,7,11]");
    });
  }

  // private
  private DocumentWrapper parseTAGML(final String tagML) {
//    LOG.info("TAGML=\n{}\n", tagML);
    printTokens(tagML);
    DocumentWrapper documentWrapper = new TAGMLImporter(store).importTAGML(tagML);
    LMNLExporter lmnlExporter = new LMNLExporter(store);
    String lmnl = lmnlExporter.toLMNL(documentWrapper);
    LOG.info("\n\nLMNL:\n{}\n", lmnl);
    return documentWrapper;
  }

}
