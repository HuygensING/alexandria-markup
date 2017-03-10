package nl.knaw.huygens.alexandria.lmnl.exporter;

import nl.knaw.huygens.alexandria.lmnl.data_model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LaTeXExporter {
  public static final Comparator<TextRangeLayer> ON_MAX_RANGE_SIZE = Comparator.comparing(TextRangeLayer::getTag)//
      .thenComparing(Comparator.comparingInt(TextRangeLayer::getMaxRangeSize));
  private static Logger LOG = LoggerFactory.getLogger(LaTeXExporter.class);

  public String toLaTeX(Document document) {
    StringBuilder latexBuilder = new StringBuilder();
    latexBuilder.append("\\documentclass{article}\n")//
        .append("\\usepackage{incgraph}\n")//
        .append("\\usepackage{tikz}\n")//
        .append("\\usepackage{latexsym}\n")//
        .append("\\usepackage[utf8x]{inputenc}\n")//
        .append("\\usetikzlibrary{arrows,arrows.meta,decorations.pathmorphing,backgrounds,positioning,fit,graphs,shapes}\n")//
        .append("\n")//
        .append("\\begin{document}\n")//
        .append("\\begin{inctext}\n")//
        .append("  \\pagenumbering{gobble}% Remove page numbers (and reset to 1)\n")//
        .append("  \\begin{tikzpicture}\n")//
        .append("    [textnode/.style={rectangle,draw=black!50,thick,rounded corners},\n")//
        .append("     textrange/.style={rectangle,draw=blue!50,thick},\n")//
        .append("     document/.style={circle,draw=black!50,thick}]\n")//
        .append("    \\node[document] (doc) {document};\n");
    Limen limen = document.value();
    appendLimen(latexBuilder, limen);
    latexBuilder.append("  \\end{tikzpicture}\n")//
        .append("\\end{inctext}\n")//
        .append("\\end{document}\n");
    return latexBuilder.toString();
  }

  private void appendLimen(StringBuilder latexBuilder, Limen limen) {
    ColorPicker colorPicker = new ColorPicker("blue", "brown", "cyan", "darkgray", "gray", "green", "lightgray", //
        "lime", "magenta", "olive", "orange", "pink", "purple", "red", "teal", "violet", "black");
    latexBuilder.append("\n    % TextNodes\n");
    if (limen != null) {
      Set<TextRange> openTextRanges = new LinkedHashSet<>();
      AtomicInteger textNodeCounter = new AtomicInteger(0);
      Map<TextNode, Integer> textNodeIndices = new HashMap<>();
      limen.getTextNodeIterator().forEachRemaining(tn -> {
        int i = textNodeCounter.getAndIncrement();
        textNodeIndices.put(tn, i);
        Set<TextRange> textRanges = limen.getTextRanges(tn);

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
      // drawTextRangesAsSets(latexBuilder, limen, colorPicker, textNodeIndices);
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
    latexBuilder.append("\n    % connect TextNodes\n    \\graph{").append("(doc)");
    for (int i = 0; i < textNodeCounter.get(); i++) {
      latexBuilder.append(" -> (tn").append(i).append(")");
    }
    latexBuilder.append("};\n");
  }

  private void markTextRanges(StringBuilder latexBuilder, Limen limen, ColorPicker colorPicker, Map<TextNode, Integer> textNodeIndices) {
    // AtomicInteger textRangeCounter = new AtomicInteger(0);
    latexBuilder.append("\n    % TextRanges");
    Map<TextRange, Integer> layerIndex = calculateLayerIndex(limen.textRangeList, textNodeIndices);
    limen.textRangeList.forEach(tr -> {
      int rangeLayerIndex = layerIndex.get(tr);
      float textRangeRow = 0.75f * (rangeLayerIndex + 1);
      String color = colorPicker.nextColor();
      if (tr.isContinuous()) {
        TextNode firstTextNode = tr.textNodes.get(0);
        TextNode lastTextNode = tr.textNodes.get(tr.textNodes.size() - 1);
        int first = textNodeIndices.get(firstTextNode);
        int last = textNodeIndices.get(lastTextNode);
        appendTextRange(latexBuilder, tr, String.valueOf(rangeLayerIndex), textRangeRow, color, first, last);

      } else {
        Iterator<TextNode> textNodeIterator = tr.textNodes.iterator();
        TextNode firstTextNode = textNodeIterator.next();
        TextNode lastTextNode = firstTextNode;
        boolean finished = false;
        int partNo = 0;
        while (!finished) {
          TextNode expectedNextNode = firstTextNode.getNextTextNode();
          boolean goOn = textNodeIterator.hasNext();
          while (goOn) {
            TextNode nextTextNode = textNodeIterator.next();
            if (nextTextNode.equals(expectedNextNode)) {
              lastTextNode = nextTextNode;
              goOn = textNodeIterator.hasNext();

            } else {
              appendTextRangePart(latexBuilder, textNodeIndices, tr, rangeLayerIndex, textRangeRow, color, firstTextNode, lastTextNode, partNo);
              firstTextNode = nextTextNode;
              lastTextNode = firstTextNode;
              partNo++;
              goOn = false;
            }
          }

          finished = finished || !textNodeIterator.hasNext();
        }
        appendTextRangePart(latexBuilder, textNodeIndices, tr, rangeLayerIndex, textRangeRow, color, firstTextNode, lastTextNode, partNo);

        for (int i = 0; i < partNo; i++) {
          String leftNode = MessageFormat.format("tr{0}_{1}e", rangeLayerIndex, i);
          String rightNode = MessageFormat.format("tr{0}_{1}b", rangeLayerIndex, (i + 1));
          latexBuilder.append("    \\draw[densely dashed,color=lightgray] (" + leftNode + ".south) to [out=350,in=190] (" + rightNode + ".south);\n");
        }
      }

    });
  }

  private void appendTextRangePart(StringBuilder latexBuilder, Map<TextNode, Integer> textNodeIndices, TextRange tr, int rangeLayerIndex, float textRangeRow, String color, TextNode firstTextNode,
      TextNode lastTextNode, int partNo) {
    int first = textNodeIndices.get(firstTextNode);
    int last = textNodeIndices.get(lastTextNode);
    String textRangePartNum = String.valueOf(rangeLayerIndex) + "_" + partNo;
    appendTextRange(latexBuilder, tr, textRangePartNum, textRangeRow, color, first, last);
  }

  private void appendTextRange(StringBuilder latexBuilder, TextRange tr, String rangeLayerIndex, float textRangeRow, String color, int first, int last) {
    latexBuilder.append("\n    \\node[label=below right:{$")//
        .append(tr.getTag())//
        .append("$}](tr")//
        .append(rangeLayerIndex)//
        .append("b)[below left=")//
        .append(textRangeRow)//
        .append(" and 0 of tn")//
        .append(first)//
        .append("]{};\n");
    latexBuilder.append("    \\node[](tr")//
        .append(rangeLayerIndex)//
        .append("e)[below right=")//
        .append(textRangeRow)//
        .append(" and 0 of tn")//
        .append(last)//
        .append("]{};\n");
    latexBuilder.append("    \\draw[Bar-Bar,thick,color=")//
        .append(color).append("] (tr")//
        .append(rangeLayerIndex)//
        .append("b) -- (tr")//
        .append(rangeLayerIndex)//
        .append("e);\n");
    latexBuilder.append("    \\draw[thin,color=lightgray] (tr")//
        .append(rangeLayerIndex)//
        .append("b.east) -- (tn")//
        .append(first)//
        .append(".west);\n");
    latexBuilder.append("    \\draw[thin,color=lightgray] (tr")//
        .append(rangeLayerIndex)//
        .append("e.west) -- (tn")//
        .append(last)//
        .append(".east);\n");
  }

  private static class TextRangeLayer {
    final Map<TextNode, Integer> textNodeIndex;

    final List<TextRange> textRanges = new ArrayList<>();
    final Set<String> tags = new HashSet<>();
    int maxRangeSize = 1; // the number of textnodes of the biggest textrange
    int lastTextNodeUsed = 0;

    TextRangeLayer(Map<TextNode, Integer> textNodeIndex) {
      this.textNodeIndex = textNodeIndex;
    }

    public void addTextRange(TextRange textRange) {
      textRanges.add(textRange);
      tags.add(normalize(textRange.getTag()));
      maxRangeSize = Math.max(maxRangeSize, textRange.textNodes.size());
      lastTextNodeUsed = textNodeIndex.get(textRange.textNodes.get(textRange.textNodes.size() - 1));
    }

    public List<TextRange> getTextRanges() {
      return textRanges;
    }

    public boolean hasTag(String tag) {
      return tags.contains(tag);
    }

    public String getTag() {
      return tags.iterator().next();
    }

    public int getMaxRangeSize() {
      return maxRangeSize;
    }

    public boolean canAdd(TextRange textRange) {
      String nTag = normalize(textRange.getTag());
      if (!tags.contains(nTag)) {
        return false;
      }
      TextNode firstTextNode = textRange.textNodes.get(0);
      int firstTextNodeIndex = textNodeIndex.get(firstTextNode);
      return (firstTextNodeIndex > lastTextNodeUsed);
    }

    private String normalize(String tag) {
      return tag.replaceFirst("=.*$", "");
    }
  }

  private Map<TextRange, Integer> calculateLayerIndex(List<TextRange> textranges, Map<TextNode, Integer> textNodeIndex) {
    List<TextRangeLayer> layers = new ArrayList<>();
    textranges.forEach(tr -> {
      Optional<TextRangeLayer> oLayer = layers.stream().filter(layer -> layer.canAdd(tr)).findFirst();
      if (oLayer.isPresent()) {
        oLayer.get().addTextRange(tr);

      } else {
        TextRangeLayer layer = new TextRangeLayer(textNodeIndex);
        layer.addTextRange(tr);
        layers.add(layer);
      }
    });

    AtomicInteger layerCounter = new AtomicInteger();
    Map<TextRange, Integer> index = new HashMap<>();
    layers.stream().sorted(ON_MAX_RANGE_SIZE).forEach(layer -> {
      int i = layerCounter.getAndIncrement();
      layer.getTextRanges().forEach(tr -> index.put(tr, i));
    });

    return index;
  }

}
