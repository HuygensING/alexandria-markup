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


import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.lmnl.data_model.*;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.exporter.LaTeXExporter;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class LMNLImporterTest extends AlexandriaLMNLBaseTest {
  final Logger LOG = LoggerFactory.getLogger(LMNLImporterTest.class);
  final LMNLExporter lmnlExporter = new LMNLExporter().useShorthand();

  @Test
  public void testMarkupAnnotation() {
    String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
    Document actual = new LMNLImporter().importLMNL(input);

    // Expectations:
    // We expect a Document
    // - with one text node
    // - with one range on it
    // - with one annotation on it.
    Document expected = new Document();
    Limen limen = expected.value();
    Markup r1 = new Markup(limen, "l");
    Annotation a1 = simpleAnnotation("n", "144");
    r1.addAnnotation(a1);
    TextNode t1 = new TextNode("He manages to keep the upper hand");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    assertTrue(compareDocuments(expected, actual));
    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);

    logKdTree(actual);
    NodeRangeIndex index = new NodeRangeIndex(actual.value());
    List<IndexPoint> indexPoints = index.getIndexPoints();
    logKdTree(actual);
    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testLexingComplex() {
    String input = "[excerpt\n"//
        + "  [source [date}1915{][title}The Housekeeper{]]\n"//
        + "  [author\n"//
        + "    [name}Robert Frost{]\n"//
        + "    [dates}1874-1963{]] }\n"//
        + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
        + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
        + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
        + "{excerpt]";

    LMNLImporter importer = new LMNLImporter();
    Document actual = importer.importLMNL(input);
    // Limen value = actual.value();

    // Markup markup = new Markup(value, "excerpt");
    // assertThat(value.markupList).hasSize(7);
    // List<Markup> markupList = value.markupList;
    //
    // markupList.stream().map(Markup::getTag).map(t -> "[" + t + "}").forEach(System.out::print);
    // Markup markup1 = markupList.get(0);
    // assertThat(markup1.getTag()).isEqualTo("excerpt");
    //
    // Markup markup2 = markupList.get(1);
    // assertThat(markup2.getTag()).isEqualTo("s");
    //
    // Markup markup3 = markupList.get(2);
    // assertThat(markup3.getTag()).isEqualTo("l");

    Document expected = new Document();
    Limen limen = expected.value();

    TextNode tn00 = new TextNode("\n");
    TextNode tn01 = new TextNode("He manages to keep the upper hand").setPreviousTextNode(tn00);
    TextNode tn02 = new TextNode("\n").setPreviousTextNode(tn01);
    TextNode tn03 = new TextNode("On his own farm.").setPreviousTextNode(tn02);
    TextNode tn04 = new TextNode(" ").setPreviousTextNode(tn03);
    TextNode tn05 = new TextNode("He's boss.").setPreviousTextNode(tn04);
    TextNode tn06 = new TextNode(" ").setPreviousTextNode(tn05);
    TextNode tn07 = new TextNode("But as to hens:").setPreviousTextNode(tn06);
    TextNode tn08 = new TextNode("\n").setPreviousTextNode(tn07);
    TextNode tn09 = new TextNode("We fence our flowers in and the hens range.").setPreviousTextNode(tn08);
    TextNode tn10 = new TextNode("\n").setPreviousTextNode(tn09);

    Annotation date = simpleAnnotation("date", "1915");
    Annotation title = simpleAnnotation("title", "The Housekeeper");
    Annotation source = simpleAnnotation("source").addAnnotation(date).addAnnotation(title);
    Annotation name = simpleAnnotation("name", "Robert Frost");
    Annotation dates = simpleAnnotation("dates", "1874-1963");
    Annotation author = simpleAnnotation("author").addAnnotation(name).addAnnotation(dates);
    Annotation n144 = simpleAnnotation("n", "144");
    Annotation n145 = simpleAnnotation("n", "145");
    Annotation n146 = simpleAnnotation("n", "146");
    Markup excerpt = new Markup(limen, "excerpt").addAnnotation(source).addAnnotation(author).setFirstAndLastTextNode(tn00, tn10);
    // 3 sentences
    Markup s1 = new Markup(limen, "s").setFirstAndLastTextNode(tn01, tn03);
    Markup s2 = new Markup(limen, "s").setOnlyTextNode(tn05);
    Markup s3 = new Markup(limen, "s").setFirstAndLastTextNode(tn07, tn09);
    // 3 lines
    Markup l1 = new Markup(limen, "l").setOnlyTextNode(tn01).addAnnotation(n144);
    Markup l2 = new Markup(limen, "l").setFirstAndLastTextNode(tn03, tn07).addAnnotation(n145);
    Markup l3 = new Markup(limen, "l").setOnlyTextNode(tn09).addAnnotation(n146);

    limen.setFirstAndLastTextNode(tn00, tn10).addMarkup(excerpt).addMarkup(s1).addMarkup(l1).addMarkup(l2).addMarkup(s2).addMarkup(s3).addMarkup(l3);

    assertActualMatchesExpected(actual, expected);

    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    logKdTree(actual);
    // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testLMNL1kings12() throws IOException {
    String pathname = "data/lmnl/1kings12.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", actual);

    logLMNL(actual);

    Limen actualLimen = actual.value();
    List<Markup> actualMarkupList = actualLimen.markupList;

    Markup excerpt = actualMarkupList.get(0);
    assertThat(excerpt.getTag()).isEqualTo("excerpt");

    List<Annotation> annotations = excerpt.getAnnotations();
    assertThat(annotations).hasSize(1); // just the soutce annotation;

    Annotation source = simpleAnnotation("source");
    Annotation book = simpleAnnotation("book", "1 Kings");
    source.addAnnotation(book);
    Annotation chapter = simpleAnnotation("chapter", "12");
    source.addAnnotation(chapter);
    String actualSourceLMNL = lmnlExporter.toLMNL(annotations.get(0)).toString();
    String expectedSourceLMNL = lmnlExporter.toLMNL(source).toString();
    assertThat(actualSourceLMNL).isEqualTo(expectedSourceLMNL);

    Markup q1 = actualMarkupList.get(2);
    assertThat(q1.getTag()).isEqualTo("q"); // first q
    assertThat(q1.textNodes).hasSize(2); // has 2 textnodes

    Markup q2 = actualMarkupList.get(3);
    assertThat(q2.getTag()).isEqualTo("q"); // second q, nested in first
    assertThat(q2.textNodes).hasSize(1); // has 1 textnode

    // compareLMNL(pathname, actual);
    logKdTree(actual);

    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testLMNLOzymandias() throws IOException {
    String pathname = "data/lmnl/ozymandias-voices-wap.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
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
  }

  @Test
  public void testDiscontinuousRanges() {
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
  }

  @Test
  public void testAnnotationTextWithRanges() {
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
    Limen limen = expected.value();

    Markup r1 = new Markup(limen, "lmnl");
    // Annotation a1 = simpleAnnotation("a", "This is the [type}annotation{type] text");
    Annotation a1 = simpleAnnotation("a");
    Limen annotationLimen = a1.value();
    TextNode at1 = new TextNode("This is the ");
    TextNode at2 = new TextNode("annotation");
    Markup ar1 = new Markup(annotationLimen, "type").addTextNode(at2);
    TextNode at3 = new TextNode(" text");
    annotationLimen//
        .addTextNode(at1)//
        .addTextNode(at2)//
        .addTextNode(at3)//
        .addMarkup(ar1);
    r1.addAnnotation(a1);

    TextNode t1 = new TextNode("This is the main text");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    compareLMNL(expected, actual);
    assertTrue(compareDocuments(expected, actual));

    logKdTree(actual);
    NodeRangeIndex index = new NodeRangeIndex(actual.value());
    List<IndexPoint> indexPoints = index.getIndexPoints();
    logKdTree(actual);
    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testAnnotationTextInAnnotationWithRanges() {
    String input = "[range1 [annotation1}[ra11}[ra12]{ra11]{annotation1]]";
    printTokens(input);
    Document actual = new LMNLImporter().importLMNL(input);

    Document expected = new Document();
    Limen limen = expected.value();

    Markup r1 = new Markup(limen, "range1");
    Annotation a1 = simpleAnnotation("annotation1");
    Limen annotationLimen = a1.value();
    TextNode at1 = new TextNode("");
    Markup ar11 = new Markup(annotationLimen, "ra11").addTextNode(at1);
    Markup ar12 = new Markup(annotationLimen, "ra12").addTextNode(at1);
    annotationLimen//
        .addTextNode(at1)//
        .addMarkup(ar11)//
        .addMarkup(ar12);
    r1.addAnnotation(a1);

    TextNode t1 = new TextNode("");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    compareLMNL(expected, actual);
    assertTrue(compareDocuments(expected, actual));
  }

  @Test
  public void testAnonymousAnnotationRangeOpener() {
    String input = "[range1 [}annotation text{]}bla{range1]";
    printTokens(input);
    Document actual = new LMNLImporter().importLMNL(input);

    Document expected = new Document();
    Limen limen = expected.value();

    Markup r1 = new Markup(limen, "range1");
    Annotation a1 = simpleAnnotation("", "annotation text");
    r1.addAnnotation(a1);

    TextNode t1 = new TextNode("bla");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    compareLMNL(expected, actual);
    assertTrue(compareDocuments(expected, actual));
  }

  @Test
  public void testAtomsAreIgnored() {
    String input = "[r}Splitting the {{Atom}}.{r]";
    printTokens(input);
    Document actual = new LMNLImporter().importLMNL(input);

    Document expected = new Document();
    Limen limen = expected.value();

    Markup r1 = new Markup(limen, "r");
    TextNode t1 = new TextNode("Splitting the .");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    compareLMNL(expected, actual);
  }

  @Test
  public void testEmptyRange() {
    String input = "[empty}{empty]";
    printTokens(input);
    Document actual = new LMNLImporter().importLMNL(input);

    Document expected = new Document();
    Limen limen = expected.value();

    Markup r1 = new Markup(limen, "empty");
    TextNode t1 = new TextNode("");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    compareLMNL(expected, actual);
    assertTrue(compareDocuments(expected, actual));
  }

  @Test
  public void testComments() {
    String input = "[!-- comment 1 --][foo [!-- comment 2 --]}FOO[!-- comment 3 --]BAR{foo]";
    Document actual = new LMNLImporter().importLMNL(input);

    // Comments are ignored, so:
    // We expect a Document
    // - with one text node
    // - with one range on it
    Document expected = new Document();
    Limen limen = expected.value();
    Markup r1 = new Markup(limen, "foo");
    TextNode t1 = new TextNode("FOOBAR");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addMarkup(r1);

    logLMNL(actual);
    compareLMNL(expected, actual);
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

  private Annotation simpleAnnotation(String tag) {
    return new Annotation(tag);
  }

  private Annotation simpleAnnotation(String tag, String content) {
    Annotation a1 = simpleAnnotation(tag);
    Limen annotationLimen = a1.value();
    TextNode annotationText = new TextNode(content);
    annotationLimen.setOnlyTextNode(annotationText);
    return a1;
  }

  private void assertActualMatchesExpected(Document actual, Document expected) {
    Limen actualLimen = actual.value();
    List<Markup> actualMarkupList = actualLimen.markupList;
    List<TextNode> actualTextNodeList = actualLimen.textNodeList;

    Limen expectedLimen = expected.value();
    List<Markup> expectedMarkupList = expectedLimen.markupList;
    List<TextNode> expectedTextNodeList = expectedLimen.textNodeList;

    assertThat(actualTextNodeList).hasSize(expectedTextNodeList.size());
    for (int i = 0; i < expectedTextNodeList.size(); i++) {
      TextNode actualTextNode = actualTextNodeList.get(i);
      TextNode expectedTextNode = expectedTextNodeList.get(i);
      assertThat(actualTextNode).isEqualToComparingFieldByFieldRecursively(expectedTextNode);
    }

    assertThat(actualMarkupList).hasSize(expectedMarkupList.size());
    for (int i = 0; i < expectedMarkupList.size(); i++) {
      Markup actualMarkup = actualMarkupList.get(i);
      Markup expectedMarkup = expectedMarkupList.get(i);
      assertThat(actualMarkup.getTag()).isEqualTo(expectedMarkup.getTag());
      Comparator<Markup> markupComparator = Comparator.comparing(Markup::getTag);
      assertThat(actualMarkup).usingComparator(markupComparator).isEqualTo(expectedMarkup);
    }

    String actualLMNL = lmnlExporter.toLMNL(actual);
    String expectedLMNL = lmnlExporter.toLMNL(expected);
    LOG.info("LMNL={}", actualLMNL);
    assertThat(actualLMNL).isEqualTo(expectedLMNL);
    // assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  // I could use a matcher framework here
  private boolean compareDocuments(Document expected, Document actual) {
    Iterator<TextNode> i1 = expected.value().getTextNodeIterator();
    Iterator<TextNode> i2 = actual.value().getTextNodeIterator();
    boolean result = true;
    while (i1.hasNext() && result) {
      TextNode t1 = i1.next();
      TextNode t2 = i2.next();
      result = compareTextNodes(t1, t2);
    }
    return result;
  }

  private boolean compareTextNodes(TextNode t1, TextNode t2) {
    return t1.getContent().equals(t2.getContent());
  }

  private void logLMNL(Document actual) {
    LOG.info("LMNL=\n{}", lmnlExporter.toLMNL(actual));
  }

  private void logKdTree(Document actual) {
    LaTeXExporter latexExporter = new LaTeXExporter(actual);
    String latex1 = latexExporter.exportMatrix();
    LOG.info("matrix=\n{}", latex1);
    String latexKdTree = latexExporter.exportKdTree();
    LOG.info("latex tree=\n{}", latexKdTree);
  }

}
