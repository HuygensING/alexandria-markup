package nl.knaw.huygens.alexandria.lmnl.importer;

import nl.knaw.huygens.alexandria.lmnl.data_model.*;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

public class LMNLImporterTest {
  final Logger LOG = LoggerFactory.getLogger(LMNLImporterTest.class);
  final LMNLExporter lmnlExporter = new LMNLExporter().useShorthand();

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

    logLMNL(actual);
    assertTrue(compareDocuments(expected, actual));
    assertThat(actual).isEqualToComparingFieldByFieldRecursively(expected);

    List<IndexPoint> indexPoints = logKdTree(actual);
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

    // TextRange textRange = new TextRange(value, "excerpt");
    // assertThat(value.textRangeList).hasSize(7);
    // List<TextRange> textRangeList = value.textRangeList;
    //
    // textRangeList.stream().map(TextRange::getTag).map(t -> "[" + t + "}").forEach(System.out::print);
    // TextRange textRange1 = textRangeList.get(0);
    // assertThat(textRange1.getTag()).isEqualTo("excerpt");
    //
    // TextRange textRange2 = textRangeList.get(1);
    // assertThat(textRange2.getTag()).isEqualTo("s");
    //
    // TextRange textRange3 = textRangeList.get(2);
    // assertThat(textRange3.getTag()).isEqualTo("l");

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
    TextRange excerpt = new TextRange(limen, "excerpt").addAnnotation(source).addAnnotation(author).setFirstAndLastTextNode(tn00, tn10);
    // 3 sentences
    TextRange s1 = new TextRange(limen, "s").setFirstAndLastTextNode(tn01, tn03);
    TextRange s2 = new TextRange(limen, "s").setOnlyTextNode(tn05);
    TextRange s3 = new TextRange(limen, "s").setFirstAndLastTextNode(tn07, tn09);
    // 3 lines
    TextRange l1 = new TextRange(limen, "l").setOnlyTextNode(tn01).addAnnotation(n144);
    TextRange l2 = new TextRange(limen, "l").setFirstAndLastTextNode(tn03, tn07).addAnnotation(n145);
    TextRange l3 = new TextRange(limen, "l").setOnlyTextNode(tn09).addAnnotation(n146);

    limen.setFirstAndLastTextNode(tn00, tn10).addTextRange(excerpt).addTextRange(s1).addTextRange(l1).addTextRange(l2).addTextRange(s2).addTextRange(s3).addTextRange(l3);

