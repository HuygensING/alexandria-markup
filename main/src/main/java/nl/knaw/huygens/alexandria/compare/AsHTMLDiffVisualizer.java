package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup-core
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;

public class AsHTMLDiffVisualizer implements DiffVisualizer {
  private final StringBuilder resultBuilder = new StringBuilder();

  private final StringBuilder originalBuilder = new StringBuilder();
  private final Map<Long, AtomicInteger> originalColSpan = new HashMap<>();
  private String originalText;

  private final StringBuilder diffBuilder = new StringBuilder();

  private final StringBuilder editedBuilder = new StringBuilder();
  private final Map<Long, AtomicInteger> editedColSpan = new HashMap<>();
  private String editedText;
  private TAGToken lastOriginalTextToken;
  private TAGToken lastEditedTextToken;

  @Override
  public void startVisualization() {
    resultBuilder.append("<style>\n")
        .append(".markup-original { background-color:#66ccff}\n")
        .append(".original { background-color:lightcyan}\n")
        .append(".edited { background-color:palegreen}\n")
        .append(".markup-edited { background-color:#33cc33}\n")
        .append("table, th, td {border: 1px solid black; border-collapse: collapse;}\n")
        .append("table table, table table th, table table td {border: 0px; border-collapse: collapse; padding: 0;}")
        .append("</style>\n")
        .append("<table border=\"1\">\n");
  }

  @Override
  public void startOriginal(final String witness1) {
    originalBuilder.append("  <tr>\n    <th class=\"original\">").append(witness1).append("</th>\n");
  }

  @Override
  public void originalTextNode(final TAGTextNode t) {
    Long textNodeId = t.getDbId();
    originalBuilder.append("    <td class=\"original\" colspan=\"original")
        .append(textNodeId)
        .append("\">")
        .append(escapedText(t.getText()))
        .append("</td>\n");
  }

  @Override
  public void endOriginal() {
    originalBuilder.append("  </tr>\n");
  }

  @Override
  public void startDiff(final String witness1, final String witness2) {
    diffBuilder.append("  <tr>\n    <th>").append(witness1).append("/").append(witness2).append("</th>\n");
  }

  @Override
  public void startAligned() {
    diffBuilder.append("    <td class=\"aligned\">");
  }

  @Override
  public void alignedTextTokens(final List<TAGToken> tokensWa, final List<TAGToken> tokensWb) {
    tokensWa.forEach(t -> diffBuilder.append(escapedContent(t)));
    incrementOriginalColSpan(tokensWa);
    incrementEditedColSpan(tokensWb);
  }

  private void incrementOriginalColSpan(final List<TAGToken> originalTextTokens) {
    originalTextTokens.stream()
        .map(ExtendedTextToken.class::cast)
        .map(ExtendedTextToken::getTextNodeIds)
        .flatMap(List::stream)
        .distinct()
        .forEach(textNodeId -> {
      originalColSpan.putIfAbsent(textNodeId, new AtomicInteger(0));
      originalColSpan.get(textNodeId).getAndIncrement();
    });
    lastOriginalTextToken = originalTextTokens.get(originalTextTokens.size() - 1);
  }

  private void incrementEditedColSpan(final List<TAGToken> editedTextTokens) {
    editedTextTokens.stream()
        .map(ExtendedTextToken.class::cast)
        .map(ExtendedTextToken::getTextNodeIds)
        .flatMap(List::stream)
        .distinct()
        .forEach(textNodeId -> {
          editedColSpan.putIfAbsent(textNodeId, new AtomicInteger(0));
          editedColSpan.get(textNodeId).getAndIncrement();
        });
    lastEditedTextToken = editedTextTokens.get(editedTextTokens.size() - 1);
  }

  @Override
  public void endAligned() {
    diffBuilder.append("</td>\n");
  }

  @Override
  public void startAddition() {
    diffBuilder.append("    <td class=\"addition\">");
  }

  @Override
  public void addedTextToken(final TAGToken t) {
    originalText = "&nbsp;";
    editedText = escapedContent(t);
    incrementOriginalColSpan(singletonList(lastOriginalTextToken));
    incrementEditedColSpan(singletonList(t));
  }

  @Override
  public void endAddition() {
    addTable();
  }

  @Override
  public void startOmission() {
    diffBuilder.append("    <td class=\"omission\">");
  }

  @Override
  public void omittedTextToken(final TAGToken t) {
    originalText = escapedContent(t);
    editedText = "&nbsp;";
    incrementOriginalColSpan(singletonList(t));
    incrementEditedColSpan(singletonList(lastEditedTextToken));
  }

  @Override
  public void endOmission() {
    addTable();
  }

  @Override
  public void startReplacement() {
    diffBuilder.append("    <td class=\"replacement\">");
  }

  @Override
  public void originalTextToken(final TAGToken t) {
    originalText = escapedContent(t);
    incrementOriginalColSpan(singletonList(t));
  }

  @Override
  public void replacementSeparator() {
  }

  @Override
  public void editedTextToken(final TAGToken t) {
    editedText = escapedContent(t);
    incrementEditedColSpan(singletonList(t));
  }

  @Override
  public void endReplacement() {
    addTable();
  }

  private void addTable() {
    diffBuilder
        .append("<table width=\"100%\"><tr><td class=\"edited\">")
        .append(editedText)
        .append("</td></tr><tr><td class=\"original\">")
        .append(originalText)
        .append("</td></tr></table></td>\n");
  }

  @Override
  public void endDiff() {
    diffBuilder.append("  </tr>\n");
  }

  @Override
  public void startEdited(final String witness2) {
    editedBuilder.append("  <tr>\n    <th class=\"edited\">").append(witness2).append("</th>\n");
  }

  @Override
  public void editedTextNode(final TAGTextNode t) {
    Long textNodeId = t.getDbId();
    editedBuilder.append("    <td class=\"edited\" colspan=\"edited")
        .append(textNodeId)
        .append("\">")
        .append(escapedText(t.getText()))
        .append("</td>\n");
  }

  @Override
  public void endEdited() {
    editedBuilder.append("  </tr>\n");
  }

  @Override
  public void endVisualization() {
    String originalMarkup = getOriginalMarkup();
    String original = originalBuilder.toString();
    for (long k : originalColSpan.keySet()) {
      original = original.replace(
          "original" + k + "\"",
          originalColSpan.get(k) + "\""
      );
    }

    String editedMarkup = getEditedMarkup();
    String edited = editedBuilder.toString();
    for (long k : editedColSpan.keySet()) {
      edited = edited.replace(
          "edited" + k + "\"",
          editedColSpan.get(k) + "\""
      );
    }
    resultBuilder
        .append(editedMarkup)
        .append(edited)
        .append(diffBuilder)
        .append(original)
        .append(originalMarkup)
        .append("</table>\n");
  }

  private String getEditedMarkup() {
    return "" ;
  }

  private String getOriginalMarkup() {
    return "";
  }

  @Override
  public String getResult() {
    return resultBuilder.toString();
  }

  private String escapedContent(final TAGToken t) {
    return escapedText(t.content);
  }

  private String escapedText(final String text) {
    String normalized = text.replace("\n", "\\n").replace(" ", "_");
    return StringEscapeUtils.escapeHtml3(normalized)
        .replace(" ", "&nbsp;");
  }
}
