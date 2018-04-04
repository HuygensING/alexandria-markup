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
import static nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert.markupSketch;
import static nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert.textNodeSketch;

public class TAGMLParserTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLParserTest.class);
  private static final LMNLExporter LMNL_EXPORTER = new LMNLExporter(store);

  @Test
  public void testOptionalMarkup() {
    String input = "[tagml>" +
        "[?optional>optional<?optional]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("optional")
      );
      assertThat(documentWrapper).hasTextNodesMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testMilestone() {
    String input = "[tagml>pre " +
        "[milestone x=4]" +
        " post<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testMarkupIdentifier() {
    String input = "[tagml>" +
        "[id~1>identified<id~1]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testStringAnnotation1() {
    String input = "[tagml>" +
        "[m s=\"string\">text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testStringAnnotation2() {
    String input = "[tagml>" +
        "[m s='string'>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testNumberAnnotation() {
    String input = "[tagml>\n" +
        "[markup pi=3.1415>text<markup]\n" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testBooleanAnnotation1() {
    String input = "[tagml>" +
        "[m b=true>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testBooleanAnnotation2() {
    String input = "[tagml>" +
        "[m b=FALSE>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testSimpleTextVariation() {
    String input = "[tagml>" +
        "pre |>to be|not to be<| post" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testTextVariationWithMarkup() {
    String input = "[tagml>" +
        "pre |>[del>to be<del]|[add>not to be<add]<| post" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testNestedTextVariationWithMarkup() {
    String input = "[tagml>" +
        "pre |>" +
        "[del>to be<del]" +
        "|" +
        "[add>not to |>[del>completely<del]|[add>utterly<add]<| be<add]" +
        "<| post" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
//      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  private DocumentWrapper assertTAGMLParses(final String input) {
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
    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));
    TAGMLListener listener = new TAGMLListener(store);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    assertThat(numberOfSyntaxErrors).isEqualTo(0);
    DocumentWrapper document = listener.getDocument();
    String lmnl = LMNL_EXPORTER.toLMNL(document);
    LOG.info("\nLMNL:\n{}\n", lmnl);
    return document;
  }
}