    assertActualMatchesExpected(actual, expected);

    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    List<IndexPoint> indexPoints = logKdTree(actual);
    // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testLMNL1kings12() throws IOException {
    String pathname = "data/1kings12.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", actual);

    logLMNL(actual);

    Limen actualLimen = actual.value();
    List<TextRange> actualTextRangeList = actualLimen.textRangeList;

    TextRange excerpt = actualTextRangeList.get(0);
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

    TextRange q1 = actualTextRangeList.get(2);
    assertThat(q1.getTag()).isEqualTo("q"); // first q
    assertThat(q1.textNodes).hasSize(2); // has 2 textnodes

    TextRange q2 = actualTextRangeList.get(3);
    assertThat(q2.getTag()).isEqualTo("q"); // second q, nested in first
    assertThat(q2.textNodes).hasSize(1); // has 1 textnode

    // compareLMNL(pathname, actual);
    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    List<IndexPoint> indexPoints = logKdTree(actual);
    // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testLMNLOzymandias() throws IOException {
    String pathname = "data/ozymandias-voices-wap.lmnl";
    InputStream input = FileUtils.openInputStream(new File(pathname));
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("document={}", actual);
    logLMNL(actual);
    assertThat(actual.value().hasTextNodes()).isTrue();
    String lmnl = lmnlExporter.toLMNL(actual);
    assertThat(lmnl).startsWith("[sonneteer [id}ozymandias{] [encoding [resp}ebeshero{] [resp}wap{]]}"); // annotations from sonneteer endtag moved to start tag
    assertThat(lmnl).contains("[meta [author}Percy Bysshe Shelley{] [title}Ozymandias{]]"); // anonymous textrange
    // compareLMNL(pathname, actual);

    List<IndexPoint> expectedIndexPoints = new ArrayList<>();
    expectedIndexPoints.add(new IndexPoint(0, 0));
    List<IndexPoint> indexPoints = logKdTree(actual);
    // assertThat(indexPoints).containsExactlyElementsOf(expectedIndexPoints);
  }

  @Test
  public void testDiscontinuousRanges(){
    String input = "'[e=e1}Ai,{e=e1]' riep Piet, '[e=e1}wat doe je, Mien?{e=e1]'";
    Document actual = new LMNLImporter().importLMNL(input);
    LOG.info("textNodes={}", actual.value().textNodeList);
    LOG.info("textRanges={}", actual.value().textRangeList);
    assertThat(actual.value().hasTextNodes()).isTrue();
    assertThat(actual.value().textRangeList).hasSize(1);
    String lmnl = lmnlExporter.toLMNL(actual);
    LOG.info("lmnl={}", lmnl);
    assertThat(lmnl).isEqualTo(input);
  }

  private void compareLMNL(String pathname, Document actual) throws IOException {
    String inLMNL = FileUtils.readFileToString(new File(pathname));
    String outLMNL = lmnlExporter.toLMNL(actual);
    assertThat(outLMNL).isEqualTo(inLMNL);
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
      Comparator<TextRange> textRangeComparator = Comparator.comparing(TextRange::getTag);
      assertThat(actualTextRange).usingComparator(textRangeComparator).isEqualTo(expectedTextRange);
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

  private List<IndexPoint> logKdTree(Document actual) {
    Limen limen = actual.value();
    List<IndexPoint> indexPoints = limen.getIndexPoints();

    Set<Integer> longTextRangeIndexes = new HashSet<>();
    for (int i = 0; i < limen.textRangeList.size(); i++) {
      if (limen.containsAtLeastHalfOfAllTextNodes(limen.textRangeList.get(i))) {
        longTextRangeIndexes.add(i);
      }
    }

    LOG.info("indexpoints={}", indexPoints);
    String latex1 = latexMatrix(limen.textNodeList, limen.textRangeList, indexPoints, longTextRangeIndexes);
    LOG.info("matrix=\n{}", latex1);
    KdTree<IndexPoint> kdTree = new KdTree<>(indexPoints);
    LOG.info("kdtree=\n{}", kdTree);
    String latexKdTree = latexKdTree(kdTree, longTextRangeIndexes);
    LOG.info("latex tree=\n{}", latexKdTree);
    return indexPoints;
  }

  private String latexMatrix(List<TextNode> allTextNodes, List<TextRange> allTextRanges, List<IndexPoint> indexPoints, Set<Integer> longTextRangeIndexes) {
    List<String> rangeLabels = allTextRanges.stream()
            .map(TextRange::getTag)
            .collect(Collectors.toList());
    List<String> rangeIndex = new ArrayList<>();
    rangeIndex.add("");
    for (int i = 0; i < rangeLabels.size(); i++) {
      rangeIndex.add(String.valueOf(i));
    }
    rangeLabels.add(0, "");
    rangeLabels.add("");
    String tabularContent = StringUtils.repeat("l|", rangeLabels.size() - 1) + "l";
    StringBuilder latexBuilder = new StringBuilder()//
            .append("\\documentclass{article}\n")//
            .append("\\usepackage{array,graphicx}\n")//
            .append("\\newcommand*\\rot{\\rotatebox{90}}\n")//
            .append("\\usepackage{incgraph}\n")//
            .append("\\begin{document}\n")//
            .append("\\begin{table}[]\n")//
            .append("\\begin{inctext}\n")//
            .append("\\centering\n").append("\\begin{tabular}{").append(tabularContent).append("}\n").append(rangeIndex.stream().map(c -> "$" + c + "$").collect(Collectors.joining(" & "))).append("\\\\\n")//
            .append("\\hline\n")//
            ;

    Iterator<IndexPoint> pointIterator = indexPoints.iterator();
    IndexPoint indexPoint = pointIterator.next();
    for (int i = 0; i < allTextNodes.size(); i++) {
      List<String> row = new ArrayList<>();
      row.add(String.valueOf(i));
      for (int j = 0; j < allTextRanges.size(); j++) {
        if (i == indexPoint.getTextNodeIndex() && j == indexPoint.getTextRangeIndex()) {
          String cell = longTextRangeIndexes.contains(j) ? "\\underline{X}" : "X";
          row.add(cell);
          if (pointIterator.hasNext()) {
            indexPoint = pointIterator.next();
          }

        } else {
          row.add(" ");
        }
      }
      String content = allTextNodes.get(i).getContent()//
              .replaceAll(" ", "\\\\textvisiblespace ")//
              .replaceAll("\n", "\\\\textbackslash n");
      row.add(content);
      latexBuilder.append(row.stream().collect(Collectors.joining(" & "))).append("\\\\ \\hline\n");
    }

    latexBuilder.append(rangeLabels.stream().map(c -> "\\rot{$" + c + "$}").collect(Collectors.joining(" & ")))//
            .append("\\\\\n")//
            .append("\\end{tabular}\n")//
            .append("\\end{inctext}\n")//
            .append("\\end{table}\n")//
            .append("\\end{document}");
    return latexBuilder.toString();
  }

  private String latexKdTree(KdTree<IndexPoint> kdTree, Set<Integer> longTextRangeIndexes) {
    StringBuilder latexBuilder = new StringBuilder()//
            .append("\\documentclass[landscape]{article}\n")//
            .append("\\usepackage[utf8]{inputenc}\n")//
            .append("\\usepackage[T1]{fontenc}\n")//
            .append("\\usepackage{incgraph}\n")//
            .append("\\usepackage{caption}\n")//
            .append("\\usepackage[margin=1in]{geometry}\n")//
            .append("\\usepackage{tikz-qtree}\n")//
            .append("\\usetikzlibrary{shadows,trees}\n")//
            .append("\\begin{document}\n")//
            .append("\\tikzset{font=\\small,\n" +
                    "%edge from parent fork down,\n" +
                    "level distance=1.5cm,\n" +
                    "textNodeAxis/.style=\n" +
                    "    {top color=white,\n" +
                    "    bottom color=blue!25,\n" +
                    "    circle,\n" +
                    "    minimum height=8mm,\n" +
                    "    draw=blue!75,\n" +
                    "    very thick,\n" +
                    "    drop shadow,\n" +
                    "    align=center,\n" +
                    "    text depth = 0pt\n" +
                    "    },\n" +
                    "textRangeAxis/.style=\n" +
                    "    {top color=white,\n" +
                    "    bottom color=green!25,\n" +
                    "    circle,\n" +
                    "    minimum height=8mm,\n" +
                    "    draw=green!75,\n" +
                    "    very thick,\n" +
                    "    drop shadow,\n" +
                    "    align=center,\n" +
                    "    text depth = 0pt\n" +
                    "    },\n" +
                    "edge from parent/.style=\n" +
                    "    {draw=black!50,\n" +
                    "    thick\n" +
                    "    }}\n" +
                    "\n" +
                    "\\centering\n")//
            .append("\\begin{figure}\n")
//            .append("\\begin{inctext}\n")
            .append("\\begin{tikzpicture}[level/.style={sibling distance=60mm/#1}]\n");
    KdTree.KdNode root = kdTree.getRoot();
    IndexPoint rootIP = root.getContent();
    String content = toNodeContent(rootIP, longTextRangeIndexes);
    latexBuilder.append("\\Tree [.\\node[textNodeAxis]{").append(content).append("};\n");
    appendChildTree(latexBuilder, root.getLesser(), "textRangeAxis", longTextRangeIndexes);
    appendChildTree(latexBuilder, root.getGreater(), "textRangeAxis", longTextRangeIndexes);
    latexBuilder
            .append("]\n")//
            .append("\\end{tikzpicture}\n")//
            .append("\\centering\n")//
            .append("\\caption*{(a,b) = (TextNodeIndex, TextRangeIndex)}\n")//
//            .append("\\end{inctext}\n")//
            .append("\\end{figure}\n")//
            .append("\\end{document}")//
    ;
    return latexBuilder.toString();
  }

  private String toNodeContent(IndexPoint indexPoint, Set<Integer> longTextRangeIndexes) {
    String content = indexPoint.toString();
    if (longTextRangeIndexes.contains(indexPoint.getTextRangeIndex())){
      return "\\underline{"+content+"}";
    }
    return content;
  }

  private void appendChildTree(StringBuilder latexBuilder, KdTree.KdNode kdnode, String style, Set<Integer> longTextRangeIndexes) {
    if (kdnode == null) {
      latexBuilder.append("[.\\node[").append(style).append("]{};\n]\n");
      return;
    }
    IndexPoint indexPoint = kdnode.getContent();
    String content = toNodeContent(indexPoint, longTextRangeIndexes);
    latexBuilder.append("[.\\node[").append(style).append("]{").append(content).append("};\n");
    String nextStyle = (style.equals("textNodeAxis") ? "textRangeAxis" : "textNodeAxis");
    if (!(kdnode.getLesser() == null && kdnode.getGreater() == null)) {
      appendChildTree(latexBuilder, kdnode.getLesser(), nextStyle, longTextRangeIndexes);
      appendChildTree(latexBuilder, kdnode.getGreater(), nextStyle, longTextRangeIndexes);
    }
    latexBuilder.append("]\n");
  }
}
