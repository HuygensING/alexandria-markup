package nl.knaw.huc.di.tag.tagml.importer;

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

import com.google.common.collect.Lists;
import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huc.di.tag.tagml.TAGML.BRANCH;
import static nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES;
import static nl.knaw.huygens.alexandria.storage.dto.TAGDocumentAssert.*;

public class TAGMLParserTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLParserTest.class);
//  private static final TAGMLExporter TAGML_EXPORTER = new TAGMLExporter(store);

  @Test
  public void testTagWithReferenceParses() {
    String input = "[tagml pers->pers01>Some text<tagml]";
    runInStore(store -> {
      AnnotationInfo pers = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml")
        );
        assertThat(document).hasTextNodesMatching(
            textNodeSketch("Some text")
        );
        assertThat(document.getLayerNames()).containsExactly("");

        TAGMarkup tagmlMarkup = document.getMarkupStream().findFirst().get();
        assertThat(tagmlMarkup).hasTag("tagml");

        AnnotationInfo persInfo = tagmlMarkup.getAnnotation("pers");
        assertThat(persInfo).isReference();
        return persInfo;
      });

      store.runInTransaction(() -> {
        String persValue = store.getReferenceValue(pers.getNodeId()).getValue();
        assertThat(persValue).isEqualTo("pers01");
      });
    });
  }

  @Test // Rd-205
  public void testDefaultLayerIsAlwaysOpen() {
    String input = "[tagml|+A>[x|A>simple<x] [t>text<t] [t>test<t]<tagml]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("x"),
          markupSketch("t"),
          markupSketch("t")
      );
      assertThat(document).hasTextNodesMatching(
          textNodeSketch("simple"),
          textNodeSketch(" "),
          textNodeSketch("text"),
          textNodeSketch(" "),
          textNodeSketch("test")
      );
      assertThat(document.getLayerNames()).containsExactly("", "A");
    });
  }

  @Test // RD-131
  public void testSimpleTextWithRoot() {
    String input = "[tagml>simple text<tagml]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
      assertThat(markups).hasSize(2);
      final TAGMarkup q = markups.get(0);

      List<TAGTextNode> qTextNodes = document.getTextNodeStreamForMarkupInLayer(q, "L1").collect(toList());
      assertThat(qTextNodes).extracting("text")
          .containsExactly("“Man,\"", "\"how ignorant art thou in thy pride of wisdom!”");
    });
  }

  @Ignore
  @Test // RD-134
  public void testTextWithMultipleLayersDiscontinuityAndNonLinearity() {
    String input = "[tagml>[pre|+L1,+L2>" +
        "[q|L1>“Man,\"<-q|L1][s|L2> I " +
        "<|cried|pleaded|>" +
        ", <s|L2][+q|L1>\"how ignorant art thou in thy pride of wisdom!”<q|L1]" +
        "<pre|L1,L2]― [post|+L3>Mary Wollstonecraft Shelley, Frankenstein<post|L3]<tagml]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);

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
      assertThat(markups).extracting("tag").containsExactly(BRANCH, BRANCHES, "s", "pre", "tagml");

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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("id")
      );
    });
  }

  @Test
  public void testStringAnnotation() {
    String input = "[text author='somebody'>some text.<text]";
    runInStore(store -> {
      TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("text")
        );
        assertThat(document).hasTextNodesMatching(
            textNodeSketch("some text.")
        );
        assertThat(document).hasMarkupWithTag("text").withStringAnnotation("author", "somebody");
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test
  public void testStringAnnotation1() {
    String input = "[tagml>" +
        "[m s=\"string\">text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        assertThat(document).hasMarkupWithTag("m").withStringAnnotation("s", "string");
        return document;
      });
      assertExportEqualsInput(input.replace("\"", "'"), doc, store);
    });
  }

  @Test
  public void testStringAnnotation2() {
    String input = "[tagml>" +
        "[m s='string'>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        assertThat(document).hasMarkupWithTag("m").withStringAnnotation("s", "string");
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test
  public void testNumberAnnotation() {
    String input = "[text pi=3.1415926>some text.<text]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("text")
        );
        assertThat(document).hasTextNodesMatching(
            textNodeSketch("some text.")
        );
        assertThat(document).hasMarkupWithTag("text").withNumberAnnotation("pi", 3.1415926);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test
  public void testNumberAnnotation1() {
    String input = "[text n=1>some text.<text]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("text")
        );
        assertThat(document).hasTextNodesMatching(
            textNodeSketch("some text.")
        );
        assertThat(document).hasMarkupWithTag("text").withNumberAnnotation("n", 1.0);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test
  public void testBooleanAnnotation() {
    String input = "[text test=true>some text.<text]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("text")
        );
        assertThat(document).hasTextNodesMatching(
            textNodeSketch("some text.")
        );
        assertThat(document).hasMarkupWithTag("text").withBooleanAnnotation("test", true);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test
  public void testBooleanAnnotation1() {
    String input = "[tagml>" +
        "[m b=true>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        assertThat(document).hasMarkupWithTag("m").withBooleanAnnotation("b", true);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test
  public void testBooleanAnnotation2() {
    String input = "[tagml>" +
        "[m b=FALSE>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        assertThat(document).hasMarkupWithTag("m").withBooleanAnnotation("b", false);
        return document;
      });
      assertExportEqualsInput(input.replace("FALSE", "false"), doc, store);
    });
  }

  @Test // NLA-468
  public void testStringListAnnotation() {
    String input = "[tagml>" +
        "[m l=['a','b','c']>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        List<String> expected = Lists.newArrayList("a", "b", "c");
        assertThat(document).hasMarkupWithTag("m").withListAnnotation("l", expected);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test // NLA-468
  public void testNumberListAnnotation() {
    String input = "[tagml>" +
        "[m l=[3,5,7,11]>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        List<Float> expected = Lists.newArrayList(3F, 5F, 7F, 11F);
        assertThat(document).hasMarkupWithTag("m").withListAnnotation("l", expected);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test // NLA-468
  public void testListAnnotationEntriesShouldAllBeOfTheSameType() {
    String input = "[tagml>" +
        "[m l=[3,true,'string']>text<m]" +
        "<tagml]";

    final String expectedError = "line 1:13 : All elements of ListAnnotation l should be of the same type.";
    runInStoreTransaction(store -> assertTAGMLParsesWithSyntaxError(input, expectedError, store));
  }

  @Test // RD-206
  public void testListElementSeparatorShouldBeComma() {
    String input = "[tagml>" +
        "[m l=[3 5 7 11]>text<m]" +
        "<tagml]";
    final String expectedError = "line 1:13 : The elements of ListAnnotation l should be separated by commas.";
    runInStoreTransaction(store -> assertTAGMLParsesWithSyntaxError(input, expectedError, store));
  }

  @Test // NLA-467
  public void testObjectAnnotation0() {
    String input = "[tagml>" +
        "[m p={valid=false}>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        Map<String, Object> expected = new HashMap();
        expected.put("valid", false);
        assertThat(document).hasMarkupWithTag("m").withObjectAnnotation("p", expected);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test // NLA-467
  public void testObjectAnnotation1() {
    String input = "[tagml>" +
        "[m p={x=1 y=2}>text<m]" +
        "<tagml]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("tagml"),
            markupSketch("m")
        );
        Map<String, Object> expected = new HashMap();
        expected.put("x", 1D);
        expected.put("y", 2D);
        assertThat(document).hasMarkupWithTag("m").withObjectAnnotation("p", expected);
        return document;
      });
      assertExportEqualsInput(input, doc, store);
    });
  }

  @Test // NLA-467
  public void testNestedObjectAnnotation() {
    String input = "[text meta={\n" +
        "    persons=[\n" +
        "      {:id=huyg0001 name='Constantijn Huygens'}\n" +
        "    ]\n" +
        "  }>[title>De Zee-Straet<title]\n" +
        "  door [author pers->huyg0001>Constantijn Huygens<author]\n" +
        "  .......\n" +
        "<text]";
    runInStore(store -> {
      final TAGDocument doc = store.runInTransaction(() -> {
        TAGDocument document = assertTAGMLParses(input, store);
        assertThat(document).hasMarkupMatching(
            markupSketch("text"),
            markupSketch("title"),
            markupSketch("author")
        );
        Map<String, Object> ch = new HashMap<>();
        ch.put(":id", "huyg001");
        ch.put("name", "Constantijn Huygens");

        List<Map<String, Object>> expected = Lists.newArrayList(ch);
//      assertThat(document).hasMarkupWithTag("text").withObjectAnnotation("meta", expected);
        return document;
      });
      LOG.info("export={}", export(doc, store));
    });
  }

  @Test
  public void testSimpleTextVariation() {
    String input = "[tagml>" +
        "pre <|to be|not to be|> post" +
        "<tagml]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
      assertThat(document).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testTextVariationWithMarkup() {
    String input = "[tagml>" +
        "pre <|[del>to be<del]|[add>not to be<add]|> post" +
        "<tagml]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
      assertThat(document).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("del"),
          markupSketch("del"),
          markupSketch("add"),
          markupSketch("add")
      );
    });
  }

  @Ignore
  @Test
  public void testElementLinking() {
    String input = "[tagml meta={:id=meta01 name='test'}>" +
        "pre [x ref->meta01>text<x] post" +
        "<tagml]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );
    });
  }

  @Test
  public void testRichTextAnnotation1() {
    String input = "[t note=[>[p>This is a [n>note<n] about this text<p]<]>main text<t]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
      assertThat(document).hasMarkupMatching(
          markupSketch("t")
      );
    });
  }

  @Test
  public void testNamespaceNeedsToBeDefinedBeforeUsage() {
    String input = "[z:t>text<z:t]";
    runInStoreTransaction(store -> assertTAGMLParsesWithSyntaxError(input, "line 1:1 : Namespace z has not been defined.", store));
  }

  @Test
  public void testIdentifyingMarkup() {
    String input = "[m :id=m1>" +
        "pre [x ref->m1>text<x] post" +
        "<m]";
    runInStoreTransaction(store -> {
      TAGDocument document = assertTAGMLParses(input, store);
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

  private TAGDocument assertTAGMLParses(final String input, final TAGStore store) {
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

    TAGMLListener listener = new TAGMLListener(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getErrors());
    }
    assertThat(errorListener.hasErrors()).isFalse();

    TAGDocument document = listener.getDocument();
    logDocumentGraph(document, input);

//    export(document);

    return document;
  }

  private void assertTAGMLParsesWithSyntaxError(String input, String expectedSyntaxErrorMessage, final TAGStore store) {
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

  private void assertExportEqualsInput(String input, TAGDocument doc, final TAGStore store) {
    String tagml = export(doc, store);
    assertThat(tagml).isEqualTo(input);
  }

  private String export(final TAGDocument document, final TAGStore store) {
    String tagml = store.runInTransaction(() -> new TAGMLExporter(store).asTAGML(document));
    LOG.info("\nTAGML:\n{}\n", tagml);
    return tagml;
  }

}
