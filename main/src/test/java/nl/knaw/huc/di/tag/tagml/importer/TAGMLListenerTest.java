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

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.tagml.TAGMLBreakingError;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.assertj.core.util.Files;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMLListenerTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListenerTest.class);
  //  private static final LMNLExporter LMNL_EXPORTER = new LMNLExporter(store);

  @Test
  public void testSnarkParses() {
    String input = Files.contentOf(new File("data/tagml/snark81.tagml"), Charset.defaultCharset());
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
        });
  }

  @Test
  public void testSchemaLocation() {
    String input =
        "[!schema http://tag.com/schemas/test-schema.yaml]\n"
            + "[tagml>"
            + "[a>a<a] [b>b<b]"
            + "<tagml]";
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
          final URL url;
          try {
            url = new URL("http://tag.com/schemas/test-schema.yaml");
            assertThat(document).hasSchemaLocation(url);
          } catch (MalformedURLException e) {
            e.printStackTrace();
          }
        });
  }

  @Test
  public void testNonOverlappingMarkupWithoutLayerInfo() {
    String input = "[tagml>" + "[a>a<a] [b>b<b]" + "<tagml]";
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
        });
  }

  @Test
  public void testUnresumedMarkup() {
    String input = "[tagml>[q>That<-q], she said, [q>is amazing!<q]<tagml]";
    String expectedSyntaxErrorMessage = "line 1:15 : Some suspended markup was not resumed: <-q]";
    assertTAGMLParsesWithSyntaxError(input, expectedSyntaxErrorMessage);
  }

  @Test
  public void testOverlappingMarkupWithoutLayerInfo() {
    String input = "[tagml>" + "[a>a [b>b<a]<b]" + "<tagml]";
    String expectedSyntaxErrorMessage =
        "line 1:1 : Missing close tag(s) for: [tagml>\n"
            + "line 1:8 : Missing close tag(s) for: [a>\n"
            + "line 1:17 : Close tag <a] found, expected <b]. Use separate layers to allow for overlap.\n"
            + "line 1:23 : Close tag <tagml] found, expected <a]. Use separate layers to allow for overlap.";
    assertTAGMLParsesWithSyntaxError(input, expectedSyntaxErrorMessage);
  }

  @Test
  public void testNonOverlappingMarkupWithLayerInfo() {
    String input = "[tagml|+a>" + "[a|a>a<a|a] [b|a>b<b|a]" + "<tagml|a]";
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
        });
  }

  @Test
  public void testOverlappingMarkupWithLayerInfo() {
    String input = "[tagml|+a,+b>" + "[a|a>a [b|b>b<a|a]<b|b]" + "<tagml|a,b]";
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
        });
  }

  @Test
  public void testBranchError() {
    String input =
        "[tagml>[layerdef|+sem,+gen>"
            + "[l|sem>a <|[add|gen>added<add]|[del|gen>del<del]|> line<l]"
            + "<layerdef]<tagml]";
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
        });
  }

  @Test
  public void testLayerShouldBeHierarchical() {
    String input =
        "[tagml|+a,+b>"
            + "[page|b>"
            + "[book|a>book title"
            + "[chapter|a>chapter title"
            + "[para|a>paragraph text"
            + "<page|b]"
            + "<chapter|a]<book|a]"
            + "[! para should close before chapter !]<para|a]"
            + "<tagml|a,b]";

    String expectedSyntaxErrorMessage =
        "line 1:1 : Missing close tag(s) for: [tagml|a,b>\n"
            + "line 1:22 : Missing close tag(s) for: [book|a>\n"
            + "line 1:40 : Missing close tag(s) for: [chapter|a>\n"
            + "line 1:94 : Close tag <chapter|a] found, expected <para|a].\n"
            + "line 1:105 : Close tag <book|a] found, expected <para|a].\n"
            + "line 1:159 : Close tag <tagml|a,b] found, expected <chapter|a].";
    assertTAGMLParsesWithSyntaxError(input, expectedSyntaxErrorMessage);
  }

  @Test
  public void testNonlinearText() {
    String input = "[o>Ice cream is <|tasty|cold|sweet|>!<o]";
    runInStoreTransaction(
        store -> {
          TAGDocument document = assertTAGMLParses(input, store);
          logDocumentGraph(document, input);

          TextGraph textGraph = document.getDTO().textGraph;

          List<TAGTextNode> textNodes = document.getTextNodeStream().collect(toList());
          assertThat(textNodes).hasSize(5);

          TAGTextNode textNode1 = textNodes.get(0);
          assertThat(textNode1).hasText("Ice cream is ");

          //      TAGTextNode textNode2 = textNodes.get(1);
          //      assertThat(textNode2).isDivergence();
          //      Long divergenceNode = textNode2.getResourceId();
          //      assertThat(incomingTextEdges(textGraph, divergenceNode)).hasSize(1);
          //      assertThat(outgoingTextEdges(textGraph, divergenceNode)).hasSize(3);

          TAGTextNode textNode3 = textNodes.get(1);
          assertThat(textNode3).hasText("tasty");

          TAGTextNode textNode4 = textNodes.get(2);
          assertThat(textNode4).hasText("cold");

          TAGTextNode textNode5 = textNodes.get(3);
          assertThat(textNode5).hasText("sweet");

          //      TAGTextNode textNode6 = textNodes.get(5);
          //      assertThat(textNode6).isConvergence();
          //      Long convergenceNode = textNode6.getResourceId();
          //      assertThat(incomingTextEdges(textGraph, convergenceNode)).hasSize(3);
          //      assertThat(outgoingTextEdges(textGraph, convergenceNode)).hasSize(1);

          TAGTextNode textNode7 = textNodes.get(4);
          assertThat(textNode7).hasText("!");
        });
  }

  // private methods

  private TAGDocument assertTAGMLParses(final String input, final TAGStore store) {
    ErrorListener errorListener = new ErrorListener();
    TAGMLParser parser = setupParser(input, errorListener);
    ParseTree parseTree = parser.document();
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    assertThat(numberOfSyntaxErrors).isEqualTo(0);

    TAGMLListener listener = walkParseTree(errorListener, parseTree, store);
    assertThat(errorListener.hasErrors()).isFalse();

    TAGDocument document = listener.getDocument();
    logDocumentGraph(document, input);
    final Map<Long, RangePair> markupRanges = document.getMarkupRangeMap();
    LOG.info("markup={}", markupRanges);
    //    String lmnl = new LMNLExporter(store).toLMNL(document);
    //    LOG.info("\nLMNL:\n{}\n", lmnl);
    return document;
  }

  private void assertTAGMLParsesWithSyntaxError(String input, String expectedSyntaxErrorMessage) {
    runInStoreTransaction(
        store -> {
          ErrorListener errorListener = new ErrorListener();
          TAGMLParser parser = setupParser(input, errorListener);
          ParseTree parseTree = parser.document();
          LOG.info("parsetree: {}", parseTree.toStringTree(parser));

          int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
          LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);

          try {
            TAGMLListener listener = walkParseTree(errorListener, parseTree, store);
            TAGDocument document = listener.getDocument();
            logDocumentGraph(document, input);
            //              fail("expected TAGMLBreakingError");
          } catch (TAGMLBreakingError e) {
          }
          assertThat(errorListener.hasErrors()).isTrue();
          String errors = errorListener.getPrefixedErrorMessagesAsString();
          assertThat(errors).isEqualTo(expectedSyntaxErrorMessage);
        });
  }

  private TAGMLParser setupParser(final String input, final ErrorListener errorListener) {
    printTokens(input);

    CodePointCharStream antlrInputStream = CharStreams.fromString(input);
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    lexer.addErrorListener(errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TAGMLParser parser = new TAGMLParser(tokens);
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    return parser;
  }

  private TAGMLListener walkParseTree(
      final ErrorListener errorListener, final ParseTree parseTree, final TAGStore store) {
    TAGMLListener listener = new TAGMLListener(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getPrefixedErrorMessagesAsString());
    }
    return listener;
  }
}
