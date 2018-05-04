package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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
import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter;
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
  public void testCharacterEscapingInRegularText() {
    String tagML = "In regular text, \\<, \\[ and \\\\ need to be escaped, |, !, \", and ' don't.";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(textNodeSketch("In regular text, <, [ and \\ need to be escaped, |, !, \", and ' don't."));

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);
    });
  }

  @Test
  public void testCharacterEscapingInTextVariation() {
    String tagML = "In text in between textVariation tags, <|\\<, \\[, \\| and \\\\ need to be escaped|!, \" and ' don't|>.";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("In text in between textVariation tags, "),
//          textDivergenceSketch(),
          textNodeSketch("<, [, | and \\ need to be escaped"),
          textNodeSketch("!, \" and ' don't"),
//          textConvergenceSketch(),
          textNodeSketch(".")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(6);
    });
  }

  @Test
  public void testMissingEndTagThrowsTAGMLSyntaxError() {
    String tagML = "[line>The rain";
    String expectedErrors = "Missing close tag(s) for: [line>";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testMissingOpenTagThrowsTAGMLSyntaxError() {
    String tagML = "on the plain.<line]";
    String expectedErrors = "line 1:14 : Close tag <line] found without corresponding open tag.";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testDifferentOpenAndCloseTAGSThrowsTAGMLSyntaxError() {
    String tagML = "[line>The Spanish rain.<paragraph]";
    String expectedErrors = "line 1:24 : Close tag <paragraph] found without corresponding open tag.\n" +
        "Missing close tag(s) for: [line>";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testNamelessTagsThrowsTAGMLSyntaxError() {
    String tagML = "[>The Spanish rain.<]";
    String expectedErrors = "syntax error: line 1:1 no viable alternative at input '[>'\n" +
        "syntax error: line 1:20 mismatched input ']' expecting {IMO_Prefix, IMO_NameOpenMarkup, IMC_Prefix, IMC_NameCloseMarkup}\n" +
        "line 1:19 : Nameless markup is not allowed here.";
    parseWithExpectedErrors(tagML, expectedErrors);
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
  public void testNamespace() {
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
  public void testMultipleNamespaces() {
    String tagML = "[!ns a http://tag.com/a]\n[!ns b http://tag.com/b]\n[a:a>[b:b>Ah!<a:a]<b:b]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("Ah!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("a:a"),
          markupSketch("b:b")
      );

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(1);

      TextNodeWrapper textNodeWrapper = textNodeWrappers.get(0);
      assertThat(textNodeWrapper).hasText("Ah!");

      final List<MarkupWrapper> markupForTextNode = document.getMarkupStreamForTextNode(textNodeWrapper).collect(toList());
      assertThat(markupForTextNode).hasSize(2);
      assertThat(markupForTextNode).extracting("tag").containsExactly("a:a", "b:b");
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
      assertThat(textNodeWrappers).hasSize(6);

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
      assertThat(tTextNodeWrappers).extracting("text").containsExactly("This is", "a test!");
    });
  }

  @Test
  public void testUnclosedDiscontinuityLeadsToError() {
    String tagML = "[t>This is<-t], he said...";
    String expectedErrors = "Some suspended markup was not resumed: <-t]";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testFalseDiscontinuityLeadsToError() {
    // There must be text between a pause and a resume tag, so the following example is not allowed:
    String tagML = "[markup>Cookie <-markup][+markup> Monster<markup]";
    String expectedErrors = "line 1:24 : There is no text between this resume tag [+markup> and it's corresponding suspend tag <-markup]. This is not allowed.";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Ignore("TODO: Handle richText annotations first")
  @Test
  public void testResumeInInnerDocumentLeadsToError() {
    String tagML = "[text> [q>Hello my name is " +
        "[gloss addition=[>that's<-q] [qualifier>mrs.<qualifier] to you<]>" +
        "Doubtfire, [+q>how do you do?<q]<gloss]<text] ";
    String expectedErrors = "";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  //  @Ignore
  @Test
  public void testAcceptedMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is <|a<x] [y>failing|an<x] [y>excellent|> test<y]<t]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();

      List<TextNodeWrapper> textNodeWrappers = document.getTextNodeStream().collect(toList());
      assertThat(textNodeWrappers).hasSize(8);
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

      List<MarkupWrapper> markupWrappers = document.getMarkupStream().collect(toList());
      assertThat(markupWrappers).hasSize(3);
      assertThat(document).hasMarkupMatching(
          markupSketch("t"),
          markupSketch("x"),
          markupSketch("y")
      );

      MarkupWrapper t = markupWrappers.get(0);
      assertThat(t.getTag()).isEqualTo("t");
      List<TextNodeWrapper> tTextNodeWrappers = t.getTextNodeStream().collect(toList());
      assertThat(tTextNodeWrappers).hasSize(8);

      MarkupWrapper x = markupWrappers.get(1);
      assertThat(x.getTag()).isEqualTo("x");
      List<TextNodeWrapper> xTextNodeWrappers = x.getTextNodeStream().collect(toList());
      assertThat(xTextNodeWrappers).hasSize(3);
      assertThat(xTextNodeWrappers).extracting("text").containsExactly("is", "a", "an");

      MarkupWrapper y = markupWrappers.get(2);
      assertThat(y.getTag()).isEqualTo("y");
      List<TextNodeWrapper> yTextNodeWrappers = x.getTextNodeStream().collect(toList());
      assertThat(xTextNodeWrappers).hasSize(3);
      assertThat(xTextNodeWrappers).extracting("text").containsExactly("failing", "excellent", "test");

    });
  }

  @Ignore
  @Test
  public void testIllegalMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is <|a [y>failing|an<x] [y>excellent|> test<y]<t]";
    String expectedErrors = "markup [x> not closed!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Ignore("passed where it should fail.")
  @Test
  public void testIncorrectOverlapNonLinearityCombination() {
    String tagML = "[text>It is a truth universally acknowledged that every <|[add>young [b>woman<add]|[del>rich<del]|> man <b] is in need of a maid.<text] ";
    String expectedErrors = "some error about the [b> tags";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testCorrectOverlapNonLinearityCombination1() {
    String tagML = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<add]<b]" +
        "|[b>[del>rich<del]|>" +
        " man <b] is in need of a maid.<text]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("It is a truth universally acknowledged that every "),
          textNodeSketch("young "),
          textNodeSketch("woman"),
          textNodeSketch("rich"),
          textNodeSketch(" man "),
          textNodeSketch(" is in need of a maid.")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("text"),
          markupSketch("add"),
          markupSketch("del"),
          markupSketch("b")
      );
    });
  }

  @Test
  public void testCorrectOverlapNonLinearityCombination2() {
    String tagML = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<add]<b]" +
        "|[b>[del>rich<del]<b]|>" +
        " [b>man<b] is in need of a maid.<text]";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("It is a truth universally acknowledged that every "),
          textNodeSketch("young "),
          textNodeSketch("woman"),
          textNodeSketch("man"),
          textNodeSketch(" is in need of a maid.")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("text"),
          markupSketch("add"),
          markupSketch("del"),
          markupSketch("b")
      );
    });
  }

  @Ignore("passes where it should fail")
  @Test
  public void testIncorrectDiscontinuityNonLinearityCombination() {
    String tagML = "[q>and what is the use of a " +
        "<|[del>book,<-q]<del]" +
        "| [add>thought Alice<add]|>" +
        " [+q>without pictures or conversation?<q]";
    String expectedErrors = "some error about the [q> tags, with branch mentioned";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testCorrectDiscontinuityNonLinearityCombination() {
    String tagML = "[q>and what is the use of a " +
        "<|[del>book,<del]" +
        "|<-q][add>thought Alice<add][+q>|>" +
        "without pictures or conversation?<q] ";
    store.runInTransaction(() -> {
      DocumentWrapper document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("and what is the use of a "),
          textNodeSketch("book,"),
          textNodeSketch("thought Alice"),
          textNodeSketch("without pictures or conversation?")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("q")
      );
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
          textNodeSketch("|"),
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
  public void testUseSuffixForSelfOverlap() {
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

  @Test
  public void testUnclosedTextVariationThrowsSyntaxError() {
    String tagML = "[t>This is <|good|bad.<t]";
    String expectedErrors = "syntax error: line 1:25 extraneous input '<EOF>' expecting {ITV_EndTextVariation, TextVariationSeparator}";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  // private methods

  private void parseWithExpectedErrors(final String tagML, final String expectedErrors) {
    store.runInTransaction(() -> {
      try {
        DocumentWrapper document = parseTAGML(tagML);
        fail("TAGMLSyntaxError expected!");
      } catch (TAGMLSyntaxError e) {
        assertThat(e).hasMessage("Parsing errors:\n" +
            expectedErrors);
      }
    });
  }

  private DocumentWrapper parseTAGML(final String tagML) {
//    LOG.info("TAGML=\n{}\n", tagML);
    printTokens(tagML);
    DocumentWrapper documentWrapper = new TAGMLImporter(store).importTAGML(tagML);
    TAGMLExporter tagmlExporter = new TAGMLExporter(store);
    String tagml = tagmlExporter.asTAGML(documentWrapper);
    LOG.info("\n\nTAGML:\n{}\n", tagml);
    return documentWrapper;
  }

}
