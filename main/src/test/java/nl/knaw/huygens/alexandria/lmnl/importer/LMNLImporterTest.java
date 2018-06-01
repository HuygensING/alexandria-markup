package nl.knaw.huygens.alexandria.lmnl.importer;

/*
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

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.data_model.IndexPoint;
import nl.knaw.huygens.alexandria.data_model.NodeRangeIndex;
import nl.knaw.huygens.alexandria.exporter.LaTeXExporter;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
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
      TAGDocument expected = store.createDocumentWrapper();
      TAGMarkup r1 = store.createMarkupWrapper(expected, "l");
      TAGAnnotation a1 = simpleAnnotation("n", "144");
      r1.addAnnotation(a1);
      TAGTextNode t1 = store.createTextNodeWrapper("He manages to keep the upper hand");
      r1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(r1);
      expected.associateTextNodeWithMarkup(t1, r1);

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

      TAGDocument expected = store.createDocumentWrapper();

      TAGTextNode tn00 = store.createTextNodeWrapper("\n");
      TAGTextNode tn01 = store.createTextNodeWrapper("He manages to keep the upper hand").addPreviousTextNode(tn00);
      TAGTextNode tn02 = store.createTextNodeWrapper("\n").addPreviousTextNode(tn01);
      TAGTextNode tn03 = store.createTextNodeWrapper("On his own farm.").addPreviousTextNode(tn02);
      TAGTextNode tn04 = store.createTextNodeWrapper(" ").addPreviousTextNode(tn03);
      TAGTextNode tn05 = store.createTextNodeWrapper("He's boss.").addPreviousTextNode(tn04);
      TAGTextNode tn06 = store.createTextNodeWrapper(" ").addPreviousTextNode(tn05);
      TAGTextNode tn07 = store.createTextNodeWrapper("But as to hens:").addPreviousTextNode(tn06);
      TAGTextNode tn08 = store.createTextNodeWrapper("\n").addPreviousTextNode(tn07);
      TAGTextNode tn09 = store.createTextNodeWrapper("We fence our flowers in and the hens range.").addPreviousTextNode(tn08);
      TAGTextNode tn10 = store.createTextNodeWrapper("\n").addPreviousTextNode(tn09);

      TAGAnnotation date = simpleAnnotation("date", "1915");
      TAGAnnotation title = simpleAnnotation("title", "The Housekeeper");
      TAGAnnotation source = simpleAnnotation("source").addAnnotation(date).addAnnotation(title);

      TAGAnnotation name = simpleAnnotation("name", "Robert Frost");
      TAGAnnotation dates = simpleAnnotation("dates", "1874-1963");
      TAGAnnotation author = simpleAnnotation("author").addAnnotation(name).addAnnotation(dates);

      TAGMarkup excerpt = store.createMarkupWrapper(expected, "excerpt")//
          .addAnnotation(source)//
          .addAnnotation(author)//
          .setFirstAndLastTextNode(tn00, tn10);
      // 3 sentences
      TAGMarkup s1 = store.createMarkupWrapper(expected, "s").setFirstAndLastTextNode(tn01, tn03);
      TAGMarkup s2 = store.createMarkupWrapper(expected, "s").setOnlyTextNode(tn05);
      TAGMarkup s3 = store.createMarkupWrapper(expected, "s").setFirstAndLastTextNode(tn07, tn09);

      // 3 lines
      TAGAnnotation n144 = simpleAnnotation("n", "144");
      TAGMarkup l1 = store.createMarkupWrapper(expected, "l").setOnlyTextNode(tn01).addAnnotation(n144);

      TAGAnnotation n145 = simpleAnnotation("n", "145");
      TAGMarkup l2 = store.createMarkupWrapper(expected, "l").setFirstAndLastTextNode(tn03, tn07).addAnnotation(n145);

      TAGAnnotation n146 = simpleAnnotation("n", "146");
      TAGMarkup l3 = store.createMarkupWrapper(expected, "l").setOnlyTextNode(tn09).addAnnotation(n146);

      expected.setFirstAndLastTextNode(tn00, tn10)//
          .addMarkup(excerpt)//
          .addMarkup(s1)//
          .addMarkup(l1)//
          .addMarkup(l2)//
          .addMarkup(s2)//
          .addMarkup(s3)//
          .addMarkup(l3);
      expected.associateTextNodeWithMarkup(tn00, excerpt);
      expected.associateTextNodeWithMarkup(tn01, excerpt);
      expected.associateTextNodeWithMarkup(tn02, excerpt);
      expected.associateTextNodeWithMarkup(tn03, excerpt);
      expected.associateTextNodeWithMarkup(tn04, excerpt);
      expected.associateTextNodeWithMarkup(tn05, excerpt);
      expected.associateTextNodeWithMarkup(tn06, excerpt);
      expected.associateTextNodeWithMarkup(tn07, excerpt);
      expected.associateTextNodeWithMarkup(tn08, excerpt);
      expected.associateTextNodeWithMarkup(tn09, excerpt);
      expected.associateTextNodeWithMarkup(tn10, excerpt);

      expected.associateTextNodeWithMarkup(tn01, s1);
      expected.associateTextNodeWithMarkup(tn02, s1);
      expected.associateTextNodeWithMarkup(tn03, s1);

      expected.associateTextNodeWithMarkup(tn05, s2);

      expected.associateTextNodeWithMarkup(tn07, s3);
      expected.associateTextNodeWithMarkup(tn08, s3);
      expected.associateTextNodeWithMarkup(tn09, s3);

      expected.associateTextNodeWithMarkup(tn01, l1);

      expected.associateTextNodeWithMarkup(tn03, l2);
      expected.associateTextNodeWithMarkup(tn04, l2);
      expected.associateTextNodeWithMarkup(tn05, l2);
      expected.associateTextNodeWithMarkup(tn06, l2);
      expected.associateTextNodeWithMarkup(tn07, l2);

      expected.associateTextNodeWithMarkup(tn09, l3);

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

      List<TAGAnnotation> annotations = excerpt.getAnnotationStream().collect(toList());
      assertThat(annotations).hasSize(1); // just the soutce annotation;

      TAGAnnotation source = simpleAnnotation("source");
      TAGAnnotation book = simpleAnnotation("book", "1 Kings");
      source.addAnnotation(book);
      TAGAnnotation chapter = simpleAnnotation("chapter", "12");
      source.addAnnotation(chapter);
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
      TAGDocument expected = store.createDocumentWrapper();
      TAGMarkup m1 = store.createMarkupWrapper(expected, "lmnl");
      TAGAnnotation a1 = simpleAnnotation("a");
      TAGDocument annotationDocument = a1.getDocument();
      TAGTextNode at1 = store.createTextNodeWrapper("This is the ");
      TAGTextNode at2 = store.createTextNodeWrapper("annotation");
      TAGMarkup am1 = store.createMarkupWrapper(annotationDocument, "type").addTextNode(at2);
      TAGTextNode at3 = store.createTextNodeWrapper(" text");
      annotationDocument//
          .addTextNode(at1)//
          .addTextNode(at2)//
          .addTextNode(at3)//
          .addMarkup(am1);
      annotationDocument.associateTextNodeWithMarkup(at2, am1);
      m1.addAnnotation(a1);

      TAGTextNode t1 = store.createTextNodeWrapper("This is the main text");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();

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
  public void testAnnotationTextInAnnotationWithRanges() throws LMNLSyntaxError {
    String input = "[range1 [annotation1}[ra11}[ra12]{ra11]{annotation1]]";
    printTokens(input);
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      TAGDocument expected = store.createDocumentWrapper();

      TAGMarkup m1 = store.createMarkupWrapper(expected, "range1");
      TAGAnnotation a1 = simpleAnnotation("annotation1");
      TAGDocument annotationDocument = a1.getDocument();
      TAGTextNode at1 = store.createTextNodeWrapper("");
      TAGMarkup ar11 = store.createMarkupWrapper(annotationDocument, "ra11").addTextNode(at1);
      TAGMarkup ar12 = store.createMarkupWrapper(annotationDocument, "ra12").addTextNode(at1);
      annotationDocument//
          .addTextNode(at1)//
          .addMarkup(ar11)//
          .addMarkup(ar12);
      m1.addAnnotation(a1);
      annotationDocument.associateTextNodeWithMarkup(at1, ar11);
      annotationDocument.associateTextNodeWithMarkup(at1, ar12);

      TAGTextNode t1 = store.createTextNodeWrapper("");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Ignore
  @Test
  public void testAnonymousAnnotationRangeOpener() throws LMNLSyntaxError {
    String input = "[range1 [}annotation text{]}bla{range1]";
    printTokens(input);
    store.runInTransaction(() -> {
      TAGDocument actual = new LMNLImporter(store).importLMNL(input);

      TAGDocument expected = store.createDocumentWrapper();

      TAGMarkup m1 = store.createMarkupWrapper(expected, "range1");
      TAGAnnotation a1 = simpleAnnotation("", "annotation text");
      m1.addAnnotation(a1);

      TAGTextNode t1 = store.createTextNodeWrapper("bla");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

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

      TAGDocument expected = store.createDocumentWrapper();

      TAGMarkup m1 = store.createMarkupWrapper(expected, "r");
      TAGTextNode t1 = store.createTextNodeWrapper("Splitting the .");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

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
      TAGDocument expected = store.createDocumentWrapper();
      TAGMarkup m1 = store.createMarkupWrapper(expected, "empty");
      TAGTextNode t1 = store.createTextNodeWrapper("");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

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
      TAGDocument document = store.createDocumentWrapper();
      TAGMarkup m1 = store.createMarkupWrapper(document, "foo");
      TAGTextNode t1 = store.createTextNodeWrapper("FOOBAR");
      m1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.associateTextNodeWithMarkup(t1, m1);
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

  private TAGAnnotation simpleAnnotation(String tag) {
    return store.createAnnotationWrapper(tag);
  }

  private TAGAnnotation simpleAnnotation(String tag, String content) {
    TAGAnnotation a1 = simpleAnnotation(tag);
    TAGDocument annotationDocument = a1.getDocument();
    TAGTextNode annotationText = store.createTextNodeWrapper(content);
    annotationDocument.setOnlyTextNode(annotationText);
    return a1;
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
      Comparator<TAGTextNode> textNodeWrapperComparator = Comparator.comparing(TAGTextNode::getText);
      assertThat(actualTextNode).usingComparator(textNodeWrapperComparator).isEqualTo(expectedTextNode);
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

  private void logTAGML(TAGDocument TAGDocument) {
    LOG.info("TAGML=\n{}", tagmlExporter.asTAGML(TAGDocument));
  }

  private void logKdTree(TAGDocument TAGDocument) {
    LaTeXExporter latexExporter = new LaTeXExporter(store, TAGDocument);
    String latex1 = latexExporter.exportMatrix();
    LOG.info("matrix=\n{}", latex1);
    String latexKdTree = latexExporter.exportKdTree();
    LOG.info("latex tree=\n{}", latexKdTree);
  }

}
