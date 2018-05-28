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
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert.*;

public class TAGMLParserTest extends TAGBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLParserTest.class);
  private static final LMNLExporter LMNL_EXPORTER = new LMNLExporter(store);

  @Test // RD-131
  public void testSimpleTextWithRoot() {
    String input = "[tagml>simple text<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml")
      );
      assertThat(documentWrapper).hasTextNodesMatching(
          textNodeSketch("simple text")
      );
      assertThat(documentWrapper.getLayerIds()).containsExactly("");
    });
  }

  @Test // RD-132
  public void testTextWithMultipleLayersOfMarkup() {
    String input = "[!ld L1 \"layer 1\"][!ld L2 \"layer 2\"][!ld L3 \"layer 3\"]\n" +
        "[L1,L2|root>[L3|post>simple text<L1,L2|root]epic epilog<L3|post]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("root"),
          markupSketch("post")

      );
      assertThat(documentWrapper).hasTextNodesMatching(
          textNodeSketch("simple text"),
          textNodeSketch("epic epilog")
      );
      assertThat(documentWrapper.getLayerIds()).containsExactly("L1", "L2", "L3");
    });
  }

  @Test // RD-133
  public void testTextWithMultipleLayersAndDiscontinuity() {
    String input = "[!ld L1 \"layer 1\"][!ld L2 \"layer 2\"][!ld L3 \"layer 3\"]\n" +
        "[L1,L2|root>[L1|q>“Man,\"<-L1|q][L2|s> I cried, <L2|s][+L1|q>\"how ignorant art thou in thy pride of wisdom!”<L1|q]<L1,L2|root]― [L3|post>Mary Wollstonecraft Shelley, Frankenstein<L3|post]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("root"),
          markupSketch("q"),
          markupSketch("s"),
          markupSketch("post")
      );
      assertThat(documentWrapper).hasTextNodesMatching(
          textNodeSketch("“Man,\""),
          textNodeSketch(" I cried, "),
          textNodeSketch("\"how ignorant art thou in thy pride of wisdom!”"),
          textNodeSketch("― "),
          textNodeSketch("Mary Wollstonecraft Shelley, Frankenstein")
      );
      assertThat(documentWrapper.getLayerIds()).containsExactly("L1", "L2", "L3");
    });
  }

  @Test // RD-134
  public void testTextWithMultipleLayersDiscontinuityAndNonLinearity() {
    String input = "[!ld L1 \"layer 1\"][!ld L2 \"layer 2\"][!ld L3 \"layer 3\"]\n" +
        "[L1,L2|root>[L1|q>“Man,\"<-L1|q][L2|s> I <|cried|pleaded|>, <L2|s][+L1|q>\"how ignorant art thou in thy pride of wisdom!”<L1|q]<L1,L2|root]― [L3|post>Mary Wollstonecraft Shelley, Frankenstein<L3|post]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("root"),
          markupSketch("q"),
          markupSketch("s"),
          markupSketch("post")
      );
      assertThat(documentWrapper).hasTextNodesMatching(
          textNodeSketch("“Man,\""),
          textNodeSketch(" I "),
          textNodeSketch("cried"),
          textNodeSketch("pleaded"),
          textNodeSketch(", "),
          textNodeSketch("\"how ignorant art thou in thy pride of wisdom!”"),
          textNodeSketch("― "),
          textNodeSketch("Mary Wollstonecraft Shelley, Frankenstein")
      );
      assertThat(documentWrapper.getLayerIds()).containsExactly("L1", "L2", "L3");
    });
  }

  @Test
  public void testOptionalMarkup() {
    String input = "[tagml>" +
        "[?optional>optional<?optional]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          optionalMarkupSketch("optional")
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
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("id")
      );
    });
  }

  @Test
  public void testStringAnnotation1() {
    String input = "[tagml>" +
        "[m s=\"string\">text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
    });
  }

  @Test
  public void testStringAnnotation2() {
    String input = "[tagml>" +
        "[m s='string'>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
    });
  }

  @Test
  public void testNumberAnnotation() {
    String input = "[tagml>\n" +
        "[markup pi=3.1415>text<markup]\n" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("markup")
      );
    });
  }

  @Test
  public void testBooleanAnnotation1() {
    String input = "[tagml>" +
        "[m b=true>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
    });
  }

  @Test
  public void testBooleanAnnotation2() {
    String input = "[tagml>" +
        "[m b=FALSE>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("tagml"),
          markupSketch("m")
      );
    });
  }

  @Test
  public void testObjectAnnotation0() {
    String input = "[tagml>" +
        "[m p={valid=false}>text<m]" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(markupSketch("tagml"));
    });
  }

  @Test
  public void testTextVariationWithMarkup() {
    String input = "[tagml>" +
        "pre <|[del>to be<del]|[add>not to be<add]|> post" +
        "<tagml]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("t")
      );
    });
  }

  @Test
  public void testMixedContentAnnotation1() {
    String input = "[t note=[>This is a [n>note<n] about this text<]>main text<t]";
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
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
      DocumentWrapper documentWrapper = assertTAGMLParses(input);
      assertThat(documentWrapper).hasMarkupMatching(
          markupSketch("m"),
          markupSketch("x")
      );
      MarkupWrapper m1 = documentWrapper.getMarkupStream().filter(m -> m.hasTag("m")).findFirst().get();
      assertThat(m1).hasMarkupId("m1");
    });
  }

  @Test
  public void testAnaphoricExample() {
    String eag = "[Extract}An ARVN officer asked [AC in Extract}[Exp in AC}a young prisoner{Exp in AC] questions, and when [Exp in AC}he{Exp in AC] failed to answer, beat [Exp in AC}him{Exp in AC]." +
        " [#Blue over Extract>[AC in #Blue}[Exp in AC#Blue}An American observer who saw [#Red over  Extract>[AC in  #Red}[Exp in AC#Red}the beating <#Blue  over  Extract]{Exp in AC#Blue]{AC in #Blue]that happened then {Exp in AC#Red]reported that the officer “really worked [Exp in AC}him{Exp in AC] over”." +
        " After [Exp in AC#Red}the beating{Exp in AC#Red]{AC in #Red]<#Red over Extract], [Exp in AC}the prisoner {Exp in AC]{AC in Extract] was forced to remain standing for hours.{Extract]";

    String tagml = "[extract%p+blue+red>An ARVN officer asked [ac@p>[exp@p>a young prisoner<exp@p] questions, and when [exp@p>he<exp@p] failed to answer, beat [exp@p>him<exp@p]." +
        " [ac@blue>[exp@blue>An American observer who saw [ac@red>[exp@red>the beating<exp@blue]<ac@blue] that happened then<exp@red] reported that the officer “really worked [exp@a>him<exp@a] over”." +
        " After [exp@red>the beating<exp@red]<ac@red], [exp@a>the prisoner<exp@p]<ac@p] was forced to remain standing for hours.<extract]";
    /*
    Alternative/additional way to indicate hierarchy, inspired by the anaphoric chains example from the Linear extended Annotation Graph paper.
    The [extract%p+blue+red> markup open tag indicates that the [extract] markup contains 3 overlapping hierarchies 'p', 'blue' and 'red'
    The first ac gets the layer id 'p' and contains the expressions 'a young prisoner','he','him','him','the prisoner'
    The second ac gets the layer id 'blue' and contains the [exp] 'An American observer who
saw the beating'
    The third ac gets the layer id 'red' and contains the [exp]s 'the beating that happened then' and 'the beating'
    The first markup tag to get a unique layer id should be the containing markup for that layer, all markup with that layer id should not overlap, and so form a markup tree for that layer.
    In the eAG representation, the connection between AC and Extract is made Explicit.
    In the TAGML representation, there is no such explicit connection. This connection should be in the schema, where it's defined that each [extract] has a list of (potentially overlapping) [ac] hierarchies.
    (Alternatively, it could be added to the [extract] markup open tag like this: [extract%p+blue+red> ... <extract], where it's implied that the layers identified by p, blue and red can overlap within [extract]  )
    So for tagml, by default it is assumed that the markup is arranged as a tree, like in xml.
    In case of overlap, the suffixes '%' '@' must be used: the markup that contains overlapping layer should list the layer ids of those layers in the markup open tag, after '%', separated by '+'
    All markup within a layer should be hierarchical, and identified with the layer id on both open and close tag, after the suffix '@'



    */
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = assertTAGMLParses(tagml);
//      assertThat(documentWrapper).hasMarkupMatching(
//          markupSketch("m"),
//          markupSketch("x")
//      );
//      MarkupWrapper m1 = documentWrapper.getMarkupStream().filter(m -> m.hasTag("m")).findFirst().get();
//      assertThat(m1).hasMarkupId("m1");
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
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    assertThat(numberOfSyntaxErrors).isEqualTo(0);

    TAGMLListener2 listener = new TAGMLListener2(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getErrors());
    }
    assertThat(errorListener.hasErrors()).isFalse();

    DocumentWrapper document = listener.getDocument();
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

    TAGMLListener2 listener = new TAGMLListener2(store, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    if (errorListener.hasErrors()) {
      LOG.error("errors: {}", errorListener.getErrors());
    }
    assertThat(errorListener.getErrors()).contains(expectedSyntaxErrorMessage);
  }

}
