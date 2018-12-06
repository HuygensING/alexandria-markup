package nl.knaw.huygens.alexandria.lmnl.importer;

/*
 * #%L
 * alexandria-markup-core
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

import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.data_model.IndexPoint;
import nl.knaw.huygens.alexandria.data_model.NodeRangeIndex;
import nl.knaw.huygens.alexandria.exporter.LaTeXExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

@Ignore
public class LMNLImporterTest extends AlexandriaBaseStoreTest {
  private final Logger LOG = LoggerFactory.getLogger(LMNLImporterTest.class);

  @Ignore
  @Test
  public void testMarkupAnnotation() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      // Expectations:
      // We expect a Document
      // - with one text node
      // - with one range on it
      // - with one annotation on it.
      TAGDocument expected = store.createDocument();
      TAGMarkup r1 = store.createMarkup(expected, "l");
      AnnotationInfo a1 = simpleAnnotation("n", "144");
      r1.addAnnotation(a1);
      TAGTextNode t1 = store.createTextNode("He manages to keep the upper hand");
      expected.addTextNode(t1, null);
      expected.addMarkup(r1);
      String layerName = "";
      expected.associateTextNodeWithMarkupForLayer(t1, r1, layerName);

      logTAGML(actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
      assertActualMatchesExpected(actual, expected);

      logKdTree(actual);
      NodeRangeIndex index = new NodeRangeIndex(store, actual);
      List<IndexPoint> indexPoints = index.getIndexPoints();
      logKdTree(actual);
      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Ignore
  @Test
  public void testLexingComplex() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[excerpt\n"//
          + "  [source [date}1915{][title}The Housekeeper{]]\n"//
          + "  [author\n"//
          + "    [name}Robert Frost{]\n"//
          + "    [dates}1874-1963{]] }\n"//
          + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
          + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
          + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
          + "{excerpt]";

      LMNLImporter importer = new LMNLImporter(store);
      TAGDocument actual = importer.importLMNL(input);

      TAGDocument expected = store.createDocument();

      TAGTextNode tn00 = store.createTextNode("\n");
      TAGTextNode tn01 = store.createTextNode("He manages to keep the upper hand");
      TAGTextNode tn02 = store.createTextNode("\n");
      TAGTextNode tn03 = store.createTextNode("On his own farm.");
      TAGTextNode tn04 = store.createTextNode(" ");
      TAGTextNode tn05 = store.createTextNode("He's boss.");
      TAGTextNode tn06 = store.createTextNode(" ");
      TAGTextNode tn07 = store.createTextNode("But as to hens:");
      TAGTextNode tn08 = store.createTextNode("\n");
      TAGTextNode tn09 = store.createTextNode("We fence our flowers in and the hens range.");
      TAGTextNode tn10 = store.createTextNode("\n");

      AnnotationInfo date = simpleAnnotation("date", "1915");
      AnnotationInfo title = simpleAnnotation("title", "The Housekeeper");
      AnnotationInfo source = simpleAnnotation("source")/*.addAnnotation(date).addAnnotation(title)*/;

      AnnotationInfo name = simpleAnnotation("name", "Robert Frost");
      AnnotationInfo dates = simpleAnnotation("dates", "1874-1963");
      AnnotationInfo author = simpleAnnotation("author")/*.addAnnotation(name).addAnnotation(dates)*/;

      TAGMarkup excerpt = store.createMarkup(expected, "excerpt")//
          .addAnnotation(source)//
          .addAnnotation(author)//
          .setFirstAndLastTextNode(tn00, tn10);
      expected.associateTextNodeWithMarkupForLayer(tn00, excerpt);
      // 3 sentences
      TAGMarkup s1 = store.createMarkup(expected, "s").setFirstAndLastTextNode(tn01, tn03);
      TAGMarkup s2 = store.createMarkup(expected, "s")/*.addTextNode(tn05)*/;
      TAGMarkup s3 = store.createMarkup(expected, "s").setFirstAndLastTextNode(tn07, tn09);

      // 3 lines
      AnnotationInfo n144 = simpleAnnotation("n", "144");
      TAGMarkup l1 = store.createMarkup(expected, "l")/*.addTextNode(tn01)*/.addAnnotation(n144);

      AnnotationInfo n145 = simpleAnnotation("n", "145");
      TAGMarkup l2 = store.createMarkup(expected, "l").setFirstAndLastTextNode(tn03, tn07).addAnnotation(n145);

      AnnotationInfo n146 = simpleAnnotation("n", "146");
      TAGMarkup l3 = store.createMarkup(expected, "l")/*.addTextNode(tn09)*/.addAnnotation(n146);

      expected.addTextNode(tn00, null)//
          .addTextNode(tn01, null)//
          .addTextNode(tn02, null)//
          .addTextNode(tn03, null)//
          .addTextNode(tn04, null)//
          .addTextNode(tn05, null)//
          .addTextNode(tn06, null)//
          .addTextNode(tn07, null)//
          .addTextNode(tn08, null)//
          .addTextNode(tn09, null)//
          .addTextNode(tn10, null)//
          .addMarkup(excerpt)//
          .addMarkup(s1)//
          .addMarkup(l1)//
          .addMarkup(l2)//
          .addMarkup(s2)//
          .addMarkup(s3)//
          .addMarkup(l3);
      String layerName = "";
      expected.associateTextNodeWithMarkupForLayer(tn00, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn01, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn02, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn03, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn04, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn05, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn06, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn07, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn08, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn09, excerpt, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn10, excerpt, layerName);

      expected.associateTextNodeWithMarkupForLayer(tn01, s1, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn02, s1, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn03, s1, layerName);

      expected.associateTextNodeWithMarkupForLayer(tn05, s2, layerName);

      expected.associateTextNodeWithMarkupForLayer(tn07, s3, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn08, s3, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn09, s3, layerName);

      expected.associateTextNodeWithMarkupForLayer(tn01, l1, layerName);

      expected.associateTextNodeWithMarkupForLayer(tn03, l2, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn04, l2, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn05, l2, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn06, l2, layerName);
      expected.associateTextNodeWithMarkupForLayer(tn07, l2, layerName);

      expected.associateTextNodeWithMarkupForLayer(tn09, l3, layerName);

      assertActualMatchesExpected(actual, expected);

      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      logKdTree(actual);
      // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Test
  public void testLMNL1kings12() throws IOException, LMNLSyntaxError {
    String pathname = "data/lmnl/1kings12.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    store.runInTransaction(() -> {
      LMNLImporter importer = new LMNLImporter(store);
      TAGDocument actual = importer.importLMNL(input);

      LOG.info("document={}", actual);

      logTAGML(actual);

      List<TAGMarkup> actualMarkupList = actual.getMarkupStream().collect(toList());

      TAGMarkup excerpt = actualMarkupList.get(0);
      assertThat(excerpt.getTag()).isEqualTo("excerpt");

      List<AnnotationInfo> annotations = excerpt.getAnnotationStream().collect(toList());
      assertThat(annotations).hasSize(1); // just the soutce annotation;

      AnnotationInfo source = simpleAnnotation("source");
      AnnotationInfo book = simpleAnnotation("book", "1 Kings");
//      source.addAnnotation(book);
      AnnotationInfo chapter = simpleAnnotation("chapter", "12");
//      source.addAnnotation(chapter);
      String actualSourceTAGML = tagmlExporter.toTAGML(annotations.get(0)).toString();
      String expectedSourceTAGML = tagmlExporter.toTAGML(source).toString();
      assertThat(actualSourceTAGML).isEqualTo(expectedSourceTAGML);

      TAGMarkup q1 = actualMarkupList.get(2);
      assertThat(q1.getTag()).isEqualTo("q"); // first q
      assertThat(q1.getTextNodeStream()).hasSize(2); // has 2 textnodes

      TAGMarkup q2 = actualMarkupList.get(3);
      assertThat(q2.getTag()).isEqualTo("q"); // second q, nested in first
      assertThat(q2.getTextNodeStream()).hasSize(1); // has 1 textnode

      // compareTAGML(pathname, actual);
      logKdTree(actual);

      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Ignore
  @Test
  public void testLMNLOzymandias() throws IOException, LMNLSyntaxError {
    String pathname = "data/lmnl/ozymandias-voices-wap.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);
      LOG.info("document={}", actual);
      logTAGML(actual);
      assertThat(actual.hasTextNodes()).isTrue();
      String tagml = tagmlExporter.asTAGML(actual);
      assertThat(tagml).startsWith("[sonneteer [id}ozymandias{] [encoding [resp}ebeshero{] [resp}wap{]]}"); // annotations from sonneteer endtag moved to start tag
      assertThat(tagml).contains("[meta [author}Percy Bysshe Shelley{] [title}Ozymandias{]]"); // anonymous markup
      // compareTAGML(pathname, actual);

      logKdTree(actual);

      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Ignore
  @Test
  public void testDiscontinuousRanges() throws LMNLSyntaxError {
    String input = "'[e [n}1{]}Ai,{e]' riep Piet, '[e [n}1{]}wat doe je, Mien?{e]'";
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      String tagml = tagmlExporter.asTAGML(actual);
      LOG.info("tagml={}", tagml);
      assertThat(tagml).isEqualTo(input);

      LOG.info("textNodes={}", actual.getTextNodeStream());
      LOG.info("markups={}", actual.getMarkupStream());
      assertThat(actual.hasTextNodes()).isTrue();
      assertThat(actual.getMarkupStream()).hasSize(1);

      LaTeXExporter latex = new LaTeXExporter(store, actual);
      LOG.info("matrix=\n{}", latex.exportMatrix());
      LOG.info("kdtree=\n{}", latex.exportKdTree());
    });
  }

  @Ignore
  @Test
  public void testAnnotationTextWithRanges() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[lmnl [a}This is the [type}annotation{type] text{a]}This is the main text{lmnl]";
      printTokens(input);
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      // Expectations:
      // We expect a Document
      // - with one text node
      // - with one range on it
      // - with one annotation on it.
      // - that has one range on it.
