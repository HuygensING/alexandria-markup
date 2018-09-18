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

public class AsHTMLDiffVisualizer implements DiffVisualizer {
  private final StringBuilder resultBuilder = new StringBuilder();

  @Override
  public void startVisualization() {
    resultBuilder.append("<table border=\"1\">\n");
  }

  @Override
  public void startOriginal() {
    resultBuilder.append("  <tr>\n    <th>Original</th>\n");
  }

  @Override
  public void originalTextNode(final TAGTextNode t) {
    resultBuilder.append("    <td class=\"original\">")
        .append(escapedText(t.getText()))
        .append("</td>\n");
  }

  @Override
  public void endOriginal() {
    resultBuilder.append("  </tr>\n");
  }

  @Override
  public void startDiff() {
    resultBuilder.append("  <tr>\n    <th>Diff</th>\n");
  }

  @Override
  public void startAligned() {
    resultBuilder.append("    <td class=\"aligned\">");
  }

  @Override
  public void alignedTextToken(final TAGToken t) {
    resultBuilder
        .append(escapedContent(t));
  }

  @Override
  public void endAligned() {
    resultBuilder.append("</td>\n");
  }

  @Override
  public void startAddition() {
    resultBuilder.append("    <td class=\"addition\">&nbsp;<br/>");
  }

  @Override
  public void addedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endAddition() {
    resultBuilder.append("</td>\n");
  }

  @Override
  public void startOmission() {
    resultBuilder.append("    <td class=\"omission\">");
  }

  @Override
  public void omittedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endOmission() {
    resultBuilder.append("<br/>&nbsp;</td>\n");
  }

  @Override
  public void startReplacement() {
    resultBuilder.append("    <td class=\"replacement\">");
  }

  @Override
  public void originalTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void replacementSeparator() {
    resultBuilder.append("<br/>");
  }

  @Override
  public void editedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endReplacement() {
    resultBuilder.append("</td>\n");
  }

  @Override
  public void endDiff() {
    resultBuilder.append("  </tr>\n");
  }

  @Override
  public void startEdited() {
    resultBuilder.append("  <tr>\n    <th>Edited</th>\n");
  }

  @Override
  public void editedTextNode(final TAGTextNode t) {
    resultBuilder.append("    <td class=\"edited\">")
        .append(escapedText(t.getText()))
        .append("</td>\n");
  }

  @Override
  public void endEdited() {
    resultBuilder.append("  </tr>\n");

  }

  @Override
  public void endVisualization() {
    resultBuilder.append("</table>\n");
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
