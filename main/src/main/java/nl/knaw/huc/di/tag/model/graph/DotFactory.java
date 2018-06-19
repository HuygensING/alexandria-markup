package nl.knaw.huc.di.tag.model.graph;

/*-
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

import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huygens.alexandria.exporter.ColorPicker;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;

public class DotFactory {
  private ColorPicker colorPicker = new ColorPicker("brown", "cyan", "darkgray", "gray", "green", "lightgray", //
      "lime", "magenta", "olive", "orange", "pink", "purple", "red", "teal", "violet", "black");
  Map<String, String> layerColor = new HashMap<>();

  public String toDot(TAGDocument document, final String label) {
    layerColor.clear();
    StringBuilder dotBuilder = new StringBuilder("digraph TextGraph{\n")
        .append("  node [style=\"filled\";fillcolor=\"white\"]\n")
        .append("  subgraph{\n");
    document.getTextNodeStream().map(this::toTextNodeLine).forEach(dotBuilder::append);

//    String sameRank = document.getTextNodeStream()
//        .map(TAGTextNode::getDbId)
//        .map(i -> "t" + i)
//        .collect(joining(";"));
    dotBuilder.append("    rank=same\n");

    TextGraph textGraph = document.getDTO().textGraph;
    document.getTextNodeStream()
        .map(TAGTextNode::getDbId)
        .flatMap(id -> textGraph
            .getOutgoingEdges(id).stream()
            .filter(TextChainEdge.class::isInstance)
            .map(TextChainEdge.class::cast))
        .map(e -> toNextEdgeLine(e, textGraph))
        .forEach(dotBuilder::append);

    dotBuilder.append("  }\n");

    document.getMarkupStream().map(this::toMarkupNodeLine).forEach(dotBuilder::append);

    document.getMarkupStream()
        .map(TAGMarkup::getDbId)
        .flatMap(id -> textGraph
            .getOutgoingEdges(id).stream()
            .filter(LayerEdge.class::isInstance)
            .map(LayerEdge.class::cast))
        .map(e -> toOutgoingEdgeLine(e, textGraph))
        .forEach(dotBuilder::append);

    String graphLabel = escape(label);
    if (!graphLabel.isEmpty()) {
      dotBuilder.append("  label=<<font color=\"brown\" point-size=\"8\"><i>" + graphLabel + "</i></font>>\n");
    }

    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private String escape(final String label) {
    return StringEscapeUtils.escapeHtml4(label)
        .replaceAll("\n", "\\\\n")
        .replace(" ", "_");
  }

  private String toTextNodeLine(final TAGTextNode textNode) {
    String shape = (textNode.isConvergence() || textNode.isDivergence())
        ? "diamond"
        : "box";
    String label;
    String templateStart = "    t%d [shape=%s;color=blue;arrowhead=none;label=";
    String templateEnd = "]\n";
    if (textNode.isDivergence()) {
      return format(templateStart + "\"<\"" + templateEnd, textNode.getDbId(), shape);

    } else if (textNode.isConvergence()) {
      return format(templateStart + "\">\"" + templateEnd, textNode.getDbId(), shape);

    } else if (textNode.getText().isEmpty()) {
      return format(templateStart + "\"\"" + templateEnd, textNode.getDbId(), shape);

    } else {
      return format(templateStart + "<%s>" + templateEnd, textNode.getDbId(), shape, escape(textNode.getText()));
    }

  }

  private String toNextEdgeLine(final TextChainEdge edge, TextGraph textGraph) {
    Long source = textGraph.getSource(edge);
    String targets = textGraph.getTargets(edge).stream().map(i -> "t" + i).collect(joining(","));
    return format("    t%d->{%s}[color=white;arrowhead=none;label=<%s>]\n", source, targets, "");
  }

  private String toMarkupNodeLine(final TAGMarkup markup) {
    if (markup.getExtendedTag().startsWith(":branches")) {
      return format("  m%d [shape=triangle;color=red;label=\"\"]\n", markup.getDbId());

    } else if (markup.getExtendedTag().startsWith(":branch")) {
      return format("  m%d [shape=point;color=red]\n", markup.getDbId());

    }
    return format("  m%d [color=red;label=<%s>]\n", markup.getDbId(), markup.getExtendedTag());
  }

  private String toOutgoingEdgeLine(LayerEdge edge, TextGraph textGraph) {
    Long source = textGraph.getSource(edge);
    String targetPrefix = edge.hasType(EdgeType.hasText) ? "t" : "m";
    Collection<Long> edgeTargets = textGraph.getTargets(edge);
    String targets = edgeTargets.stream().map(i -> targetPrefix + i).collect(joining(","));
    String layerName = edge.getLayerName();
    String label = layerName.isEmpty()
        ? ""
        : ";label=<<font point-size=\"8\">" + layerName + "</font>>";
    String color = getLayerColor(layerName);
    if (edgeTargets.size() == 1) {
      return format("  m%d->%s[color=%s;arrowhead=none%s]\n", source, targets, color, label);

    } else {
      String hyperId = "h" + source + layerName;
      return format("  %s [shape=point;color=%s;label=\"\"]\n" +
              "  m%d->%s [color=%s;arrowhead=none%s]\n" +
              "  %s->{%s}[color=%s;arrowhead=none]\n",
          hyperId, color,
          source, hyperId, color, label,
          hyperId, targets, color);
    }
  }

  private String getLayerColor(final String layerName) {
    return layerColor.computeIfAbsent(layerName, k -> colorPicker.nextColor());
  }

}