//      TAGDocument expected = store.createDocument();
//      TAGMarkup m1 = store.createMarkup(expected, "lmnl");
//      AnnotationInfo a1 = simpleAnnotation("a");
//      TAGDocument annotationDocument = a1.getDocument();
//      TAGTextNode at1 = store.createTextNode("This is the ");
//      TAGTextNode at2 = store.createTextNode("annotation");
//      TAGMarkup am1 = store.createMarkup(annotationDocument, "type")/*.addTextNode(at2)*/;
//      TAGTextNode at3 = store.createTextNode(" text");
//      annotationDocument//
//          .addTextNode(at1)//
//          .addTextNode(at2)//
//          .addTextNode(at3)//
//          .addMarkup(am1);
//      String layerName = "";
//      annotationDocument.associateTextNodeWithMarkupForLayer(at2, am1, layerName);
//      m1.addAnnotation(a1);
//
//      TAGTextNode t1 = store.createTextNode("This is the main text");
////      m1.addTextNode(t1);
//      expected.addTextNode(t1);
//      expected.addMarkup(m1);
//      expected.associateTextNodeWithMarkupForLayer(t1, m1, layerName);
//
//      logTAGML(actual);
//      compareTAGML(expected, actual);
//      assertThat(compareDocuments(expected, actual)).isTrue();
//
//      logKdTree(actual);
//      NodeRangeIndex index = new NodeRangeIndex(store, actual);
//      List<IndexPoint> indexPoints = index.getIndexPoints();
//      logKdTree(actual);
//      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
//      expectedIndexPoints.add(new IndexPoint(0, 0));
//      assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Ignore
  @Test
  public void testAnnotationTextInAnnotationWithRanges() throws LMNLSyntaxError {
    String input = "[range1 [annotation1}[ra11}[ra12]{ra11]{annotation1]]";
    printTokens(input);
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      TAGDocument expected = store.createDocument();

      TAGMarkup m1 = store.createMarkup(expected, "range1");
      AnnotationInfo a1 = simpleAnnotation("annotation1");
//      TAGDocument annotationDocument = a1.getDocument();
//      TAGTextNode at1 = store.createTextNode("");
//      TAGMarkup ar11 = store.createMarkup(annotationDocument, "ra11")/*.addTextNode(at1)*/;
//      TAGMarkup ar12 = store.createMarkup(annotationDocument, "ra12")/*.addTextNode(at1)*/;
//      annotationDocument//
//          .addTextNode(at1)//
//          .addMarkup(ar11)//
//          .addMarkup(ar12);
//      m1.addAnnotation(a1);
//      String layerName = "";
//      annotationDocument.associateTextNodeWithMarkupForLayer(at1, ar11, layerName);
//      annotationDocument.associateTextNodeWithMarkupForLayer(at1, ar12, layerName);
//
//      TAGTextNode t1 = store.createTextNode("");
////      m1.addTextNode(t1);
//      expected.addTextNode(t1);
//      expected.addMarkup(m1);
//      expected.associateTextNodeWithMarkupForLayer(t1, m1, layerName);
//
//      logTAGML(actual);
//      compareTAGML(expected, actual);
//      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Ignore
  @Test
  public void testAnonymousAnnotationRangeOpener() throws LMNLSyntaxError {
    String input = "[range1 [}annotation text{]}bla{range1]";
    printTokens(input);
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      TAGDocument expected = store.createDocument();

      TAGMarkup m1 = store.createMarkup(expected, "range1");
      AnnotationInfo a1 = simpleAnnotation("", "annotation text");
      m1.addAnnotation(a1);

      TAGTextNode t1 = store.createTextNode("bla");
//      m1.addTextNode(t1);
      expected.addTextNode(t1, null);
      expected.addMarkup(m1);
      String layerName = "";
      expected.associateTextNodeWithMarkupForLayer(t1, m1, layerName);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Ignore
  @Test
  public void testAtomsAreIgnored() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[r}Splitting the {{Atom}}.{r]";
      printTokens(input);
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      TAGDocument expected = store.createDocument();

      TAGMarkup m1 = store.createMarkup(expected, "r");
      TAGTextNode t1 = store.createTextNode("Splitting the .");
//      m1.addTextNode(t1);
      expected.addTextNode(t1, null);
      expected.addMarkup(m1);

      String layerName = "";
      expected.associateTextNodeWithMarkupForLayer(t1, m1, layerName);

      logTAGML(actual);
      compareTAGML(expected, actual);
    });
  }

  @Ignore
  @Test
  public void testEmptyRange() throws LMNLSyntaxError {
    String input = "[empty}{empty]";
    printTokens(input);
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);
      TAGDocument expected = store.createDocument();
      TAGMarkup m1 = store.createMarkup(expected, "empty");
      TAGTextNode t1 = store.createTextNode("");
