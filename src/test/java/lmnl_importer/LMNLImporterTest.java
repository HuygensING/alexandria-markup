package lmnl_importer;

import data_model.*;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class LMNLImporterTest {

  @Test
  public void testTextRangeAnnotation() {
    String input = "[l [n}144{n]}He manages to keep the upper hand{l]";
    LMNLImporter importer = new LMNLImporter();
    Document actual = importer.importLMNL(input);


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
    r1.addTextNode(t1);
    limen.addTextNode(t1);
    limen.addTextRange(r1);

    assertTrue(compareDocuments(expected, actual));
    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);
  }

  @Test
  public void testLexingComplex() {
    String input = "[excerpt\n" +
            "  [source [date}1915{][title}The Housekeeper{]]\n" +
            "  [author\n" +
            "    [name}Robert Frost{]\n" +
            "    [dates}1874-1963{]] }\n" +
            "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n" +
            "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n" +
            "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n" +
            "{excerpt]";

    LMNLImporter importer = new LMNLImporter();
    Document actual = importer.importLMNL(input);
    Limen value = actual.value();
    TextRange textRange = new TextRange(value, "excerpt");
    assertThat(value.textRangeList).hasSize(7);
    List<TextRange> textRangeList = value.textRangeList;

    textRangeList.stream().map(TextRange::getTag).map(t->"["+t+"}").forEach(System.out::print);
    TextRange textRange1 = textRangeList.get(0);
    assertThat(textRange1.getTag()).isEqualTo("excerpt");

    TextRange textRange2 = textRangeList.get(1);
    assertThat(textRange2.getTag()).isEqualTo("s");

    TextRange textRange3 = textRangeList.get(2);
    assertThat(textRange3.getTag()).isEqualTo("l");
  }

  private Annotation simpleAnnotation(String tag, String content) {
    Annotation a1 = new Annotation(tag);
    Limen annotationLimen = a1.value();
    TextNode annotationText = new TextNode(content);
    annotationLimen.addTextNode(annotationText);
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
