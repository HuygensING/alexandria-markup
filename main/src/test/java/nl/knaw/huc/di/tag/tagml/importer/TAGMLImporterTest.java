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
import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huygens.alexandria.storage.dto.TAGDocumentAssert.*;
import static org.junit.Assert.fail;

public class TAGMLImporterTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporterTest.class);

  @Test
  public void testCMLHTS18() {
    String tagML = "[tagml>\n" +
        "[page>\n" +
        "[p>\n" +
        "[line>1st. Voice from the Springs<line]\n" +
        "[line>Thrice three hundred thousand years<line]\n" +
        "[line>We had been stained with bitter blood<line]\n" +
        "<p]\n" +
        "<page]\n" +
        "[page>\n" +
        "[p>\n" +
        "[line>And had ran mute 'mid shrieks of slaugter<line]\n" +
        "[line>Thro' a city and a multitude<line]\n" +
        "<p]\n" +
        "<page]\n" +
        "<tagml]\n";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testCMLHTS19() {
    String tagML = "[tagml>\n" +
        "[text|+A,+B>\n" +
        "[page|A>\n" +
        "[p|B>\n" +
        "[line>1st. Voice from the Springs<line]\n" +
        "[line>Thrice three hundred thousand years<line]\n" +
        "[line>We had been stained with bitter blood<line]\n" +
        "<page|A]\n" +
        "[page|A>\n" +
        "[line>And had ran mute 'mid shrieks of slaugter\\[sic]<line]\n" +
        "[line>Thro' a city & a multitude<line]\n" +
        "<p|B]\n" +
        "<page|A]\n" +
        "<text|A,B]\n" +
        "<tagml]\n";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testCMLHTS20() {
    String tagML = "[tagml>\n" +
        "[text|+A,+B,+C>\n" +
        "[page|A>\n" +
        "[p|B>\n" +
        "[p|C>\n" +
        "[line>1st. Voice from the Springs<line]\n" +
        "[line>Thrice three hundred thousand years<line]\n" +
        "[line>We had been stained with bitter blood<line]\n" +
        "<p|C]\n" +
        "<page|A]\n" +
        "[page|A>\n" +
        "[p|C>\n" +
        "[line>And had ran mute 'mid shrieks of slaugter<line]\n" +
        "[line>Thro' a city and a multitude<line]\n" +
        "<p|B]\n" +
        "<p|C]\n" +
        "<page|A]\n" +
        "<text|A,B,C]\n" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
    });
  }

  @Test
  public void testSimpleTAGML() {
    String tagML = "[line>The rain in Spain falls mainly on the plain.<line]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(textNodeSketch("The rain in Spain falls mainly on the plain."));
      assertThat(document).hasMarkupMatching(markupSketch("line"));

      List<TAGTextNode> tagTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(tagTextNodes).hasSize(1);

      TAGTextNode textNode = tagTextNodes.get(0);
      assertThat(textNode).hasText("The rain in Spain falls mainly on the plain.");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").contains("line");
    });
  }

  @Test
  public void testCharacterEscapingInRegularText() {
    String tagML = "In regular text, \\<, \\[ and \\\\ need to be escaped, |, !, \", and ' don't.";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(textNodeSketch("In regular text, <, [ and \\ need to be escaped, |, !, \", and ' don't."));

      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(1);
    });
  }

  @Test
  public void testCharacterEscapingInTextVariation() {
    String tagML = "[t>In text in between textVariation tags, <|\\<, \\[, \\| and \\\\ need to be escaped|!, \" and ' don't|>.<t]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("In text in between textVariation tags, "),
//          textDivergenceSketch(),
          textNodeSketch("<, [, | and \\ need to be escaped"),
          textNodeSketch("!, \" and ' don't"),
//          textConvergenceSketch(),
          textNodeSketch(".")
      );

      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(4);
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
    String expectedErrors = "line 1:15 : Close tag <line] found without corresponding open tag.";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testDifferentOpenAndCloseTAGSThrowsTAGMLSyntaxError() {
    String tagML = "[line>The Spanish rain.<paragraph]";
    String expectedErrors = "line 1:25 : Close tag <paragraph] found without corresponding open tag.\n" +
        "Missing close tag(s) for: [line>";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testNamelessTagsThrowsTAGMLSyntaxError() {
    String tagML = "[>The Spanish rain.<]";
    String expectedErrors = "syntax error: line 1:1 no viable alternative at input '[>'\n" +
        "syntax error: line 1:20 mismatched input ']' expecting {IMO_Prefix, IMO_Name, IMC_Prefix, IMC_Name}\n" +
        "line 1:20 : Nameless markup is not allowed here.";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testOverlap() {
    String tagML = "[x|+la,+lb>[a|la>J'onn J'onzz [b|lb>likes<a|la] Oreos<b|lb]<x|la,lb]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).hasMarkupWithTag("a").inLayer("la").withTextNodesWithText("J'onn J'onzz ", "likes");
      assertThat(document).hasMarkupWithTag("b").inLayer("lb").withTextNodesWithText("likes", " Oreos");
    });
  }

  @Test
  public void testTAGML2() {
    String tagML = "[line|+a,+b>[a|a>The rain in [country>Spain<country] [b|b>falls<a|a] mainly on the plain.<b|b]<line|a,b]";
//    String tagML = "[line|+A,+B,+N>[a|A>[name|N>Trump<name|N] [b|B>likes<a|A] [name|N>Kim<name|N]<b|B]<line|A,B,N]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
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

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(5);

      TAGTextNode textNode = textNodes.get(1);
      assertThat(textNode).hasText("Spain");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(3);
      assertThat(markupForTextNode).extracting("tag").containsExactly("country", "a", "line");

      List<String> textSegments = document.getDTO().textGraph
          .getTextNodeIdStream()
          .map(id -> store.getTextNodeDTO(id))
          .map(t -> t.getText())
          .collect(toList());

      assertThat(textSegments).containsExactly("The rain in ", "Spain", " ", "falls", " mainly on the plain.");

    });
  }

  @Test
  public void testCommentsAreIgnored() {
    String tagML = "[! before !][a>Ah![! within !]<a][! after !]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("Ah!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("a")
      );

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(1);

      TAGTextNode textNode = textNodes.get(0);
      assertThat(textNode).hasText("Ah!");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").containsExactly("a");
    });
  }

  @Test
  public void testNamespace() {
    String tagML = "[!ns a http://tag.com/a][a:a>Ah!<a:a]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("Ah!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("a:a")
      );

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(1);

      TAGTextNode textNode = textNodes.get(0);
      assertThat(textNode).hasText("Ah!");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").containsExactly("a:a");
    });
  }

  @Test
  public void testMultipleNamespaces() {
    String tagML = "[!ns a http://tag.com/a]\n[!ns b http://tag.com/b]\n[a:a>[b:b>Ah!<b:b]<a:a]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("Ah!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("a:a"),
          markupSketch("b:b")
      );

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(1);

      TAGTextNode textNode = textNodes.get(0);
      assertThat(textNode).hasText("Ah!");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(2);
      assertThat(markupForTextNode).extracting("tag").containsExactly("b:b", "a:a");
    });
  }

  @Test
  public void testTextVariation() {
    String tagML = "[t>This is a <|lame|dope|> test!<t]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
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

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(4);

      TAGTextNode textNode = textNodes.get(0);
      assertThat(textNode).hasText("This is a ");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(1);
      assertThat(markupForTextNode).extracting("tag").containsExactly("t");
    });
  }

  @Test
  public void testMilestone() {
    String tagML = "[t>This is a [space chars=10] test!<t]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
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

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(3);

      TAGTextNode textNode = textNodes.get(1);
      assertThat(textNode).hasText("");

      final List<TAGMarkup> markupForTextNode = document.getMarkupStreamForTextNode(textNode).collect(toList());
      assertThat(markupForTextNode).hasSize(2);
      assertThat(markupForTextNode).extracting("tag").containsExactly("t", "space");
    });
  }

  @Test
  public void testDiscontinuity() {
    String tagML = "[x>[t>This is<-t], he said, [+t>a test!<t]<x]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("This is"),
          textNodeSketch(", he said, "),
          textNodeSketch("a test!")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );

      List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
      assertThat(textNodes).hasSize(3);

      List<TAGMarkup> markups = document.getMarkupStream().collect(toList());
      assertThat(markups).hasSize(3);

      TAGMarkup t = markups.get(1);
      assertThat(t).hasTag("t");
      List<TAGTextNode> tTAGTextNodes = t.getTextNodeStream().collect(toList());
      assertThat(tTAGTextNodes).extracting("text").containsExactly("This is", "a test!");
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
    String tagML = "[x>[markup>Cookie <-markup][+markup> Monster<markup]<x]";
    String expectedErrors = "line 1:28 : There is no text between this resume tag: [+markup> and its corresponding suspend tag: <-markup]. This is not allowed.\n" +
        "parsing aborted!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Ignore("TODO: Handle richText annotations first")
  @Test
  public void testResumeInInnerDocumentLeadsToError() {
    String tagML = "[text> [q>Hello my name is " +
        "[gloss addition=[>that's<-q] [qualifier>mrs.<qualifier] to you<]>" +
        "Doubtfire, [+q>how do you do?<q]<gloss]<text] ";
    String expectedErrors = "some error";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  //  @Ignore
  @Test
  public void testAcceptedMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is <|a failing|an excellent|><x] test<t]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();

      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).extracting("text").containsExactly(
          "This ",
          "is ",
//          "", // <|
          "a failing",
          "an excellent",
//          "", // |>
          " test"
      );

      List<TAGMarkup> TAGMarkups = document.getMarkupStream().collect(toList());
      assertThat(TAGMarkups)
          .extracting("tag")
          .containsExactly("t", "x", ":branches", ":branch", ":branch");

      TAGMarkup t = TAGMarkups.get(0);
      assertThat(t.getTag()).isEqualTo("t");
      List<TAGTextNode> tTAGTextNodes = t.getTextNodeStream().collect(toList());
      assertThat(tTAGTextNodes).hasSize(5);

      TAGMarkup x = TAGMarkups.get(1);
      assertThat(x.getTag()).isEqualTo("x");
      List<TAGTextNode> xTAGTextNodes = x.getTextNodeStream().collect(toList());
      assertThat(xTAGTextNodes)
          .extracting("text")
          .containsExactly("is ", "a failing", "an excellent");

    });
  }

  @Test
  public void testIllegalMarkupDifferenceInNonLinearity() {
    String tagML = "[t>This [x>is <|a [y>failing|an<x] [y>excellent|> test<y]<t]";
    String expectedErrors = "line 1:29 : Markup [y> opened in branch 1 must be closed before starting a new branch.\n" +
        "parsing aborted!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testOpenMarkupInNonLinearAnnotatedTextThrowsError() {
    String tagML = "[t>[l>I'm <|done.<l][l>|ready.|finished.|> Let's go!.<l]<t]";
    String expectedErrors = "line 1:19 : Markup [l> opened before branch 1, should not be closed in a branch.\n" +
        "parsing aborted!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testIncorrectOverlapNonLinearityCombination() {
    String tagML = "[text|+w>It is a truth universally acknowledged that every " +
        "<|" +
        "[add>young [b|w>woman<add]" +
        "|" +
        "[del>rich<del]" +
        "|>" +
        " man<b|w] is in need of a maid.<text] ";
    String expectedErrors = "line 1:88 : Markup [b|w> opened in branch 1 must be closed before starting a new branch.\n" +
        "parsing aborted!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

//  @Test
//  public void testCorrectOverlapNonLinearityCombination1() {
//    String tagML = "[text>It is a truth universally acknowledged that every " +
//        "<|[add>young [b>woman<b]<add]" +
//        "|[b>[del>rich<del]|>" +
//        " man <b] is in need of a maid.<text]";
//    store.runInTransaction(() -> {
//      TAGDocument document = parseTAGML(tagML);
//      assertThat(document).isNotNull();
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
  public void testCorrectOverlapNonLinearityCombination2() {
    String tagML = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<b]<add]" +
        "|[b>[del>rich<del]<b]|>" +
        " [b>man<b] is in need of a maid.<text]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
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

  @Test
  public void testIncorrectDiscontinuityNonLinearityCombination() {
    String tagML = "[x>[q>and what is the use of a " +
        "<|[del>book,<del]<-q]" +
        "| [add>thought Alice<add]|>" +
        " [+q>without pictures or conversation?<q]<x]";
    String expectedErrors = "line 1:50 : Markup [q> opened before branch 1, should not be closed in a branch.\n" +
        "parsing aborted!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  @Test
  public void testCorrectDiscontinuityNonLinearityCombination() {
    String tagML = "[x>[q>and what is the use of a book" +
        "<|[del>, really,<del]" +
        "|[add|+A>\"<-q] thought Alice [+q>\"<add|A]|>" +
        "without pictures or conversation?<q]<x]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("and what is the use of a book"),
          textNodeSketch(", really,"),
          textNodeSketch("\""),
          textNodeSketch(" thought Alice "),
          textNodeSketch("\""),
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
      TAGDocument document = parseTAGML(tagML);
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
      TAGDocument document = parseTAGML(tagML);
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
      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(3);

      TAGTextNode always = TAGTextNodes.get(1);
      List<TAGMarkup> TAGMarkups = document.getMarkupStreamForTextNode(always).collect(toList());
      assertThat(TAGMarkups).hasSize(2);

      TAGMarkup del = TAGMarkups.get(0);
      assertThat(del).hasTag("del");
      assertThat(del).isOptional();
    });
  }

  @Test
  public void testContainmentIsDefault() {
    String tagML = "[tag>word1 [phr>word2 [phr>word3<phr] word4<phr] word5<tag]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
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
      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(5);

      List<TAGMarkup> TAGMarkups = document.getMarkupStream().collect(toList());
      TAGMarkup phr0 = TAGMarkups.get(1);
      assertThat(phr0).hasTag("phr");
      List<TAGTextNode> textNodes0 = phr0.getTextNodeStream().collect(toList());
      assertThat(textNodes0).extracting("text")
          .containsExactlyInAnyOrder("word2 ", "word3", " word4");

      TAGMarkup phr1 = TAGMarkups.get(2);
      assertThat(phr1).hasTag("phr");
      List<TAGTextNode> textNodes1 = phr1.getTextNodeStream().collect(toList());
      assertThat(textNodes1).extracting("text")
          .containsExactly("word3");
    });
  }

  @Test
  public void testUseLayersForSelfOverlap() {
    String tagML = "[x|+p1,+p2>word1 [phr|p1>word2 [phr|p2>word3<phr|p1] word4<phr|p2] word5<x|p1,p2]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
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
      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(5);

      List<TAGMarkup> TAGMarkups = document.getMarkupStream().collect(toList());
      TAGMarkup phr0 = TAGMarkups.get(1);
      assertThat(phr0).hasTag("phr");
      List<TAGTextNode> textNodes0 = document.getTextNodeStreamForMarkupInLayer(phr0, "p1").collect(toList());
      assertThat(textNodes0).extracting("text")
          .containsExactly("word2 ", "word3");

      TAGMarkup phr1 = TAGMarkups.get(2);
      assertThat(phr0).hasTag("phr");
      List<TAGTextNode> textNodes1 = document.getTextNodeStreamForMarkupInLayer(phr1, "p2").collect(toList());
      assertThat(textNodes1).extracting("text")
          .containsExactly("word3", " word4");
    });
  }

  @Test
  public void testStringAnnotations() {
    String tagML = "[markup a='string' b=\"string\">text<markup]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("text")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("markup")
      );
      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(1);

      List<TAGMarkup> TAGMarkups = document.getMarkupStream().collect(toList());
      TAGMarkup markup = TAGMarkups.get(0);
      List<TAGAnnotation> TAGAnnotations = markup.getAnnotationStream().collect(toList());
      assertThat(TAGAnnotations).hasSize(2);

      TAGAnnotation annotationA = TAGAnnotations.get(0);
      assertThat(annotationA).hasTag("a");

      TAGAnnotation annotationB = TAGAnnotations.get(1);
      assertThat(annotationB).hasTag("b");
    });
  }

  @Test
  public void testListAnnotations() {
    String tagML = "[markup primes=[1,2,3,5,7,11]>text<markup]";
    store.runInTransaction(() -> {
      TAGDocument document = parseTAGML(tagML);
      assertThat(document).isNotNull();
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("text")
      );
      assertThat(document).hasMarkupMatching(
          markupSketch("markup")
      );
      List<TAGTextNode> TAGTextNodes = document.getTextNodeStream().collect(toList());
      assertThat(TAGTextNodes).hasSize(1);

      List<TAGMarkup> TAGMarkups = document.getMarkupStream().collect(toList());
      TAGMarkup markup = TAGMarkups.get(0);
      List<TAGAnnotation> TAGAnnotations = markup.getAnnotationStream().collect(toList());
      assertThat(TAGAnnotations).hasSize(1);

      TAGAnnotation annotationPrimes = TAGAnnotations.get(0);
      assertThat(annotationPrimes).hasTag("primes");
//      List<TAGTextNode> annotationTextNodes = annotationPrimes.getDocument().getTextNodeStream().collect(toList());
//      assertThat(annotationTextNodes).hasSize(1);
//      assertThat(annotationTextNodes).extracting("text").containsExactly("[1,2,3,5,7,11]");
    });
  }

  @Test
  public void testUnclosedTextVariationThrowsSyntaxError() {
    String tagML = "[t>This is <|good|bad.<t]";
    String expectedErrors = "syntax error: line 1:25 extraneous input '<EOF>' expecting {ITV_EndTextVariation, TextVariationSeparator}\n" +
        "line 1:24 : Markup [t> opened before branch 2, should not be closed in a branch.\n" +
        "parsing aborted!";
    parseWithExpectedErrors(tagML, expectedErrors);
  }

  // private methods

  private void parseWithExpectedErrors(final String tagML, final String expectedErrors) {
    store.runInTransaction(() -> {
      try {
        TAGDocument document = parseTAGML(tagML);
        logDocumentGraph(document, tagML);
        fail("TAGMLSyntaxError expected!");
      } catch (TAGMLSyntaxError e) {
        assertThat(e).hasMessage("Parsing errors:\n" +
            expectedErrors);
      }
    });
  }

  private TAGDocument parseTAGML(final String tagML) {
//    LOG.info("TAGML=\n{}\n", tagML);
    printTokens(tagML);
    TAGDocument document = new TAGMLImporter(store).importTAGML(tagML);
    logDocumentGraph(document, tagML);
    TAGMLExporter tagmlExporter = new TAGMLExporter(store);
    String tagml = tagmlExporter.asTAGML(document);
    LOG.info("\n\nTAGML:\n{}\n", tagml);
    return document;
  }

}
