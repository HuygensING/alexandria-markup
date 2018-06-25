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
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huygens.alexandria.storage.dto.TAGDocumentAssert.*;

public class TAGMLParserTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLParserTest.class);
  private static final LMNLExporter LMNL_EXPORTER = new LMNLExporter(store);

  @Test // RD-131
  public void testSimpleTextWithRoot() {
    String input = "[tagml>simple text<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("simple text")
      );
      assertThat(document.getLayerNames()).containsExactly("");
    });
  }

  @Test // RD-132
  public void testTextWithMultipleLayersOfMarkup() {
    String input = "[text>[some|+L1>[all|+L2>word1<some|L1] word2<all|L2]<text]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("text"),
          markupSketch("some"),
          markupSketch("all")

      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("word1"),
          textNodeSketch(" word2")
      );
      assertThat(document.getLayerNames()).containsExactly("", "L1", "L2");
    });
  }

  @Test // RD-133
  public void testTextWithMultipleLayersAndDiscontinuity() {
    String input = "[tagml>" +
        "[pre|+L1,+L2>[q|L1>“Man,\"<-q|L1][s|L2> I cried, <s|L2][+q|L1>\"how ignorant art thou in thy pride of wisdom!”<q|L1]<pre|L1,L2]" +
        "― " +
        "[post|+L3>Mary Wollstonecraft Shelley, Frankenstein<post|L3]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("pre"),
          markupSketch("q"),
          markupSketch("s"),
          markupSketch("post")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("“Man,\""),
          textNodeSketch(" I cried, "),
          textNodeSketch("\"how ignorant art thou in thy pride of wisdom!”"),
          textNodeSketch("― "),
          textNodeSketch("Mary Wollstonecraft Shelley, Frankenstein")
      );
      assertThat(document.getLayerNames()).containsExactly(TAGML.DEFAULT_LAYER, "L1", "L2", "L3");

      List<TAGMarkup> markups = document.getMarkupStream().filter(m -> m.hasTag("q")).collect(toList());
      assertThat(markups).hasSize(1);
      final TAGMarkup q = markups.get(0);

      List<TAGTextNode> qTextNodes = document.getTextNodeStreamForMarkupInLayer(q, "L1").collect(toList());
      assertThat(qTextNodes).extracting("text")
          .containsExactly("“Man,\"", "\"how ignorant art thou in thy pride of wisdom!”");
    });
  }

  @Test // RD-134
  public void testTextWithMultipleLayersDiscontinuityAndNonLinearity() {
    String input = "[tagml>[pre|+L1,+L2>" +
        "[q|L1>“Man,\"<-q|L1][s|L2> I " +
        "<|cried|pleaded|>" +
        ", <s|L2][+q|L1>\"how ignorant art thou in thy pride of wisdom!”<q|L1]" +
        "<pre|L1,L2]― [post|+L3>Mary Wollstonecraft Shelley, Frankenstein<post|L3]<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);

      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("pre"),
          markupSketch("q"),
          markupSketch("s"),
          markupSketch("post")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("“Man,\""),
          textNodeSketch(" I "),
          textNodeSketch("cried"),
          textNodeSketch("pleaded"),
          textNodeSketch(", "),
          textNodeSketch("\"how ignorant art thou in thy pride of wisdom!”"),
          textNodeSketch("― "),
          textNodeSketch("Mary Wollstonecraft Shelley, Frankenstein")
      );
      assertThat(document.getLayerNames()).containsExactly("", "L1", "L2", "L3");
      TAGTextNode pleaded = document.getTextNodeStream()
          .filter(tn -> tn.getText().equals("pleaded"))
          .findFirst()
          .get();
      List<TAGMarkup> markups = document.getMarkupStreamForTextNode(pleaded).collect(toList());
      assertThat(markups).extracting("tag").containsExactly(":branch", ":branches", "s", "pre", "tagml");

      List<TAGMarkup> preMarkups = document.getMarkupStream().filter(m -> m.hasTag("pre")).collect(toList());
      assertThat(preMarkups).hasSize(1);
      final TAGMarkup l1RootMarkup = preMarkups.get(0);

      List<TAGTextNode> l1TextNodes = document.getTextNodeStreamForMarkupInLayer(l1RootMarkup, "L1").collect(toList());
      assertThat(l1TextNodes).extracting("text")
          .containsExactly(
              "“Man,\"",
              "\"how ignorant art thou in thy pride of wisdom!”"
          );
    });
  }

  @Test
  public void testOptionalMarkup() {
    String input = "[tagml>" +
        "[?optional>optional<?optional]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          optionalMarkupSketch("optional")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("optional")
      );
    });
  }

  @Test
  public void testDiscontinuity() {
    String input = "[tagml>" +
        "[discontinuity>yes<-discontinuity]no[+discontinuity>yes<discontinuity]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("discontinuity")
      );
    });
  }

  @Test
  public void testMilestone() {
    String input = "[tagml>pre " +
        "[milestone x=4]" +
        " post<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("milestone")
      );
    });
  }

  @Test
  public void testMarkupIdentifier() {
    String input = "[tagml>" +
        "[id~1>identified<id~1]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("id")
      );
    });
  }

  @Test
  public void testStringAnnotation() {
    String input = "[text author='somebody'>some text.<text]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("text")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("some text.")
      );
      assertThat(document).hasMarkupWithTag("text").withStringAnnotation("author", "somebody");
    });
  }

  @Test
  public void testStringAnnotation1() {
    String input = "[tagml>" +
        "[m s=\"string\">text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
      assertThat(document).hasMarkupWithTag("m").withStringAnnotation("s", "string");
    });
  }

  @Test
  public void testStringAnnotation2() {
    String input = "[tagml>" +
        "[m s='string'>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
      assertThat(document).hasMarkupWithTag("m").withStringAnnotation("s", "string");
    });
  }

  @Test
  public void testNumberAnnotation() {
    String input = "[text pi=3.1415926>some text.<text]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("text")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("some text.")
      );
      assertThat(document).hasMarkupWithTag("text").withNumberAnnotation("pi", 3.1415926f);
    });
  }

  @Test
  public void testBooleanAnnotation() {
    String input = "[text test=true>some text.<text]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("text")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("some text.")
      );
      assertThat(document).hasMarkupWithTag("text").withBooleanAnnotation("test", true);
    });
  }

  @Test
  public void testBooleanAnnotation1() {
    String input = "[tagml>" +
        "[m b=true>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
      assertThat(document).hasMarkupWithTag("m").withBooleanAnnotation("b", true);
    });
  }

  @Test
  public void testBooleanAnnotation2() {
    String input = "[tagml>" +
        "[m b=FALSE>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
      assertThat(document).hasMarkupWithTag("m").withBooleanAnnotation("b", false);
    });
  }

  @Test
  public void testObjectAnnotation0() {
    String input = "[tagml>" +
        "[m p={valid=false}>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
    });
  }

  @Test
  public void testObjectAnnotation1() {
    String input = "[tagml>" +
        "[m p={x=1 y=2}>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
    });
  }

  @Test
  public void testNestedObjectAnnotation() {
    String input = "[text meta={\n" +
        "    persons=[\n" +
        "      {:id=huyg0001 name='Constantijn Huygens'}\n" +
        "    ]\n" +
        "  }>[title>De Zee-Straet<title]\n" +
        "  door [author pers->huyg0001>Constantijn Huygens<author]\n" +
        "  .......\n" +
        "<text]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("text"),
          markupSketch("title"),
          markupSketch("author")
      );
    });
  }

  @Test
  public void testSimpleTextVariation() {
    String input = "[tagml>" +
        "pre <|to be|not to be|> post" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testTextVariationWithMarkup() {
    String input = "[tagml>" +
        "pre <|[del>to be<del]|[add>not to be<add]|> post" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("del"),
          markupSketch("add")
      );
    });
  }

  @Test
  public void testNestedTextVariationWithMarkup() {
    String input = "[tagml>" +
        "pre <|" +
        "[del>to be<del]" +
        "|" +
        "[add>not to <|[del>completely<del]|[add>utterly<add]|> be<add]" +
        "|> post" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("del"),
          markupSketch("del"),
          markupSketch("add"),
          markupSketch("add")
      );
    });
  }

  @Test
  public void testElementLinking() {
    String input = "[tagml meta={:id=meta01 name='test'}>" +
        "pre [x ref->meta01>text<x] post" +
        "<tagml]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("x")
      );
    });
  }

  @Test
  public void testNestedObjectAnnotation2() {
    String input = "[t meta={:id=meta01 obj={t='test'} n=1}>" +
        "text" +
        "<t]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );
    });
  }

  @Test
  public void testMixedContentAnnotation1() {
    String input = "[t note=[>This is a [n>note<n] about this text<]>main text<t]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );
    });
  }

  @Test
  public void testNamespaceNeedsToBeDefinedBeforeUsage() {
    String input = "[z:t>text<z:t]";
    store.runInTransaction(() -> assertTAGMLParsesWithSyntaxError(input, "line 1:1 : Namespace z has not been defined."));
  }

  @Test
  public void testIdentifyingMarkup() {
    String input = "[m :id=m1>" +
        "pre [x ref->m1>text<x] post" +
        "<m]";
    store.runInTransaction(() -> {
      TAGDocument document = assertTAGMLParses(input);
      assertThat(document).hasMarkupMatching(
          markupSketch("m"),
          markupSketch("x")
      );
      TAGMarkup m1 = document.getMarkupStream()
          .filter(m -> m.hasTag("m"))
          .findFirst().get();
      assertThat(m1).hasMarkupId("m1");
    });
  }

  private TAGDocument assertTAGMLParses(final String input) {
    printTokens(input);

    CodePointCharStream antlrInputStream = CharStreams.fromString(input);
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TAGMLParser parser = new TAGMLParser(tokens);
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    assertThat(numberOfSyntaxErrors).isEqualTo(0);

//    TAGMLListener listener = new TAGMLListener(store, errorListener);
    TAGMLListener listener = new TAGMLListener(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getErrors());
    }
    assertThat(errorListener.hasErrors()).isFalse();

    TAGDocument document = listener.getDocument();
    logDocumentGraph(document, input);

    String lmnl = LMNL_EXPORTER.toLMNL(document);
    LOG.info("\nLMNL:\n{}\n", lmnl);
    return document;
  }

  private void assertTAGMLParsesWithSyntaxError(String input, String expectedSyntaxErrorMessage) {
    printTokens(input);

    CodePointCharStream antlrInputStream = CharStreams.fromString(input);
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TAGMLParser parser = new TAGMLParser(tokens);
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);

//    TAGMLListener listener = new TAGMLListener(store, errorListener);
    TAGMLListener listener = new TAGMLListener(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getErrors());
    }
    assertThat(errorListener.getErrors()).contains(expectedSyntaxErrorMessage);
  }

}
