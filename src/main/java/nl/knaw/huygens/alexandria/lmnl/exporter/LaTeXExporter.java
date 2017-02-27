package nl.knaw.huygens.alexandria.lmnl.exporter;

import nl.knaw.huygens.alexandria.lmnl.data_model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by bramb on 07/02/2017.
 */
public class LaTeXExporter {
  private static Logger LOG = LoggerFactory.getLogger(LaTeXExporter.class);

  public String toLaTeX(Document document) {
    StringBuilder latexBuilder = new StringBuilder();
    latexBuilder.append("\\documentclass{article}\n")
            .append("\\usepackage{incgraph}\n")
            .append("\\usepackage{tikz}\n")
            .append("\\usepackage{latexsym}\n")
            .append("\\usepackage[utf8x]{inputenc}\n")
            .append("\\usetikzlibrary{arrows,arrows.meta,decorations.pathmorphing,backgrounds,positioning,fit,graphs,shapes}\n")
            .append("\n")
            .append("\\begin{document}\n")
            .append("\\begin{inctext}\n")
            .append("  \\pagenumbering{gobble}% Remove page numbers (and reset to 1)\n")
            .append("  \\begin{tikzpicture}\n")
            .append("    [textnode/.style={rectangle,draw=black!50,thick,rounded corners},\n")
            .append("     textrange/.style={rectangle,draw=blue!50,thick},\n")
            .append("     document/.style={circle,draw=black!50,thick}]\n")
            .append("    \\node[document] (doc) {document};\n");
    Limen limen = document.value();
    appendLimen(latexBuilder, limen);
    latexBuilder.append("  \\end{tikzpicture}\n")
            .append("\\end{inctext}\n")
            .append("\\end{document}\n");
    return latexBuilder.toString();
  }

  private void appendLimen(StringBuilder latexBuilder, Limen limen) {
    ColorPicker colorPicker = new ColorPicker("blue", "brown", "cyan", "darkgray", "gray", "green",
            "lightgray", "lime", "magenta", "olive", "orange", "pink", "purple", "red", "teal", "violet", "black");
    latexBuilder.append("\n    % TextNodes\n");
    if (limen != null) {
      Set<TextRange> openTextRanges = new LinkedHashSet<>();
      AtomicInteger textNodeCounter = new AtomicInteger(0);
      Map<TextNode, Integer> textNodeIndices = new HashMap<>();
      limen.getTextNodeIterator().forEachRemaining(tn -> {
        int i = textNodeCounter.getAndIncrement();
        textNodeIndices.put(tn, i);
        List<TextRange> textRanges = limen.getTextRanges(tn);

        List<TextRange> toClose = new ArrayList<>();
        toClose.addAll(openTextRanges);
        toClose.removeAll(textRanges);
        Collections.reverse(toClose);

        List<TextRange> toOpen = new ArrayList<>();
        toOpen.addAll(textRanges);
        toOpen.removeAll(openTextRanges);

        openTextRanges.removeAll(toClose);
        openTextRanges.addAll(toOpen);

        addTextNode(latexBuilder, tn, i);
      });
      connectTextNodes(latexBuilder, textNodeCounter);
      markTextRanges(latexBuilder, limen, colorPicker, textNodeIndices);
//      drawTextRangesAsSets(latexBuilder, limen, colorPicker, textNodeIndices);
    }
  }

  private void drawTextRangesAsSets(StringBuilder latexBuilder, Limen limen, ColorPicker colorPicker, Map<TextNode, Integer> textNodeIndices) {
    limen.textRangeList.forEach(tr -> {
      String color = colorPicker.nextColor();
      latexBuilder.append("    \\node[draw=").append(color).append(",shape=rectangle,fit=");
      tr.textNodes.forEach(tn -> {
        int i = textNodeIndices.get(tn);
        latexBuilder.append("(tn").append(i).append(")");
      });
      latexBuilder.append(",label={[").append(color).append("]below:$").append(tr.getTag()).append("$}]{};\n");
    });
  }

  private void addTextNode(StringBuilder latexBuilder, TextNode tn, int i) {
    String content = tn.getContent().replaceAll(" ", "\\\\textvisiblespace ").replaceAll("\n", "\\\\textbackslash n");
    String relPos = i == 0 ? "below=of doc" : ("right=of tn" + (i - 1));
    String nodeLine = "    \\node[textnode] (tn" + i + ") [" + relPos + "] {" + content + "};\n";
    latexBuilder.append(nodeLine);
  }

