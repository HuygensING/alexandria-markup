package nl.knaw.huc.di.tag.model.graph;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import nl.knaw.huc.di.tag.tagml.importer2.TAGKnowledgeModel;
import nl.knaw.huygens.alexandria.exporter.ColorPicker;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.apache.commons.lang3.StringEscapeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static nl.knaw.huc.di.tag.tagml.TAGML.BRANCH;
import static nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES;

public class DotFactory {
  private ColorPicker colorPicker = new ColorPicker("red", "blue", "darkgray", "gray", "green", "lightgray", //
      "lime", "magenta", "olive", "orange", "pink", "purple", "brown", "cyan", "teal", "violet", "black");
  Map<String, String> layerColor = new HashMap<>();
  private TextGraph textGraph;

  public String toDot(TAGKnowledgeModel knowledgeModel, final String label) {
    layerColor.clear();
    StringBuilder dotBuilder = new StringBuilder("digraph TextGraph{\n")
        .append("  node [font=\"helvetica\";style=\"filled\";fillcolor=\"white\"]\n")
        .append("  d [shape=doublecircle;label=\"\"]\n")
        .append("  subgraph{\n");
    // TODO

    return dotBuilder.append("}").toString();
  }

  public String toDot(TAGDocument document, final String label) {
    layerColor.clear();
    StringBuilder dotBuilder = new StringBuilder("digraph TextGraph{\n")
        .append("  node [font=\"helvetica\";style=\"filled\";fillcolor=\"white\"]\n")
        .append("  d [shape=doublecircle;label=\"\"]\n")
        .append("  subgraph{\n");
    document.getTextNodeStream().map(this::toTextNodeLine).forEach(dotBuilder::append);

    dotBuilder.append("    rank=same\n");

    textGraph = document.getDTO().textGraph;
    AtomicLong prevNode = new AtomicLong(-1);
    textGraph.getTextNodeIdStream().forEach(id -> {
      if (prevNode.get() != -1) {
        dotBuilder.append(toNextEdgeLine(prevNode.get(), id));
      }
      prevNode.set(id);
    });

    dotBuilder.append("  }\n");

    document.getMarkupStream().map(this::toMarkupNodeLine).forEach(dotBuilder::append);

    document.getMarkupStream().map(this::toMarkupContinuationLine).forEach(dotBuilder::append);

    document.getMarkupStream()
        .map(TAGMarkup::getDbId)
        .flatMap(id -> textGraph
            .getOutgoingEdges(id).stream()
            .filter(LayerEdge.class::isInstance)
            .map(LayerEdge.class::cast))
        .map(e -> toOutgoingEdgeLine(e, textGraph))
        .forEach(dotBuilder::append);

    textGraph.getOutgoingEdges(textGraph.documentNode)
        .stream()
        .flatMap(e -> textGraph.getTargets(e).stream())
        .map(root -> "  d->m" + root + " [arrowhead=none]\n")
        .forEach(dotBuilder::append);

    String graphLabel = escape(label);
    if (!graphLabel.isEmpty()) {
      dotBuilder.append("  label=<<font color=\"brown\" point-size=\"8\"><i>").append(graphLabel).append("</i></font>>\n");
    }

    dotBuilder.append("}");
    return dotBuilder.toString();
  }

  private String escape(final String label) {
    return StringEscapeUtils.escapeHtml4(label)
        .replaceAll("\n", "\\\\n")
//        .replace(" ", "_")
        ;
  }

  private String toTextNodeLine(final TAGTextNode textNode) {
    String shape = "box";
    String templateStart = "    t%d [shape=%s;arrowhead=none;label=";
    String templateEnd = "]\n";

    if (textNode.getText().isEmpty()) {
      return format(templateStart + "\"\"" + templateEnd, textNode.getDbId(), shape);

    } else {
      String textPrefix = "#PCDATA<br/>";
      return format(templateStart + "<%s%s>" + templateEnd, textNode.getDbId(), shape, textPrefix, escape(textNode.getText()));
    }
  }

  private String toNextEdgeLine(final Long textNode0, Long textNode1) {
    return format("    t%d->t%d [color=invis;arrowhead=none;label=\"\"]\n", textNode0, textNode1);
  }

  private String toMarkupNodeLine(final TAGMarkup markup) {
    if (markup.getExtendedTag().startsWith(BRANCHES)) {
      return format("  m%d [shape=triangle;color=red;label=\"\"]\n", markup.getDbId());

    } else if (markup.getExtendedTag().startsWith(BRANCH)) {
      return format("  m%d [shape=point;color=red]\n", markup.getDbId());

    }
    StringBuilder pre = new StringBuilder();
    StringBuilder post = new StringBuilder();
    Iterator<String> layerIterator = markup.getLayers().iterator();
    String layerName = layerIterator.next();
    String color = getLayerColor(layerName);
    while (layerIterator.hasNext()) {
      String otherLayer = layerIterator.next();
      String otherColor = getLayerColor(otherLayer);
      pre.append("  subgraph cluster_").append(markup.getDbId()).append(otherLayer).append("{\n")
          .append("    style=rounded\n    color=").append(otherColor).append("\n  ");
      post.append("  }\n");
    }
    return format("%s  m%d [color=%s;label=<%s>]\n%s", pre, markup.getDbId(), color, markup.getExtendedTag(), post);
  }

  private String toMarkupContinuationLine(final TAGMarkup tagMarkup) {
    Optional<Long> continuedMarkup = textGraph.getContinuedMarkupId(tagMarkup.getDbId());
    if (continuedMarkup.isPresent()) {
      return format("  m%d->m%d [color=red;style=dashed;arrowhead=none]\n", tagMarkup.getDbId(), continuedMarkup.get());
    }
    return "";
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