//      m1.addTextNode(t1);
      expected.addTextNode(t1, null);
      expected.addMarkup(m1);
      String layerName = "";
      expected.associateTextNodeWithMarkupForLayer(t1, m1, layerName);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Ignore
  @Test
  public void testComments() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[!-- comment 1 --][foo [!-- comment 2 --]}FOO[!-- comment 3 --]BAR{foo]";
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      // Comments are ignored, so:
      // We expect a Document
      // - with one text node
      // - with one range on it
      TAGDocument document = store.createDocument();
      TAGMarkup m1 = store.createMarkup(document, "foo");
      TAGTextNode t1 = store.createTextNode("FOOBAR");
//      m1.addTextNode(t1);
      document.addTextNode(t1, null);
      String layerName = "";
      document.associateTextNodeWithMarkupForLayer(t1, m1, layerName);
      document.addMarkup(m1);
      logTAGML(actual);
      compareTAGML(document, actual);
    });
  }

  @Test
  public void testUnclosedRangeThrowsSyntaxError() {
    store.runInTransaction(() -> {
      String input = "[tag} tag [v}is{v] not closed";
      try {
        TAGDocument actual = new LMNLImporter(store).importLMNL(input);
        fail("no LMNLSyntaxError thrown");
      } catch (LMNLSyntaxError e) {
        assertThat(e.getMessage()).contains("Unclosed LMNL range(s): [tag}");
      }
    });
  }

  @Test
  public void testUnopenedRangeThrowsSyntaxError() {
    store.runInTransaction(() -> {
      String input = "text{lmnl]";
      try {
        new LMNLImporter(store).importLMNL(input);
        fail("no LMNLSyntaxError thrown");
      } catch (LMNLSyntaxError e) {
        assertThat(e.getMessage()).contains("Closing tag {lmnl] found without corresponding open tag.");
      }
    });
  }

  @Test
  public void testSyntaxError() {
    store.runInTransaction(() -> {
      String input = "[a}bla{b]";
      try {
        new LMNLImporter(store).importLMNL(input);
        fail("no LMNLSyntaxError thrown");
      } catch (LMNLSyntaxError e) {
        assertThat(e.getMessage()).contains("Unclosed LMNL range(s): [a}");
        assertThat(e.getMessage()).contains("Closing tag {b] found without corresponding open tag.");
      }
    });
  }

  @Test
  public void testAcrosticFileThrowsSyntaxError() throws IOException {
    String pathname = "data/lmnl/acrostic-syntax-error.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    store.runInTransaction(() -> {
      try {
        new LMNLImporter(store).importLMNL(input);
        fail("no LMNLSyntaxError thrown");
      } catch (LMNLSyntaxError e) {
        assertThat(e.getMessage()).contains("Unclosed LMNL range(s): [H}, [name}, [T}, [name}, [lizabeth}, [name=a}");
      }
    });
  }

