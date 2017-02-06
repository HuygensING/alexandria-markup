package lmnl_importer;

import data_model.*;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporterTest {
  Logger LOG = LoggerFactory.getLogger(LMNLImporterTest.class);

  @Test
  public void testTextRangeAnnotation() {
    String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
    Document actual = new LMNLImporter().importLMNL(input);

    // Expectations:
    // We expect a Document
    // - with one text node
    // - with one range on it
    // - with one annotation on it.
    Document expected = new Document();
    Limen limen = expected.value();
    TextRange r1 = new TextRange(limen, "l");
    Annotation a1 = simpleAnnotation("n", "144");
    r1.addAnnotation(a1);
    TextNode t1 = new TextNode("He manages to keep the upper hand");
    r1.setOnlyTextNode(t1);
    limen.setOnlyTextNode(t1);
    limen.addTextRange(r1);

    assertTrue(compareDocuments(expected, actual));
    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
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
//    Limen value = actual.value();

//    TextRange textRange = new TextRange(value, "excerpt");
//    assertThat(value.textRangeList).hasSize(7);
//    List<TextRange> textRangeList = value.textRangeList;
//
//    textRangeList.stream().map(TextRange::getTag).map(t -> "[" + t + "}").forEach(System.out::print);
//    TextRange textRange1 = textRangeList.get(0);
//    assertThat(textRange1.getTag()).isEqualTo("excerpt");
//
//    TextRange textRange2 = textRangeList.get(1);
//    assertThat(textRange2.getTag()).isEqualTo("s");
//
//    TextRange textRange3 = textRangeList.get(2);
//    assertThat(textRange3.getTag()).isEqualTo("l");

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

    Annotation date = simpleAnnotation("date","1915");
    Annotation title = simpleAnnotation("title","The Housekeeper");
    Annotation source = simpleAnnotation("source")
            .addAnnotation(date)
            .addAnnotation(title)
            ;
    Annotation name = simpleAnnotation("name","Robert Frost");
    Annotation dates = simpleAnnotation("dates","1874-1963");
    Annotation author = simpleAnnotation("author")
            .addAnnotation(name)
            .addAnnotation(dates)
            ;
    Annotation n144 = simpleAnnotation("n","144");
    Annotation n145 = simpleAnnotation("n","145");
    Annotation n146 = simpleAnnotation("n","146");
    TextRange excerpt = new TextRange(limen, "excerpt")
            .addAnnotation(source)
            .addAnnotation(author)
            .setFirstAndLastTextNode(tn00,tn10)
            ;
    // 3 sentences
    TextRange s1 = new TextRange(limen, "s")
            .setFirstAndLastTextNode(tn01,tn03)
            ;
    TextRange s2 = new TextRange(limen, "s")
            .setOnlyTextNode(tn05)
            ;
    TextRange s3 = new TextRange(limen, "s")
            .setFirstAndLastTextNode(tn07,tn09)
            ;
    // 3 lines
    TextRange l1 = new TextRange(limen, "l")
            .setOnlyTextNode(tn01)
            .addAnnotation(n144)
            ;
    TextRange l2 = new TextRange(limen, "l")
            .setFirstAndLastTextNode(tn03,tn07)
            .addAnnotation(n145)
            ;
    TextRange l3 = new TextRange(limen, "l")
            .setOnlyTextNode(tn09)
            .addAnnotation(n146)
            ;

    limen.setFirstAndLastTextNode(tn00,tn10)
            .addTextRange(excerpt)
            .addTextRange(s1)
            .addTextRange(l1)
            .addTextRange(l2)
            .addTextRange(s2)
            .addTextRange(s3)
            .addTextRange(l3)
    ;

    assertActualMatchesExpected(actual,expected);
  }

  private void assertActualMatchesExpected(Document actual, Document expected) {
    Limen actualLimen = actual.value();
    List<TextRange> actualTextRangeList = actualLimen.textRangeList;
    List<TextNode> actualTextNodeList = actualLimen.textNodeList;

    Limen expectedLimen = expected.value();
    List<TextRange> expectedTextRangeList = expectedLimen.textRangeList;
    List<TextNode> expectedTextNodeList = expectedLimen.textNodeList;

    assertThat(actualTextNodeList).hasSize(expectedTextNodeList.size());
    for (int i = 0; i < expectedTextNodeList.size(); i++) {
      TextNode actualTextNode = actualTextNodeList.get(i);
      TextNode expectedTextNode = expectedTextNodeList.get(i);
      assertThat(actualTextNode).isEqualToComparingFieldByFieldRecursively(expectedTextNode);
    }

    assertThat(actualTextRangeList).hasSize(expectedTextRangeList.size());
    for (int i = 0; i < expectedTextRangeList.size(); i++) {
      TextRange actualTextRange = actualTextRangeList.get(i);
      TextRange expectedTextRange = expectedTextRangeList.get(i);
      assertThat(actualTextRange.getTag()).isEqualTo(expectedTextRange.getTag());
      assertThat(actualTextRange).isEqualToComparingFieldByFieldRecursively(expectedTextRange);
    }

//    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void testLMNL1kings12() throws IOException {
    InputStream input = FileUtils.openInputStream(new File("data/1kings12.lmnl"));
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", actual);

    Document expected = new Document();
    Limen limen = expected.value();
    TextRange excerpt = new TextRange(limen, "excerpt");
    Annotation source = simpleAnnotation("source");
    Annotation book = simpleAnnotation("book", "1 Kings");
    source.addAnnotation(book);
    Annotation chapter = simpleAnnotation("chapter", "12");
    source.addAnnotation(chapter);
    excerpt.addAnnotation(source);

    TextRange verse1 = new TextRange(limen, "verse");
    TextNode t1 = new TextNode("And he said unto them, ");
    verse1.addTextNode(t1);

//    TextNode t1 = new TextNode("He manages to keep the upper hand");
//    excerpt.addTextNode(t1);
//    limen.addTextNode(t1);

    limen.addTextRange(excerpt);

    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void testLMNLOzymandias() throws IOException {
    InputStream input = FileUtils.openInputStream(new File("data/ozymandias-voices-wap.lmnl"));
    Document document = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", document);
    assertThat(document).isNotNull();
  }

  private Annotation simpleAnnotation(String tag) {
    Annotation a1 = new Annotation(tag);
    return a1;
  }

  private Annotation simpleAnnotation(String tag, String content) {
    Annotation a1 = simpleAnnotation(tag);
    Limen annotationLimen = a1.value();
    TextNode annotationText = new TextNode(content);
    annotationLimen.setOnlyTextNode(annotationText);
    return a1;
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
    return true;
  }

  private boolean compareTextNodes(TextNode t1, TextNode t2) {
    return t1.getContent().equals(t2.getContent());
  }
}
