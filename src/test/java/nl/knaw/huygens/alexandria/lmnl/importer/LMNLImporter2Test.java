package nl.knaw.huygens.alexandria.lmnl.importer;


/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import static java.util.stream.Collectors.toList;
import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.IndexPoint;
import nl.knaw.huygens.alexandria.lmnl.data_model.NodeRangeIndex2;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter2;
import nl.knaw.huygens.alexandria.lmnl.exporter.LaTeXExporter;
import nl.knaw.huygens.alexandria.lmnl.exporter.LaTeXExporter2;
import nl.knaw.huygens.alexandria.lmnl.storage.TAGStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGMarkup;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.TextNodeWrapper;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class LMNLImporter2Test extends AlexandriaLMNLBaseTest {
  private static Path tmpDir;
  final Logger LOG = LoggerFactory.getLogger(LMNLImporter2Test.class);
  static TAGStore store;
  static LMNLExporter2 lmnlExporter;

  @BeforeClass
  public static void beforeClass() throws IOException {
    tmpDir = Files.createTempDirectory("tmpDir");
    tmpDir.toFile().deleteOnExit();
    store = new TAGStore(tmpDir.toString(), false);
    lmnlExporter = new LMNLExporter2(store).useShorthand();
  }

  @AfterClass
  public static void afterClass() {
    tmpDir.toFile().delete();
  }

  @Test
  public void testMarkupAnnotation() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
      TAGDocument tagDocument = new LMNLImporter2().importLMNL(input);
      DocumentWrapper actual = new DocumentWrapper(store, tagDocument);

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

      logLMNL(actual);
      assertTrue(compareDocuments(expected, actual));
      assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);

      logKdTree(actual);
      NodeRangeIndex2 index = new NodeRangeIndex2(store, actual.getDocument());
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

      LMNLImporter2 importer = new LMNLImporter2(store);
      TAGDocument tagDocument = importer.importLMNL(input);
      DocumentWrapper actual = new DocumentWrapper(store, tagDocument);

      DocumentWrapper expected = store.createDocumentWrapper();

      TextNodeWrapper tn00 = store.createTextNodeWrapper("\n");
      TextNodeWrapper tn01 = store.createTextNodeWrapper("He manages to keep the upper hand").setPreviousTextNode(tn00);
      TextNodeWrapper tn02 = store.createTextNodeWrapper("\n").setPreviousTextNode(tn01);
      TextNodeWrapper tn03 = store.createTextNodeWrapper("On his own farm.").setPreviousTextNode(tn02);
      TextNodeWrapper tn04 = store.createTextNodeWrapper(" ").setPreviousTextNode(tn03);
      TextNodeWrapper tn05 = store.createTextNodeWrapper("He's boss.").setPreviousTextNode(tn04);
      TextNodeWrapper tn06 = store.createTextNodeWrapper(" ").setPreviousTextNode(tn05);
      TextNodeWrapper tn07 = store.createTextNodeWrapper("But as to hens:").setPreviousTextNode(tn06);
      TextNodeWrapper tn08 = store.createTextNodeWrapper("\n").setPreviousTextNode(tn07);
      TextNodeWrapper tn09 = store.createTextNodeWrapper("We fence our flowers in and the hens range.").setPreviousTextNode(tn08);
      TextNodeWrapper tn10 = store.createTextNodeWrapper("\n").setPreviousTextNode(tn09);

      AnnotationWrapper date = simpleAnnotation("date", "1915");
      AnnotationWrapper title = simpleAnnotation("title", "The Housekeeper");
      AnnotationWrapper source = simpleAnnotation("source").addAnnotation(date).addAnnotation(title);
      AnnotationWrapper name = simpleAnnotation("name", "Robert Frost");
      AnnotationWrapper dates = simpleAnnotation("dates", "1874-1963");
      AnnotationWrapper author = simpleAnnotation("author").addAnnotation(name).addAnnotation(dates);
      AnnotationWrapper n144 = simpleAnnotation("n", "144");
      AnnotationWrapper n145 = simpleAnnotation("n", "145");
      AnnotationWrapper n146 = simpleAnnotation("n", "146");
      MarkupWrapper excerpt = store.createMarkupWrapper(expected, "excerpt").addAnnotation(source).addAnnotation(author).setFirstAndLastTextNode(tn00, tn10);
      // 3 sentences
      MarkupWrapper s1 = store.createMarkupWrapper(expected, "s").setFirstAndLastTextNode(tn01, tn03);
      MarkupWrapper s2 = store.createMarkupWrapper(expected, "s").setOnlyTextNode(tn05);
      MarkupWrapper s3 = store.createMarkupWrapper(expected, "s").setFirstAndLastTextNode(tn07, tn09);
      // 3 lines
      MarkupWrapper l1 = store.createMarkupWrapper(expected, "l").setOnlyTextNode(tn01).addAnnotation(n144);
      MarkupWrapper l2 = store.createMarkupWrapper(expected, "l").setFirstAndLastTextNode(tn03, tn07).addAnnotation(n145);
      MarkupWrapper l3 = store.createMarkupWrapper(expected, "l").setOnlyTextNode(tn09).addAnnotation(n146);

      expected.setFirstAndLastTextNode(tn00, tn10).addMarkup(excerpt).addMarkup(s1).addMarkup(l1).addMarkup(l2).addMarkup(s2).addMarkup(s3).addMarkup(l3);

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
      LMNLImporter2 importer = new LMNLImporter2(store);
      TAGDocument tagDocument = importer.importLMNL(input);
      DocumentWrapper actual = new DocumentWrapper(store, tagDocument);

      LOG.info("document={}", actual);

      logLMNL(actual);

      List<MarkupWrapper> actualMarkupList = actual.getMarkups().collect(toList());

      MarkupWrapper excerpt = actualMarkupList.get(0);
      assertThat(excerpt.getTag()).isEqualTo("excerpt");

      List<AnnotationWrapper> annotations = excerpt.getAnnotations().collect(toList());
      assertThat(annotations).hasSize(1); // just the soutce annotation;

      AnnotationWrapper source = simpleAnnotation("source");
      AnnotationWrapper book = simpleAnnotation("book", "1 Kings");
      source.addAnnotation(book);
      AnnotationWrapper chapter = simpleAnnotation("chapter", "12");
      source.addAnnotation(chapter);
      String actualSourceLMNL = lmnlExporter.toLMNL(annotations.get(0)).toString();
      String expectedSourceLMNL = lmnlExporter.toLMNL(source).toString();
      assertThat(actualSourceLMNL).isEqualTo(expectedSourceLMNL);

      TAGMarkup q1 = actualMarkupList.get(2);
      assertThat(q1.getTag()).isEqualTo("q"); // first q
      assertThat(q1.textNodes).hasSize(2); // has 2 textnodes

      TAGMarkup q2 = actualMarkupList.get(3);
      assertThat(q2.getTag()).isEqualTo("q"); // second q, nested in first
      assertThat(q2.textNodes).hasSize(1); // has 1 textnode

      // compareLMNL(pathname, actual);
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
      Document actual = new LMNLImporter().importLMNL(input);
      LOG.info("document={}", actual);
      logLMNL(actual);
      assertThat(actual.value().hasTextNodes()).isTrue();
      String lmnl = lmnlExporter.toLMNL(actual);
      assertThat(lmnl).startsWith("[sonneteer [id}ozymandias{] [encoding [resp}ebeshero{] [resp}wap{]]}"); // annotations from sonneteer endtag moved to start tag
      assertThat(lmnl).contains("[meta [author}Percy Bysshe Shelley{] [title}Ozymandias{]]"); // anonymous markup
      // compareLMNL(pathname, actual);

      logKdTree(actual);

      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Test
  public void testDiscontinuousRanges() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "'[e [n}1{]}Ai,{e]' riep Piet, '[e [n}1{]}wat doe je, Mien?{e]'";
      Document actual = new LMNLImporter().importLMNL(input);
      LOG.info("textNodes={}", actual.value().textNodeList);
      LOG.info("markups={}", actual.value().markupList);
      assertThat(actual.value().hasTextNodes()).isTrue();
      assertThat(actual.value().markupList).hasSize(1);

      String lmnl = lmnlExporter.toLMNL(actual);
      LOG.info("lmnl={}", lmnl);
      assertThat(lmnl).isEqualTo(input);

      LaTeXExporter latex = new LaTeXExporter(actual);
      LOG.info("matrix=\n{}", latex.exportMatrix());
      LOG.info("kdtree=\n{}", latex.exportKdTree());
    });
  }

  @Test
  public void testAnnotationTextWithRanges() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[lmnl [a}This is the [type}annotation{type] text{a]}This is the main text{lmnl]";
      printTokens(input);
      Document actual = new LMNLImporter().importLMNL(input);

      // Expectations:
      // We expect a Document
      // - with one text node
      // - with one range on it
      // - with one annotation on it.
      // - that has one range on it.
      Document expected = new Document();
      TAGDocument document = expected.value();

      TAGMarkup r1 = new TAGMarkup(document, "lmnl");
      // TAGAnnotation a1 = simpleAnnotation("a", "This is the [type}annotation{type] text");
      AnnotationWrapper a1 = simpleAnnotation("a");
      TAGDocument annotationLimen = a1.value();
      TAGTextNode at1 = new TAGTextNode("This is the ");
      TAGTextNode at2 = new TAGTextNode("annotation");
      TAGMarkup ar1 = new TAGMarkup(annotationLimen, "type").addTextNode(at2);
      TAGTextNode at3 = new TAGTextNode(" text");
      annotationLimen//
          .addTextNode(at1)//
          .addTextNode(at2)//
          .addTextNode(at3)//
          .addMarkup(ar1);
      r1.addAnnotation(a1);

      TAGTextNode t1 = new TAGTextNode("This is the main text");
      r1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.addMarkup(r1);

      logLMNL(actual);
      compareLMNL(expected, actual);
      assertTrue(compareDocuments(expected, actual));

      logKdTree(actual);
      NodeRangeIndex2 index = new NodeRangeIndex2(actual);
      List<IndexPoint> indexPoints = index.getIndexPoints();
      logKdTree(actual);
      List<IndexPoint> expectedIndexPoints = new ArrayList<>();
      expectedIndexPoints.add(new IndexPoint(0, 0));
      assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
    });
  }

  @Test
  public void testAnnotationTextInAnnotationWithRanges() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[range1 [annotation1}[ra11}[ra12]{ra11]{annotation1]]";
      printTokens(input);
      Document actual = new LMNLImporter().importLMNL(input);

      Document expected = new Document();
      TAGDocument document = expected.value();

      TAGMarkup r1 = new TAGMarkup(document, "range1");
      AnnotationWrapper a1 = simpleAnnotation("annotation1");
      TAGDocument annotationLimen = a1.value();
      TAGTextNode at1 = new TAGTextNode("");
      TAGMarkup ar11 = new TAGMarkup(annotationLimen, "ra11").addTextNode(at1);
      TAGMarkup ar12 = new TAGMarkup(annotationLimen, "ra12").addTextNode(at1);
      annotationLimen//
          .addTextNode(at1)//
          .addMarkup(ar11)//
          .addMarkup(ar12);
      r1.addAnnotation(a1);

      TAGTextNode t1 = new TAGTextNode("");
      r1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.addMarkup(r1);

      logLMNL(actual);
      compareLMNL(expected, actual);
      assertTrue(compareDocuments(expected, actual));
    });
  }

  @Test
  public void testAnonymousAnnotationRangeOpener() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[range1 [}annotation text{]}bla{range1]";
      printTokens(input);
      Document actual = new LMNLImporter().importLMNL(input);

      Document expected = new Document();
      TAGDocument document = expected.value();

      TAGMarkup r1 = new TAGMarkup(document, "range1");
      AnnotationWrapper a1 = simpleAnnotation("", "annotation text");
      r1.addAnnotation(a1);

      TAGTextNode t1 = new TAGTextNode("bla");
      r1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.addMarkup(r1);

      logLMNL(actual);
      compareLMNL(expected, actual);
      assertTrue(compareDocuments(expected, actual));
    });
  }

  @Test
  public void testAtomsAreIgnored() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[r}Splitting the {{Atom}}.{r]";
      printTokens(input);
      Document actual = new LMNLImporter().importLMNL(input);

      Document expected = new Document();
      TAGDocument document = expected.value();

      TAGMarkup r1 = new TAGMarkup(document, "r");
      TAGTextNode t1 = new TAGTextNode("Splitting the .");
      r1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.addMarkup(r1);

      logLMNL(actual);
      compareLMNL(expected, actual);
    });
  }

  @Test
  public void testEmptyRange() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[empty}{empty]";
      printTokens(input);
      Document actual = new LMNLImporter().importLMNL(input);

      Document expected = new Document();
      TAGDocument document = expected.value();

      TAGMarkup r1 = new TAGMarkup(document, "empty");
      TAGTextNode t1 = new TAGTextNode("");
      r1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.addMarkup(r1);

      logLMNL(actual);
      compareLMNL(expected, actual);
      assertTrue(compareDocuments(expected, actual));
    });
  }

  @Test
  public void testComments() throws LMNLSyntaxError {
    store.runInTransaction(() -> {
      String input = "[!-- comment 1 --][foo [!-- comment 2 --]}FOO[!-- comment 3 --]BAR{foo]";
      Document actual = new LMNLImporter().importLMNL(input);

      // Comments are ignored, so:
      // We expect a Document
      // - with one text node
      // - with one range on it
      Document expected = new Document();
      TAGDocument document = expected.value();
      TAGMarkup r1 = new TAGMarkup(document, "foo");
      TAGTextNode t1 = new TAGTextNode("FOOBAR");
      r1.setOnlyTextNode(t1);
      document.setOnlyTextNode(t1);
      document.addMarkup(r1);
      logLMNL(actual);
      compareLMNL(expected, actual);
    });
  }

  @Test
  public void testUnclosedRangeThrowsSyntaxError() {
    store.runInTransaction(() -> {
      String input = "[tag} tag [v}is{v] not closed";
      try {
        TAGDocument actual = new LMNLImporter2(store).importLMNL(input);
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
        TAGDocument actual = new LMNLImporter2(store).importLMNL(input);
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
        TAGDocument actual = new LMNLImporter2(store).importLMNL(input);
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
        TAGDocument actual = new LMNLImporter2(store).importLMNL(input);
        fail("no LMNLSyntaxError thrown");
      } catch (LMNLSyntaxError e) {
        assertThat(e.getMessage()).contains("Unclosed LMNL range(s): [H}, [name}, [T}, [name}, [lizabeth}, [name=a}");
      }
    });
  }

  private void compareLMNL(String pathname, Document actual) throws IOException {
    String inLMNL = FileUtils.readFileToString(new File(pathname), "UTF-8");
    String outLMNL = lmnlExporter.toLMNL(actual);
    assertThat(outLMNL).isEqualTo(inLMNL);
  }

  private void compareLMNL(Document expected, Document actual) {
    String expectedLMNL = lmnlExporter.toLMNL(expected);
    String actualLMNL = lmnlExporter.toLMNL(actual);
    assertThat(actualLMNL).isEqualTo(expectedLMNL);
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

  private void assertActualMatchesExpected(DocumentWrapper actualWrapper, DocumentWrapper expectedWrapper) {
    TAGDocument actual = actualWrapper.getDocument();
    TAGDocument expected = expectedWrapper.getDocument();
    List<TAGMarkup> actualMarkupList = actual.getMarkupIds().stream().map(store::getMarkup).collect(toList());
    List<TAGTextNode> actualTextNodeList = actual.getTextNodeIds().stream().map(store::getTextNode).collect(toList());

    List<TAGMarkup> expectedMarkupList = expected.getMarkupIds().stream().map(store::getMarkup).collect(toList());
    List<TAGTextNode> expectedTextNodeList = expected.getTextNodeIds().stream().map(store::getTextNode).collect(toList());

    assertThat(actualTextNodeList).hasSize(expectedTextNodeList.size());
    for (int i = 0; i < expectedTextNodeList.size(); i++) {
      TAGTextNode actualTextNode = actualTextNodeList.get(i);
      TAGTextNode expectedTextNode = expectedTextNodeList.get(i);
      assertThat(actualTextNode).isEqualToComparingFieldByFieldRecursively(expectedTextNode);
    }

    assertThat(actualMarkupList).hasSize(expectedMarkupList.size());
    for (int i = 0; i < expectedMarkupList.size(); i++) {
      TAGMarkup actualMarkup = actualMarkupList.get(i);
      TAGMarkup expectedMarkup = expectedMarkupList.get(i);
      assertThat(actualMarkup.getTag()).isEqualTo(expectedMarkup.getTag());
      Comparator<TAGMarkup> markupComparator = Comparator.comparing(TAGMarkup::getTag);
      assertThat(actualMarkup).usingComparator(markupComparator).isEqualTo(expectedMarkup);
    }

    String actualLMNL = lmnlExporter.toLMNL(actual);
    String expectedLMNL = lmnlExporter.toLMNL(expected);
    LOG.info("LMNL={}", actualLMNL);
    assertThat(actualLMNL).isEqualTo(expectedLMNL);
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

  private void logLMNL(DocumentWrapper documentWrapper) {
    LOG.info("LMNL=\n{}", lmnlExporter.toLMNL(documentWrapper.getDocument()));
  }

  private void logKdTree(DocumentWrapper documentWrapper) {
    LaTeXExporter2 latexExporter = new LaTeXExporter2(store, documentWrapper.getDocument());
    String latex1 = latexExporter.exportMatrix();
    LOG.info("matrix=\n{}", latex1);
    String latexKdTree = latexExporter.exportKdTree();
    LOG.info("latex tree=\n{}", latexKdTree);
  }

}
