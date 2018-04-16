package nl.knaw.huygens.alexandria.exporter;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.data_model.IndexPoint;
import nl.knaw.huygens.alexandria.data_model.KdTree;
import nl.knaw.huygens.alexandria.data_model.NodeRangeIndex;
import nl.knaw.huygens.alexandria.freemarker.FreeMarker;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public class LaTeXExporter {
  private static Logger LOG = LoggerFactory.getLogger(LaTeXExporter.class);
  private static final Comparator<MarkupLayer> ON_MAX_RANGE_SIZE = Comparator.comparing(MarkupLayer::getTag)//
      .thenComparingInt(MarkupLayer::getMaxRangeSize);
  private List<IndexPoint> indexPoints;
  private static TAGStore store;
  private final DocumentWrapper document;
  private Set<Integer> longMarkupIndexes;
  private NodeRangeIndex index;

  public LaTeXExporter(TAGStore store, DocumentWrapper document) {
    LaTeXExporter.store = store;
    this.document = document;
  }

  public String exportMarkupOverlap() {
    Map<String, Object> map = new HashMap<>();
    long maxMarkupsPerTextNode = document.getTextNodeStream()
        .map(document::getMarkupStreamForTextNode)
        .mapToLong(Stream::count)//
        .max()//
        .getAsLong();
    map.put("maxdepth", maxMarkupsPerTextNode);
    StringBuilder latexBuilder = new StringBuilder();
    if (document != null) {
      document.getTextNodeStream().forEach(tn -> {
        int size = document.getDocument().getMarkupIds().size();
        addColoredTextNode(latexBuilder, tn, size);
      });
    }
    map.put("body", latexBuilder.toString());
    return FreeMarker.templateToString("colored-text.tex.ftl", map, this.getClass());
  }

  private void addColoredTextNode(StringBuilder latexBuilder, TextNodeWrapper tn, int depth) {
    String content = tn.getText();
    if ("\n".equals(content)) {
      latexBuilder.append("\\TextNode{").append(depth).append("}{\\n}\\\\\n");
    } else {
      String[] parts = content.split("\n");
      List<String> colorboxes = new ArrayList<>();
      for (int i = 0; i < parts.length; i++) {
        String part = parts[i];
        if (i < parts.length - 1) {
          part += "\\n";
        }
        colorboxes.add("\\TextNode{" + depth + "}{" + part.replaceAll("&", "\\\\&") + "}");
      }
      latexBuilder.append(colorboxes.stream().collect(Collectors.joining("\\\\\n")));
    }
  }

  public String exportDocument() {
    Map<String, Object> map = new HashMap<>();
    StringBuilder latexBuilder = new StringBuilder();
    appendDocument(latexBuilder, document);
    map.put("body", latexBuilder.toString());
    return FreeMarker.templateToString("document.tex.ftl", map, this.getClass());
  }

  private void appendDocument(StringBuilder latexBuilder, DocumentWrapper document) {
    ColorPicker colorPicker = new ColorPicker("blue", "brown", "cyan", "darkgray", "gray", "green", "lightgray", //
        "lime", "magenta", "olive", "orange", "pink", "purple", "red", "teal", "violet", "black");
    latexBuilder.append("\n    % TextNodes\n");
    if (document != null) {
      Set<MarkupWrapper> openMarkups = new LinkedHashSet<>();
      AtomicInteger textNodeCounter = new AtomicInteger(0);
      Map<TextNodeWrapper, Integer> textNodeIndices = new HashMap<>();
      document.getTextNodeStream().forEach(tn -> {
        int i = textNodeCounter.getAndIncrement();
        textNodeIndices.put(tn, i);
        Set<MarkupWrapper> markups = document.getMarkupStreamForTextNode(tn)//
            .collect(Collectors.toSet());

        List<MarkupWrapper> toClose = new ArrayList<>(openMarkups);
        toClose.removeAll(markups);
        Collections.reverse(toClose);

        List<MarkupWrapper> toOpen = new ArrayList<>(markups);
        toOpen.removeAll(openMarkups);

        openMarkups.removeAll(toClose);
        openMarkups.addAll(toOpen);

        addTextNode(latexBuilder, tn, i);
      });

      connectTextNodes(latexBuilder, textNodeCounter);
      markMarkups(latexBuilder, document, colorPicker, textNodeIndices);
      // drawMarkupsAsSets(latexBuilder, document, colorPicker, textNodeIndices);
    }
  }

  private void addTextNode(StringBuilder latexBuilder, TextNodeWrapper tn, int i) {
    String content = escapedContent(tn);
    String relPos = i == 0 ? "below=of doc" : ("right=of tn" + (i - 1));
    String nodeLine = "    \\node[textnode] (tn" + i + ") [" + relPos + "] {" + content + "};\n";
    latexBuilder.append(nodeLine);
  }

  public String exportMatrix() {
    Map<String, Object> map = new HashMap<>();
    String body = exportMatrix(document.getTextNodeStream().collect(toList()), document.getMarkupStream().collect(toList()), getIndexPoints(), getLongMarkupIndexes());
    map.put("body", body);
    return FreeMarker.templateToString("matrix.tex.ftl", map, this.getClass());
  }

  private String exportMatrix(List<TextNodeWrapper> allTextNodes, List<MarkupWrapper> allMarkups, List<IndexPoint> indexPoints, Set<Integer> longMarkupIndexes) {
    List<String> rangeLabels = allMarkups.stream().map(MarkupWrapper::getTag).collect(toList());
    List<String> rangeIndex = new ArrayList<>();
    rangeIndex.add("");
    for (int i = 0; i < rangeLabels.size(); i++) {
      rangeIndex.add(String.valueOf(i));
    }
    rangeLabels.add(0, "");
    rangeLabels.add("");
    String tabularContent = StringUtils.repeat("l|", rangeLabels.size() - 1) + "l";
    StringBuilder latexBuilder = new StringBuilder()//
        .append("\\begin{tabular}{").append(tabularContent).append("}\n")//
        .append(rangeIndex.stream().map(c -> "$" + c + "$").collect(Collectors.joining(" & "))).append("\\\\\n")//
        .append("\\hline\n")//
        ;

    Iterator<IndexPoint> pointIterator = indexPoints.iterator();
    if (pointIterator.hasNext()) {
      IndexPoint indexPoint = pointIterator.next();
      for (int i = 0; i < allTextNodes.size(); i++) {
        List<String> row = new ArrayList<>();
        row.add(String.valueOf(i));
        for (int j = 0; j < allMarkups.size(); j++) {
          if (i == indexPoint.getTextNodeIndex() && j == indexPoint.getMarkupIndex()) {
            String cell = longMarkupIndexes.contains(j) ? "\\underline{X}" : "X";
            row.add(cell);
            if (pointIterator.hasNext()) {
              indexPoint = pointIterator.next();
            }

          } else {
            row.add(" ");
          }
        }
        String content = escapedContent(allTextNodes.get(i));
        row.add(content);
        latexBuilder.append(row.stream().collect(Collectors.joining(" & "))).append("\\\\ \\hline\n");
      }
    }

    latexBuilder
        .append(rangeLabels.stream()//
            .map(c -> "\\rot{$" + c + "$}")//
            .collect(Collectors.joining(" & ")))//
        .append("\\\\\n")//
        .append("\\end{tabular}\n");
    return latexBuilder.toString();
  }

  public String exportKdTree() {
    Map<String, Object> map = new HashMap<>();
    String kdTree = exportKdTree(getIndex().getKdTree(), getLongMarkupIndexes());
    map.put("body", kdTree);
    return FreeMarker.templateToString("kdtree.tex.ftl", map, this.getClass());
  }

  private String exportKdTree(KdTree<IndexPoint> kdTree, Set<Integer> longMarkupIndexes) {
    StringBuilder latexBuilder = new StringBuilder();
    KdTree.KdNode root = kdTree.getRoot();
    if (root != null) {
      IndexPoint rootIP = root.getContent();
      String content = toNodeContent(rootIP, longMarkupIndexes);
      latexBuilder.append("\\Tree [.\\node[textNodeAxis]{").append(content).append("};\n");
      appendChildTree(latexBuilder, root.getLesser(), "markupAxis", longMarkupIndexes);
      appendChildTree(latexBuilder, root.getGreater(), "markupAxis", longMarkupIndexes);
    }
    return latexBuilder.append("]\\\\\n").toString();
  }

  public String exportGradient() {
    Map<String, Object> map = new HashMap<>();
    StringBuilder latexBuilder = new StringBuilder();
    appendGradedTAGDocument(latexBuilder, document);
    map.put("body", latexBuilder.toString());
    return FreeMarker.templateToString("gradient.tex.ftl", map, this.getClass());
  }

  private List<IndexPoint> getIndexPoints() {
    if (indexPoints == null) {
      indexPoints = getIndex().getIndexPoints();
    }
    return indexPoints;
  }

  private NodeRangeIndex getIndex() {
    if (index == null) {
      index = new NodeRangeIndex(store, document);
    }
    return index;
  }

  private Set<Integer> getLongMarkupIndexes() {
    if (longMarkupIndexes == null) {
      longMarkupIndexes = new HashSet<>();
      for (int i = 0; i < document.getDocument().getMarkupIds().size(); i++) {
        Long markupId = document.getDocument().getMarkupIds().get(i);
        MarkupWrapper markup = store.getMarkupWrapper(markupId);
        if (document.containsAtLeastHalfOfAllTextNodes(markup)) {
          longMarkupIndexes.add(i);
        }
      }
    }
    return longMarkupIndexes;
  }

  private void appendGradedTAGDocument(StringBuilder latexBuilder, DocumentWrapper document) {
    long maxMarkupsPerTextNode = document.getTextNodeStream()
        .map(document::getMarkupStreamForTextNode)//
        .mapToLong(Stream::count)//
        .max()//
        .getAsLong();
    latexBuilder.append("\n    % TextNodes\n");
    if (document != null) {
      Set<MarkupWrapper> openMarkups = new LinkedHashSet<>();
      AtomicInteger textNodeCounter = new AtomicInteger(0);
      // Map<TextNodeWrapper, Integer> textNodeIndices = new HashMap<>();
      document.getTextNodeStream().forEach(tn -> {
        int i = textNodeCounter.getAndIncrement();
        // textNodeIndices.put(tn, i);
        Set<MarkupWrapper> markups = document.getMarkupStreamForTextNode(tn).collect(Collectors.toSet());

        List<MarkupWrapper> toClose = new ArrayList<>(openMarkups);
        toClose.removeAll(markups);
        Collections.reverse(toClose);

        List<MarkupWrapper> toOpen = new ArrayList<>(markups);
        toOpen.removeAll(openMarkups);

        openMarkups.removeAll(toClose);
        openMarkups.addAll(toOpen);

        long size = document.getMarkupStreamForTextNode(tn).count();
        float gradient = size / (float) maxMarkupsPerTextNode;

        int r = 255 - Math.round(255 * gradient);
        int g = 255;
        int b = 255 - Math.round(255 * gradient);
        Color color = new Color(r, g, b);
        String fillColor = ColorUtil.toLaTeX(color);

        addGradedTextNode(latexBuilder, tn, i, fillColor, size);
      });

    }
  }

  // private void drawMarkupsAsSets(StringBuilder latexBuilder, DocumentWrapper document, ColorPicker colorPicker, Map<TextNodeWrapper, Integer> textNodeIndices) {
  // document.getMarkupIdsForTextNodeIds().stream().map(store::getMarkup).forEach.forEach(tr -> {
  // String color = colorPicker.nextColor();
  // latexBuilder.append(" \\node[draw=").append(color).append(",shape=rectangle,fit=");
  // tr.textNodes.forEach(tn -> {
  // int i = textNodeIndices.get(tn);
  // latexBuilder.append("(tn").append(i).append(")");
  // });
  // latexBuilder.append(",label={[").append(color).append("]below:$").append(tr.getTag()).append("$}]{};\n");
  // });
  // }

  private void addGradedTextNode(StringBuilder latexBuilder, TextNodeWrapper tn, int i, String fillColor, long size) {
    String content = escapedContent(tn);
    String relPos = i == 0 ? "" : "right=0 of tn" + (i - 1);
    String nodeLine = "    \\node[textnode,fill=" + fillColor + "] (tn" + i + ") [" + relPos + "] {" + content + "};\n";
    latexBuilder.append(nodeLine);
  }

  private String escapedContent(TextNodeWrapper tn) {
    return tn.getText()//
        .replaceAll(" ", "\\\\s ")//
        .replaceAll("&", "\\\\& ")//
        .replaceAll("\n", "\\\\n ");
  }

  private void connectTextNodes(StringBuilder latexBuilder, AtomicInteger textNodeCounter) {
    latexBuilder.append("\n    % connect TextNodes\n    \\graph{").append("(doc)");
    for (int i = 0; i < textNodeCounter.get(); i++) {
      latexBuilder.append(" -> (tn").append(i).append(")");
    }
    latexBuilder.append("};\n");
  }

  private void markMarkups(StringBuilder latexBuilder, DocumentWrapper document, ColorPicker colorPicker, Map<TextNodeWrapper, Integer> textNodeIndices) {
    // AtomicInteger markupCounter = new AtomicInteger(0);
    latexBuilder.append("\n    % Markups");
    Map<MarkupWrapper, Integer> layerIndex = calculateLayerIndex(document.getMarkupStream().collect(toList()), textNodeIndices);
    document.getMarkupStream().forEach(tr -> {
      int rangeLayerIndex = layerIndex.get(tr);
      float markupRow = 0.75f * (rangeLayerIndex + 1);
      String color = colorPicker.nextColor();
      if (tr.isContinuous()) {
        List<Long> textNodeIds = tr.getMarkup().getTextNodeIds();
        TextNodeWrapper firstTextNode = store.getTextNodeWrapper(textNodeIds.get(0));
        TextNodeWrapper lastTextNode = store.getTextNodeWrapper(textNodeIds.get(textNodeIds.size() - 1));
        int first = textNodeIndices.get(firstTextNode);
        int last = textNodeIndices.get(lastTextNode);

        appendMarkup(latexBuilder, tr, String.valueOf(rangeLayerIndex), markupRow, color, first, last);

      } else {
        Iterator<TextNodeWrapper> textNodeIterator = tr.getTextNodeStream().iterator();
        TextNodeWrapper firstTextNode = textNodeIterator.next();
        TextNodeWrapper lastTextNode = firstTextNode;
        boolean finished = false;
        int partNo = 0;
        while (!finished) {
          TextNodeWrapper expectedNextNode = firstTextNode.getNextTextNode();
          boolean goOn = textNodeIterator.hasNext();
          while (goOn) {
            TextNodeWrapper nextTextNode = textNodeIterator.next();
            if (nextTextNode.equals(expectedNextNode)) {
              lastTextNode = nextTextNode;
              expectedNextNode = lastTextNode.getNextTextNode();
              goOn = textNodeIterator.hasNext();

            } else {
              appendMarkupPart(latexBuilder, textNodeIndices, tr, rangeLayerIndex, markupRow, color, firstTextNode, lastTextNode, partNo);
              firstTextNode = nextTextNode;
              lastTextNode = firstTextNode;
              partNo++;
              goOn = false;
            }
          }

          finished = finished || !textNodeIterator.hasNext();
        }

        appendMarkupPart(latexBuilder, textNodeIndices, tr, rangeLayerIndex, markupRow, color, firstTextNode, lastTextNode, partNo);

        for (int i = 0; i < partNo; i++) {
          String leftNode = MessageFormat.format("tr{0}_{1}e", rangeLayerIndex, i);
          String rightNode = MessageFormat.format("tr{0}_{1}b", rangeLayerIndex, (i + 1));
          latexBuilder.append("    \\draw[densely dashed,color=").append(color).append("] (").append(leftNode).append(".south) to [out=350,in=190] (").append(rightNode).append(".south);\n");
        }
      }

    });
  }

  private void appendMarkupPart(StringBuilder latexBuilder, Map<TextNodeWrapper, Integer> textNodeIndices, MarkupWrapper tr, int rangeLayerIndex, float markupRow, String color, TextNodeWrapper firstTextNode,
                                TextNodeWrapper lastTextNode, int partNo) {
    int first = textNodeIndices.get(firstTextNode);
    int last = textNodeIndices.get(lastTextNode);
    String markupPartNum = String.valueOf(rangeLayerIndex) + "_" + partNo;
    appendMarkup(latexBuilder, tr, markupPartNum, markupRow, color, first, last);
  }

  private void appendMarkup(StringBuilder latexBuilder, MarkupWrapper tr, String rangeLayerIndex, float markupRow, String color, int first, int last) {
    latexBuilder.append("\n    \\node[label=below right:{$")//
        .append(tr.getTag())//
        .append("$}](tr")//
        .append(rangeLayerIndex)//
        .append("b)[below left=")//
        .append(markupRow)//
        .append(" and 0 of tn")//
        .append(first)//
        .append("]{};\n");
    latexBuilder.append("    \\node[](tr")//
        .append(rangeLayerIndex)//
        .append("e)[below right=")//
        .append(markupRow)//
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

  private static class MarkupLayer {
    final Map<TextNodeWrapper, Integer> textNodeIndex;

    final List<MarkupWrapper> markups = new ArrayList<>();
    final Set<String> tags = new HashSet<>();
    int maxMarkupSize = 1; // the number of textnodes of the biggest markup
    int lastTextNodeUsed = 0;

    MarkupLayer(Map<TextNodeWrapper, Integer> textNodeIndex) {
      this.textNodeIndex = textNodeIndex;
    }

    void addMarkup(MarkupWrapper markup) {
      // LOG.info("markup={}", markup.getTag());
      markups.add(markup);
      tags.add(normalize(markup.getTag()));
      int size = markup.getMarkup().getTextNodeIds().size();
      maxMarkupSize = Math.max(maxMarkupSize, size);
      int lastIndex = size - 1;
      TextNodeWrapper lastTextNode = store.getTextNodeWrapper(markup.getMarkup().getTextNodeIds().get(lastIndex));
      lastTextNodeUsed = textNodeIndex.get(lastTextNode);
    }

    List<MarkupWrapper> getMarkups() {
      return markups;
    }

    public boolean hasTag(String tag) {
      return tags.contains(tag);
    }

    String getTag() {
      return tags.iterator().next();
    }

    int getMaxRangeSize() {
      return maxMarkupSize;
    }

    boolean canAdd(MarkupWrapper markup) {
      String nTag = normalize(markup.getTag());
      if (!tags.contains(nTag)) {
        return false;
      }
      TextNodeWrapper firstTextNode = store.getTextNodeWrapper(markup.getMarkup().getTextNodeIds().get(0));
      int firstTextNodeIndex = textNodeIndex.get(firstTextNode);
      return (firstTextNodeIndex > lastTextNodeUsed);
    }

    private String normalize(String tag) {
      return tag.replaceFirst("=.*$", "");
    }
  }

  private Map<MarkupWrapper, Integer> calculateLayerIndex(List<MarkupWrapper> markupList, Map<TextNodeWrapper, Integer> textNodeIndex) {
    List<MarkupLayer> layers = new ArrayList<>();
    markupList.forEach(tr -> {
      Optional<MarkupLayer> oLayer = layers.stream().filter(layer -> layer.canAdd(tr)).findFirst();
      if (oLayer.isPresent()) {
        oLayer.get().addMarkup(tr);

      } else {
        MarkupLayer layer = new MarkupLayer(textNodeIndex);
        layer.addMarkup(tr);
        layers.add(layer);
      }
    });

    AtomicInteger layerCounter = new AtomicInteger();
    Map<MarkupWrapper, Integer> index = new HashMap<>();
    layers.stream().sorted(ON_MAX_RANGE_SIZE).forEach(layer -> {
      int i = layerCounter.getAndIncrement();
      layer.getMarkups().forEach(tr -> index.put(tr, i));
    });

    return index;
  }

  private String toNodeContent(IndexPoint indexPoint, Set<Integer> longMarkupIndexes) {
    String content = indexPoint.toString();
    if (longMarkupIndexes.contains(indexPoint.getMarkupIndex())) {
      return "\\underline{" + content + "}";
    }
    return content;
  }

  private void appendChildTree(StringBuilder latexBuilder, KdTree.KdNode kdnode, String style, Set<Integer> longMarkupIndexes) {
    if (kdnode == null) {
      latexBuilder.append("[.\\node[").append(style).append("]{};\n]\n");
      return;
    }
    IndexPoint indexPoint = kdnode.getContent();
    String content = toNodeContent(indexPoint, longMarkupIndexes);
    latexBuilder.append("[.\\node[").append(style).append("]{").append(content).append("};\n");
    String nextStyle = (style.equals("textNodeAxis") ? "markupAxis" : "textNodeAxis");
    if (!(kdnode.getLesser() == null && kdnode.getGreater() == null)) {
      appendChildTree(latexBuilder, kdnode.getLesser(), nextStyle, longMarkupIndexes);
      appendChildTree(latexBuilder, kdnode.getGreater(), nextStyle, longMarkupIndexes);
    }
    latexBuilder.append("]\n");
  }

}
