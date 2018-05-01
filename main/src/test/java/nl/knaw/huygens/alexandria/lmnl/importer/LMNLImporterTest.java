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
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.apache.commons.io.FileUtils;
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

  @Test
  public void testMarkupAnnotation() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

      // Expectations:
      // We expect a Document
      // - with one text node
      // - with one range on it
      // - with one annotation on it.
      DocumentWrapper expected = store.createDocumentWrapper();
      MarkupWrapper r1 = store.createMarkupWrapper(expected, "l");
      AnnotationWrapper a1 = simpleAnnotation("n", "144");
      r1.addAnnotation(a1);
      TextNodeWrapper t1 = store.createTextNodeWrapper("He manages to keep the upper hand");
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
      DocumentWrapper actual = importer.importLMNL(input);

      DocumentWrapper expected = store.createDocumentWrapper();

      TextNodeWrapper tn00 = store.createTextNodeWrapper("\n");
      TextNodeWrapper tn01 = store.createTextNodeWrapper("He manages to keep the upper hand").addPreviousTextNode(tn00);
      TextNodeWrapper tn02 = store.createTextNodeWrapper("\n").addPreviousTextNode(tn01);
      TextNodeWrapper tn03 = store.createTextNodeWrapper("On his own farm.").addPreviousTextNode(tn02);
      TextNodeWrapper tn04 = store.createTextNodeWrapper(" ").addPreviousTextNode(tn03);
      TextNodeWrapper tn05 = store.createTextNodeWrapper("He's boss.").addPreviousTextNode(tn04);
      TextNodeWrapper tn06 = store.createTextNodeWrapper(" ").addPreviousTextNode(tn05);
      TextNodeWrapper tn07 = store.createTextNodeWrapper("But as to hens:").addPreviousTextNode(tn06);
      TextNodeWrapper tn08 = store.createTextNodeWrapper("\n").addPreviousTextNode(tn07);
      TextNodeWrapper tn09 = store.createTextNodeWrapper("We fence our flowers in and the hens range.").addPreviousTextNode(tn08);
      TextNodeWrapper tn10 = store.createTextNodeWrapper("\n").addPreviousTextNode(tn09);

      AnnotationWrapper date = simpleAnnotation("date", "1915");
      AnnotationWrapper title = simpleAnnotation("title", "The Housekeeper");
      AnnotationWrapper source = simpleAnnotation("source").addAnnotation(date).addAnnotation(title);

      AnnotationWrapper name = simpleAnnotation("name", "Robert Frost");
      AnnotationWrapper dates = simpleAnnotation("dates", "1874-1963");
      AnnotationWrapper author = simpleAnnotation("author").addAnnotation(name).addAnnotation(dates);

      MarkupWrapper excerpt = store.createMarkupWrapper(expected, "excerpt")//
          .addAnnotation(source)//
          .addAnnotation(author)//
          .setFirstAndLastTextNode(tn00, tn10);
      // 3 sentences
      MarkupWrapper s1 = store.createMarkupWrapper(expected, "s").setFirstAndLastTextNode(tn01, tn03);
      MarkupWrapper s2 = store.createMarkupWrapper(expected, "s").setOnlyTextNode(tn05);
      MarkupWrapper s3 = store.createMarkupWrapper(expected, "s").setFirstAndLastTextNode(tn07, tn09);

      // 3 lines
      AnnotationWrapper n144 = simpleAnnotation("n", "144");
      MarkupWrapper l1 = store.createMarkupWrapper(expected, "l").setOnlyTextNode(tn01).addAnnotation(n144);

      AnnotationWrapper n145 = simpleAnnotation("n", "145");
      MarkupWrapper l2 = store.createMarkupWrapper(expected, "l").setFirstAndLastTextNode(tn03, tn07).addAnnotation(n145);

      AnnotationWrapper n146 = simpleAnnotation("n", "146");
      MarkupWrapper l3 = store.createMarkupWrapper(expected, "l").setOnlyTextNode(tn09).addAnnotation(n146);

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
      DocumentWrapper actual = importer.importLMNL(input);

      LOG.info("document={}", actual);

      logTAGML(actual);

      List<MarkupWrapper> actualMarkupList = actual.getMarkupStream().collect(toList());

      MarkupWrapper excerpt = actualMarkupList.get(0);
      assertThat(excerpt.getTag()).isEqualTo("excerpt");

      List<AnnotationWrapper> annotations = excerpt.getAnnotationStream().collect(toList());
      assertThat(annotations).hasSize(1); // just the soutce annotation;

      AnnotationWrapper source = simpleAnnotation("source");
      AnnotationWrapper book = simpleAnnotation("book", "1 Kings");
      source.addAnnotation(book);
      AnnotationWrapper chapter = simpleAnnotation("chapter", "12");
      source.addAnnotation(chapter);
      String actualSourceTAGML = tagmlExporter.toTAGML(annotations.get(0)).toString();
      String expectedSourceTAGML = tagmlExporter.toTAGML(source).toString();
      assertThat(actualSourceTAGML).isEqualTo(expectedSourceTAGML);

      MarkupWrapper q1 = actualMarkupList.get(2);
      assertThat(q1.getTag()).isEqualTo("q"); // first q
      assertThat(q1.getTextNodeStream()).hasSize(2); // has 2 textnodes

      MarkupWrapper q2 = actualMarkupList.get(3);
      assertThat(q2.getTag()).isEqualTo("q"); // second q, nested in first
      assertThat(q2.getTextNodeStream()).hasSize(1); // has 1 textnode

      // compareTAGML(pathname, actual);
      logKdTree(actual);

      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Test
  public void testLMNLOzymandias() throws IOException, LMNLSyntaxError {
    String pathname = "data/lmnl/ozymandias-voices-wap.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    store.runInTransaction(() -> {
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);
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

  @Test
  public void testDiscontinuousRanges() throws LMNLSyntaxError {
    String input = "'[e [n}1{]}Ai,{e]' riep Piet, '[e [n}1{]}wat doe je, Mien?{e]'";
    store.runInTransaction(() -> {
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

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

  @Test
  public void testAnnotationTextWithRanges() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[lmnl [a}This is the [type}annotation{type] text{a]}This is the main text{lmnl]";
      printTokens(input);
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

      // Expectations:
      // We expect a Document
      // - with one text node
      // - with one range on it
      // - with one annotation on it.
      // - that has one range on it.
      DocumentWrapper expected = store.createDocumentWrapper();
      MarkupWrapper m1 = store.createMarkupWrapper(expected, "lmnl");
      AnnotationWrapper a1 = simpleAnnotation("a");
      DocumentWrapper annotationDocument = a1.getDocument();
      TextNodeWrapper at1 = store.createTextNodeWrapper("This is the ");
      TextNodeWrapper at2 = store.createTextNodeWrapper("annotation");
      MarkupWrapper am1 = store.createMarkupWrapper(annotationDocument, "type").addTextNode(at2);
      TextNodeWrapper at3 = store.createTextNodeWrapper(" text");
      annotationDocument//
          .addTextNode(at1)//
          .addTextNode(at2)//
          .addTextNode(at3)//
          .addMarkup(am1);
      annotationDocument.associateTextNodeWithMarkup(at2, am1);
      m1.addAnnotation(a1);

      TextNodeWrapper t1 = store.createTextNodeWrapper("This is the main text");
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


  @Test
  public void testAnnotationTextInAnnotationWithRanges() throws LMNLSyntaxError {
    String input = "[range1 [annotation1}[ra11}[ra12]{ra11]{annotation1]]";
    printTokens(input);
    store.runInTransaction(() -> {
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

      DocumentWrapper expected = store.createDocumentWrapper();

      MarkupWrapper m1 = store.createMarkupWrapper(expected, "range1");
      AnnotationWrapper a1 = simpleAnnotation("annotation1");
      DocumentWrapper annotationDocument = a1.getDocument();
      TextNodeWrapper at1 = store.createTextNodeWrapper("");
      MarkupWrapper ar11 = store.createMarkupWrapper(annotationDocument, "ra11").addTextNode(at1);
      MarkupWrapper ar12 = store.createMarkupWrapper(annotationDocument, "ra12").addTextNode(at1);
      annotationDocument//
          .addTextNode(at1)//
          .addMarkup(ar11)//
          .addMarkup(ar12);
      m1.addAnnotation(a1);
      annotationDocument.associateTextNodeWithMarkup(at1, ar11);
      annotationDocument.associateTextNodeWithMarkup(at1, ar12);

      TextNodeWrapper t1 = store.createTextNodeWrapper("");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Test
  public void testAnonymousAnnotationRangeOpener() throws LMNLSyntaxError {
    String input = "[range1 [}annotation text{]}bla{range1]";
    printTokens(input);
    store.runInTransaction(() -> {
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

      DocumentWrapper expected = store.createDocumentWrapper();

      MarkupWrapper m1 = store.createMarkupWrapper(expected, "range1");
      AnnotationWrapper a1 = simpleAnnotation("", "annotation text");
      m1.addAnnotation(a1);

      TextNodeWrapper t1 = store.createTextNodeWrapper("bla");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Test
  public void testAtomsAreIgnored() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[r}Splitting the {{Atom}}.{r]";
      printTokens(input);
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

      DocumentWrapper expected = store.createDocumentWrapper();

      MarkupWrapper m1 = store.createMarkupWrapper(expected, "r");
      TextNodeWrapper t1 = store.createTextNodeWrapper("Splitting the .");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

      logTAGML(actual);
      compareTAGML(expected, actual);
    });
  }

  @Test
  public void testEmptyRange() throws LMNLSyntaxError {
    String input = "[empty}{empty]";
    printTokens(input);
    store.runInTransaction(() -> {
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);
      DocumentWrapper expected = store.createDocumentWrapper();
      MarkupWrapper m1 = store.createMarkupWrapper(expected, "empty");
      TextNodeWrapper t1 = store.createTextNodeWrapper("");
      m1.setOnlyTextNode(t1);
      expected.setOnlyTextNode(t1);
      expected.addMarkup(m1);
      expected.associateTextNodeWithMarkup(t1, m1);

      logTAGML(actual);
      compareTAGML(expected, actual);
      assertThat(compareDocuments(expected, actual)).isTrue();
    });
  }

  @Test
  public void testComments() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[!-- comment 1 --][foo [!-- comment 2 --]}FOO[!-- comment 3 --]BAR{foo]";
      DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);

      // Comments are ignored, so:
      // We expect a Document
      // - with one text node
      // - with one range on it
      DocumentWrapper document = store.createDocumentWrapper();
      MarkupWrapper m1 = store.createMarkupWrapper(document, "foo");
      TextNodeWrapper t1 = store.createTextNodeWrapper("FOOBAR");
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
        DocumentWrapper actual = new LMNLImporter(store).importLMNL(input);
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

  private void compareTAGML(DocumentWrapper expected, DocumentWrapper actual) {
    String expectedTAGML = tagmlExporter.asTAGML(expected);
    String actualTAGML = tagmlExporter.asTAGML(actual);
    assertThat(actualTAGML).isEqualTo(expectedTAGML);
  }

  private AnnotationWrapper simpleAnnotation(String tag) {
    return store.createAnnotationWrapper(tag);
  }

  private AnnotationWrapper simpleAnnotation(String tag, String content) {
    AnnotationWrapper a1 = simpleAnnotation(tag);
    DocumentWrapper annotationDocument = a1.getDocument();
    TextNodeWrapper annotationText = store.createTextNodeWrapper(content);
    annotationDocument.setOnlyTextNode(annotationText);
    return a1;
  }

  private void assertActualMatchesExpected(DocumentWrapper actual, DocumentWrapper expected) {
    List<MarkupWrapper> actualMarkupList = actual.getMarkupStream().collect(toList());
    List<TextNodeWrapper> actualTextNodeList = actual.getTextNodeStream().collect(toList());

    List<MarkupWrapper> expectedMarkupList = expected.getMarkupStream().collect(toList());
    List<TextNodeWrapper> expectedTextNodeList = expected.getTextNodeStream().collect(toList());

    assertThat(actualTextNodeList).hasSize(expectedTextNodeList.size());
    for (int i = 0; i < expectedTextNodeList.size(); i++) {
      TextNodeWrapper actualTextNode = actualTextNodeList.get(i);
      TextNodeWrapper expectedTextNode = expectedTextNodeList.get(i);
      Comparator<TextNodeWrapper> textNodeWrapperComparator = Comparator.comparing(TextNodeWrapper::getText);
      assertThat(actualTextNode).usingComparator(textNodeWrapperComparator).isEqualTo(expectedTextNode);
    }

    assertThat(actualMarkupList).hasSize(expectedMarkupList.size());
    for (int i = 0; i < expectedMarkupList.size(); i++) {
      MarkupWrapper actualMarkup = actualMarkupList.get(i);
      MarkupWrapper expectedMarkup = expectedMarkupList.get(i);
      assertThat(actualMarkup.getTag()).isEqualTo(expectedMarkup.getTag());
      Comparator<MarkupWrapper> markupComparator = Comparator.comparing(MarkupWrapper::getTag);
      assertThat(actualMarkup).usingComparator(markupComparator).isEqualTo(expectedMarkup);
    }

    String actualTAGML = tagmlExporter.asTAGML(actual);
    String expectedTAGML = tagmlExporter.asTAGML(expected);
    LOG.info("TAGML={}", actualTAGML);
    assertThat(actualTAGML).isEqualTo(expectedTAGML);
    // assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  private boolean compareDocuments(DocumentWrapper expected, DocumentWrapper actual) {
    Iterator<TAGTextNode> i1 = expected.getTextNodeIterator();
    Iterator<TAGTextNode> i2 = actual.getTextNodeIterator();
    boolean result = true;
    while (i1.hasNext() && result) {
      TAGTextNode t1 = i1.next();
      TAGTextNode t2 = i2.next();
      result = compareTextNodes(t1, t2);
    }
    return result;
  }

  private boolean compareTextNodes(TAGTextNode t1, TAGTextNode t2) {
    return t1.getText().equals(t2.getText());
  }

  private void logTAGML(DocumentWrapper documentWrapper) {
    LOG.info("TAGML=\n{}", tagmlExporter.asTAGML(documentWrapper));
  }

  private void logKdTree(DocumentWrapper documentWrapper) {
    LaTeXExporter latexExporter = new LaTeXExporter(store, documentWrapper);
    String latex1 = latexExporter.exportMatrix();
    LOG.info("matrix=\n{}", latex1);
    String latexKdTree = latexExporter.exportKdTree();
    LOG.info("latex tree=\n{}", latexKdTree);
  }

}