  private void connectTextNodes(StringBuilder latexBuilder, AtomicInteger textNodeCounter) {
    latexBuilder.append("");
    latexBuilder.append("\n    % connect TextNodes\n    \\graph{")
            .append("(doc)");
    for (int i = 0; i < textNodeCounter.get(); i++) {
      latexBuilder.append(" -> (tn").append(i).append(")");
    }
    latexBuilder.append("};\n");
  }

  private void markTextRanges(StringBuilder latexBuilder, Limen limen, ColorPicker colorPicker, Map<TextNode, Integer> textNodeIndices) {
    AtomicInteger textRangeCounter = new AtomicInteger(0);
    latexBuilder.append("\n    % TextRanges");
    Map<TextRange, Integer> layerIndex = calculateLayerIndex(limen.textRangeList, textNodeIndices);
    limen.textRangeList.forEach(tr -> {
      int rangeLayerIndex = layerIndex.get(tr);
      float textRangeRow = 0.75f * (rangeLayerIndex + 1);
      String color = colorPicker.nextColor();
      TextNode firstTextNode = tr.textNodes.get(0);
      TextNode lastTextNode = tr.textNodes.get(tr.textNodes.size() - 1);
      int first = textNodeIndices.get(firstTextNode);
      int last = textNodeIndices.get(lastTextNode);
      latexBuilder.append("\n    \\node[label=below right:{$")
              .append(tr.getTag())
              .append("$}](tr")
              .append(rangeLayerIndex)
              .append("b)[below left=")
              .append(textRangeRow)
              .append(" and 0 of tn")
              .append(first)
              .append("]{};\n");
      latexBuilder.append("    \\node[](tr")
              .append(rangeLayerIndex)
              .append("e)[below right=")
              .append(textRangeRow)
              .append(" and 0 of tn")
              .append(last)
              .append("]{};\n");
      latexBuilder.append("    \\draw[Bar-Bar,thick,color=")
              .append(color).append("] (tr")
              .append(rangeLayerIndex)
              .append("b) -- (tr")
              .append(rangeLayerIndex)
              .append("e);\n");
    });
  }

  private Map<TextRange, Integer> calculateLayerIndex(List<TextRange> textranges, Map<TextNode, Integer> textNodeIndex) {
//    return textRangeCounter.getAndIncrement();
    Map<String, List<TextRange>> map = textranges.stream().collect(Collectors.groupingBy(TextRange::getTag));

    Map<TextRange, Integer> index = new HashMap<>();
    Map<Integer, Integer> lastTextNodeUsedInLayer = new HashMap<>();
//    map.values().stream().flatMap(List::stream).forEach(tr -> {
      textranges.forEach(tr -> {
      int firstTextNodeIndex = textNodeIndex.get(tr.textNodes.get(0));
      int lastTextNodeIndex = textNodeIndex.get(tr.textNodes.get(tr.textNodes.size() - 1));
      int layerNo = 0;
      if (!index.isEmpty()) {
        boolean goOn = true;
        while (goOn) {
          int lastTextNodeInLayer = lastTextNodeUsedInLayer.get(layerNo);
          if (firstTextNodeIndex > lastTextNodeInLayer) {
            goOn = false;
          } else {
            layerNo += 1;
            if (!lastTextNodeUsedInLayer.containsKey(layerNo)) {
              goOn = false;
            }
          }
        }
      }
      index.put(tr, layerNo);
      lastTextNodeUsedInLayer.put(layerNo, lastTextNodeIndex);
    });
    return index;
  }

  public StringBuilder toLMNL(Annotation annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getTag());
    annotation.getAnnotations().forEach(a1 ->
            annotationBuilder.append(" ").append(toLMNL(a1))
    );
    Limen limen = annotation.value();
    if (limen.hasTextNodes()) {
      annotationBuilder.append("}");
      appendLimen(annotationBuilder, limen);
      annotationBuilder.append("{").append(annotation.getTag()).append("]");
    } else {
      annotationBuilder.append("]");
    }
    return annotationBuilder;
  }

}