//  private void compareTAGML(String pathname, DocumentWrapper actual) throws IOException {
//    String inLMNL = FileUtils.readFileToString(new File(pathname), "UTF-8");
//    String outTAGML = tagmlExporter.asTAGML(actual);
//    assertThat(outTAGML).isEqualTo(inLMNL);
//  }

  private void compareTAGML(TAGDocument expected, TAGDocument actual) {
    String expectedTAGML = tagmlExporter.asTAGML(expected);
    String actualTAGML = tagmlExporter.asTAGML(actual);
    assertThat(actualTAGML).isEqualTo(expectedTAGML);
  }

  private AnnotationInfo simpleAnnotation(String tag) {
    return null;
//    return store.createAnnotation(tag);
  }

  private AnnotationInfo simpleAnnotation(String tag, String content) {
    //    TAGDocument annotationDocument = a1.getDocument();
//    TAGTextNode annotationText = store.createTextNode(content);
//    annotationDocument.addTextNode(annotationText);
    return simpleAnnotation(tag);
  }

  private void assertActualMatchesExpected(TAGDocument actual, TAGDocument expected) {
    List<TAGMarkup> actualMarkupList = actual.getMarkupStream().collect(toList());
    List<TAGTextNode> actualTextNodeList = actual.getTextNodeStream().collect(toList());

    List<TAGMarkup> expectedMarkupList = expected.getMarkupStream().collect(toList());
    List<TAGTextNode> expectedTextNodeList = expected.getTextNodeStream().collect(toList());

    assertThat(actualTextNodeList).hasSize(expectedTextNodeList.size());
    for (int i = 0; i < expectedTextNodeList.size(); i++) {
      TAGTextNode actualTextNode = actualTextNodeList.get(i);
      TAGTextNode expectedTextNode = expectedTextNodeList.get(i);
      Comparator<TAGTextNode> textNodeComparator = Comparator.comparing(TAGTextNode::getText);
      assertThat(actualTextNode).usingComparator(textNodeComparator).isEqualTo(expectedTextNode);
    }

    assertThat(actualMarkupList).hasSize(expectedMarkupList.size());
    for (int i = 0; i < expectedMarkupList.size(); i++) {
      TAGMarkup actualMarkup = actualMarkupList.get(i);
      TAGMarkup expectedMarkup = expectedMarkupList.get(i);
      assertThat(actualMarkup.getTag()).isEqualTo(expectedMarkup.getTag());
      Comparator<TAGMarkup> markupComparator = Comparator.comparing(TAGMarkup::getTag);
      assertThat(actualMarkup).usingComparator(markupComparator).isEqualTo(expectedMarkup);
    }

    String actualTAGML = tagmlExporter.asTAGML(actual);
    String expectedTAGML = tagmlExporter.asTAGML(expected);
    LOG.info("TAGML={}", actualTAGML);
    assertThat(actualTAGML).isEqualTo(expectedTAGML);
    // assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  private boolean compareDocuments(TAGDocument expected, TAGDocument actual) {
    Iterator<TAGTextNodeDTO> i1 = expected.getTextNodeIterator();
    Iterator<TAGTextNodeDTO> i2 = actual.getTextNodeIterator();
    boolean result = true;
    while (i1.hasNext() && result) {
      TAGTextNodeDTO t1 = i1.next();
      TAGTextNodeDTO t2 = i2.next();
      result = compareTextNodes(t1, t2);
    }
    return result;
  }

  private boolean compareTextNodes(TAGTextNodeDTO t1, TAGTextNodeDTO t2) {
    return t1.getText().equals(t2.getText());
  }

  private void logTAGML(TAGDocument document) {
    LOG.info("TAGML=\n{}", tagmlExporter.asTAGML(document));
  }

  private void logKdTree(TAGDocument document) {
    LaTeXExporter latexExporter = new LaTeXExporter(store, document);
    String latex1 = latexExporter.exportMatrix();
    LOG.info("matrix=\n{}", latex1);
    String latexKdTree = latexExporter.exportKdTree();
    LOG.info("latex tree=\n{}", latexKdTree);
  }

}
