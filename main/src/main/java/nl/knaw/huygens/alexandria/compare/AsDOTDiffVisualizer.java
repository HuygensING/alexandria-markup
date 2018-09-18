package nl.knaw.huygens.alexandria.compare;

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
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.apache.commons.text.StringEscapeUtils;
import prioritised_xml_collation.TAGToken;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class AsDOTDiffVisualizer implements DiffVisualizer {
  private final StringBuilder resultBuilder = new StringBuilder();
  private AtomicInteger nodeCounter = new AtomicInteger();
  private int startIndex;

  @Override
  public void startVisualization() {
    resultBuilder.append("digraph G{\n  rankdir=LR\n  node [shape=box]\n");
  }

  @Override
  public void startOriginal() {
    startIndex = nodeCounter.get();
    resultBuilder.append("  subgraph cluster_original{\n    rank=same\n    style=invis\n");
  }

  @Override
  public void originalTextNode(final TAGTextNode t) {
    resultBuilder.append("    tn")
        .append(nodeCounter.getAndIncrement())
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
        .append("    ").append(originalTextEdges).append("\n")
        .append("  }\n");
  }

  @Override
  public void startDiff() {
    startIndex = nodeCounter.get();
    resultBuilder.append("  subgraph cluster_diff{\n    rank=same\n    style=invis\n");
  }

  @Override
  public void startAligned() {
    resultBuilder.append("    tn")
        .append(nodeCounter.getAndIncrement())
        .append(" [label=<");
  }

  @Override
  public void alignedTextToken(final TAGToken t) {
    resultBuilder
        .append(escapedContent(t));
  }

  @Override
  public void endAligned() {
    resultBuilder.append(">]\n");
  }

  @Override
  public void startAddition() {
    resultBuilder
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [shape=point]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [label=<");
  }

  @Override
  public void addedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endAddition() {
    resultBuilder
        .append(">]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [shape=point]\n");
  }

  @Override
  public void startOmission() {
    resultBuilder
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [shape=point]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [label=<");
  }

  @Override
  public void omittedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endOmission() {
    resultBuilder
        .append(">]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [shape=point]\n");
  }

  @Override
  public void startReplacement() {
    resultBuilder
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [shape=point]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [label=<");
  }

  @Override
  public void originalTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void replacementSeparator() {
    resultBuilder
        .append(">]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [label=<");
  }

  @Override
  public void editedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endReplacement() {
    resultBuilder
        .append(">]\n")
        .append("    tn").append(nodeCounter.getAndIncrement()).append(" [shape=point]\n");
  }

  @Override
  public void endDiff() {
    resultBuilder.append("  }\n");
  }

  @Override
  public void startEdited() {
    startIndex = nodeCounter.get();
    resultBuilder.append("  subgraph cluster_edited{\n    rank=same\n    style=invis\n");
  }

  @Override
  public void editedTextNode(final TAGTextNode t) {
    resultBuilder.append("    tn")
        .append(nodeCounter.getAndIncrement())
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
    resultBuilder.append("}\n");
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
}
