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
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMLListener2Test extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLListener2Test.class);
  private static final LMNLExporter LMNL_EXPORTER = new LMNLExporter(store);

  @Test
  public void testNonOverlappingMarkupWithoutLayerInfo() {
    String input = "[tagml>" +
        "[a>a<a] [b>b<b]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
    });
  }

  @Test
  public void testOverlappingMarkupWithoutLayerInfo() {
    String input = "[tagml>" +
        "[a>a [b>b<a]<b]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
    });
  }

  @Test
  public void testNonOverlappingMarkupWithLayerInfo() {
    String input = "[!ld a \"a\"][a|tagml>" +
        "[a|a>a<a|a] [a|b>b<a|b]" +
        "<a|tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
    });
  }

  @Test
  public void testOverlappingMarkupWithLayerInfo() {
    String input = "[!ld a \"a\"][!ld b \"b\"][a|tagml>" +
        "[a|a>a [b|b>b<a|a]<b|b]" +
        "<a|tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
    });
  }

  // private methods

  private DocumentWrapper assertTAGMLParses(final String input) {
    ErrorListener errorListener = new ErrorListener();
    TAGMLParser parser = setupParser(input, errorListener);
    ParseTree parseTree = parser.document();
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    assertThat(numberOfSyntaxErrors).isEqualTo(0);

    TAGMLListener2 listener = walkParseTree(errorListener, parseTree);
    assertThat(errorListener.hasErrors()).isFalse();

    DocumentWrapper document = listener.getDocument();
    String lmnl = LMNL_EXPORTER.toLMNL(document);
    LOG.info("\nLMNL:\n{}\n", lmnl);
    return document;
  }

  private void assertTAGMLParsesWithSyntaxError(String input, String expectedSyntaxErrorMessage) {
    ErrorListener errorListener = new ErrorListener();
    TAGMLParser parser = setupParser(input, errorListener);
    ParseTree parseTree = parser.document();
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);

    TAGMLListener2 listener = walkParseTree(errorListener, parseTree);
    assertThat(errorListener.getErrors()).contains(expectedSyntaxErrorMessage);
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

  private TAGMLListener2 walkParseTree(final ErrorListener errorListener, final ParseTree parseTree) {
    TAGMLListener2 listener = new TAGMLListener2(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getErrors());
    }
    return listener;
  }
}
