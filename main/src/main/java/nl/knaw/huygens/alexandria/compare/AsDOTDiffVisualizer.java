package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.apache.commons.text.StringEscapeUtils;
import prioritised_xml_collation.TAGToken;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AsDOTDiffVisualizer implements DiffVisualizer {
  private final StringBuilder resultBuilder = new StringBuilder();
  private final AtomicInteger nodeCounter = new AtomicInteger();
  private int startIndex;
  private final Map<Long, String> textNodeVariables = new HashMap<>();
  private final SetMultimap<Long, String> edges = MultimapBuilder.hashKeys().hashSetValues().build();
  private String lastNode;
  private final Stack<List<String>> replacements = new Stack<>();

  @Override
  public void startVisualization() {
    resultBuilder
        .append("digraph G{\n")
        .append("  rankdir=LR\n")
        .append("  edge [arrowhead=none]\n")
        .append("  node [shape=box]\n");
  }

  @Override
  public void startOriginal(final String witness1) {
    startIndex = nodeCounter.get();
    resultBuilder
        .append("  subgraph cluster_original{\n")
        .append("    node[color=red]\n")
        .append("    edge[color=red]\n")
        .append("    style=invis\n")
        .append("    O [shape=ellipse;style=bold;label=<").append(witness1).append(">]\n")
        .append("    O -> tn").append(startIndex).append("\n");
  }

  @Override
  public void originalTextNode(final TAGTextNode t) {
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    textNodeVariables.put(t.getDbId(), nodeVariable);
    resultBuilder.append("    ")
        .append(nodeVariable)
        .append(" [label=<")
        .append(escapedText(t.getText()))
        .append(">]\n");
  }

  @Override
  public void endOriginal() {
    List<String> originalNodes = new ArrayList<>();
    for (int i = startIndex; i < nodeCounter.get(); i++) {
      originalNodes.add("tn" + i);
    }
    String originalTextEdges = String.join("->", originalNodes);
    resultBuilder
        .append("    ").append(originalTextEdges).append(" \n")
        .append("  }\n");
  }

  @Override
  public void startDiff(final String witness1, final String witness2) {
    startIndex = nodeCounter.get();
    resultBuilder
        .append("  subgraph cluster_diff{\n")
        .append("    node[color=brown]\n")
        .append("    edge[color=brown]\n")
        .append("    style=invis\n")
        .append("    OE [shape=ellipse;style=bold;label=<").append(witness1).append("/").append(witness2).append(">]\n")
        .append("    OE -> tn").append(startIndex).append("\n");
    lastNode = "";
  }

  @Override
  public void startAligned() {
    String nodeVariable = "tn" + nodeCounter.get();
    resultBuilder.append("    ")
        .append(nodeVariable)
        .append(" [label=<");
  }

  @Override
  public void alignedTextTokens(final List<TAGToken> tokensWa, final List<TAGToken> tokensWb) {
    String nodeVariable = "tn" + nodeCounter.get();
    for (TAGToken t : tokensWa) {
      resultBuilder.append(escapedContent(t));
      ExtendedTextToken tt = (ExtendedTextToken) t;
      for (Long textNodeId : tt.getTextNodeIds()) {
        edges.put(textNodeId, nodeVariable);
      }
    }
    for (TAGToken t : tokensWb) {
      ExtendedTextToken tt = (ExtendedTextToken) t;
      for (Long textNodeId : tt.getTextNodeIds()) {
        edges.put(textNodeId, nodeVariable);
      }
    }
  }

  @Override
  public void endAligned() {
    resultBuilder.append(">]\n");
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    addEdge(lastNode, nodeVariable);
    lastNode = nodeVariable;
  }

  @Override
  public void startAddition() {
    String startAdditionNode = "tn" + nodeCounter.getAndIncrement();
    resultBuilder.append("    ").append(startAdditionNode).append(" [shape=point]\n");
    addEdge(lastNode, startAdditionNode);
    lastNode = startAdditionNode;
    resultBuilder.append("    tn").append(nodeCounter.get()).append(" [label=<");
  }

  @Override
  public void addedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
    addEdges(t);
  }

  @Override
  public void endAddition() {
    String startAdditionNode = lastNode;
    resultBuilder.append(">]\n");
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    addEdge(startAdditionNode, nodeVariable);

    String endAdditionNode = "tn" + nodeCounter.getAndIncrement();
    resultBuilder.append("    ").append(endAdditionNode).append(" [shape=point]\n");
    addEdge(startAdditionNode, endAdditionNode);
    addEdge(nodeVariable, endAdditionNode);
    lastNode = endAdditionNode;
  }

  @Override
  public void startOmission() {
    String startOmissionNode = "tn" + nodeCounter.getAndIncrement();
    resultBuilder.append("    ").append(startOmissionNode).append(" [shape=point]\n");
    addEdge(lastNode, startOmissionNode);
    lastNode = startOmissionNode;
    resultBuilder.append("    tn").append(nodeCounter.get()).append(" [label=<");
  }

  @Override
  public void omittedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
    addEdges(t);
  }

  @Override
  public void endOmission() {
    String startOmissionNode = lastNode;
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    resultBuilder
        .append(">]\n");
    addEdge(startOmissionNode, nodeVariable);

    String endOmissionNodeVariable = "tn" + nodeCounter.getAndIncrement();
    resultBuilder.append("    ").append(endOmissionNodeVariable).append(" [shape=point]\n");
    addEdge(startOmissionNode, endOmissionNodeVariable);
    addEdge(nodeVariable, endOmissionNodeVariable);
    lastNode = endOmissionNodeVariable;
  }

  @Override
  public void startReplacement() {
    replacements.push(new ArrayList<>());
    String startReplacementNode = "tn" + nodeCounter.getAndIncrement();
    resultBuilder.append("    ").append(startReplacementNode).append(" [shape=point]\n");
    addEdge(lastNode, startReplacementNode);
    lastNode = startReplacementNode;
    resultBuilder.append("    tn").append(nodeCounter.get()).append(" [label=<");
  }

  @Override
  public void originalTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
    addEdges(t);
  }

  @Override
  public void replacementSeparator() {
    resultBuilder.append(">]\n");
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    replacements.peek().add(nodeVariable);

    nodeVariable = "tn" + nodeCounter.get();
    resultBuilder.append("    ").append(nodeVariable).append(" [label=<");
  }

  @Override
  public void editedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
    addEdges(t);
  }

  @Override
  public void endReplacement() {
    resultBuilder.append(">]\n");
    String startReplacementNode = lastNode;
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    replacements.peek().add(nodeVariable);

    String endReplacementNode = "tn" + nodeCounter.getAndIncrement();
    resultBuilder.append("    ").append(endReplacementNode).append(" [shape=point]\n");

    List<String> options = replacements.pop();
    options.forEach(option -> {
      addEdge(startReplacementNode, option);
      addEdge(option, endReplacementNode);
    });
    lastNode = endReplacementNode;
  }

  @Override
  public void endDiff() {
    resultBuilder.append("  }\n");
  }

  @Override
  public void startEdited(final String witness2) {
    startIndex = nodeCounter.get();
    resultBuilder
        .append("  subgraph cluster_edited{\n")
        .append("    node[color=blue]\n")
        .append("    edge[color=blue]\n")
        .append("    style=invis\n")
        .append("    E [shape=ellipse;style=bold;label=<").append(witness2).append(">]\n")
        .append("    E -> tn").append(startIndex).append("\n");
  }

  @Override
  public void editedTextNode(final TAGTextNode t) {
    String nodeVariable = "tn" + nodeCounter.getAndIncrement();
    textNodeVariables.put(t.getDbId(), nodeVariable);
    resultBuilder.append("    ")
        .append(nodeVariable)
        .append(" [label=<")
        .append(escapedText(t.getText()))
        .append(">]\n");
  }

  @Override
  public void endEdited() {
    List<String> editedNodes = new ArrayList<>();
    for (int i = startIndex; i < nodeCounter.get(); i++) {
      editedNodes.add("tn" + i);
    }
    String editedTextEdges = String.join("->", editedNodes);
    resultBuilder
        .append("    ").append(editedTextEdges).append("\n")
        .append("  }\n");
  }

  @Override
  public void endVisualization() {
    edges.forEach((textNodeId, nodeVariable) -> {
      String leftNodeVariable = textNodeVariables.get(textNodeId);
      resultBuilder
//          .append("  rank=same{").append(leftNodeVariable).append(" ").append(nodeVariable).append("}\n")
          .append("  ").append(leftNodeVariable).append("->").append(nodeVariable).append(" [style=dashed;arrowhead=none]\n");
    });
    resultBuilder.append("rank=same{O OE E}\n")
        .append("}\n");
  }

  @Override
  public String getResult() {
    return resultBuilder.toString();
  }

  private String escapedContent(final TAGToken t) {
    return escapedText(t.content);
  }

  private String escapedText(final String text) {
    String normalized = text.replace("\n", "\\n");
    return StringEscapeUtils.escapeHtml3(normalized)
        .replace(" ", "&nbsp;");
  }

  private void addEdge(final String leftNode, final String rightNode) {
    if (!leftNode.isEmpty()) {
      resultBuilder.append("    ").append(leftNode).append("->").append(rightNode).append("\n");
    }
  }

  private void addEdges(final TAGToken t) {
    String nodeVariable = "tn" + nodeCounter.get();
    ExtendedTextToken tt = (ExtendedTextToken) t;
    for (Long textNodeId : tt.getTextNodeIds()) {
      edges.put(textNodeId, nodeVariable);
    }
  }
}
