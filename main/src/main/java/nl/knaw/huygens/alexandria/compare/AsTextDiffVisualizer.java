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
import prioritised_xml_collation.TAGToken;

public class AsTextDiffVisualizer implements DiffVisualizer {
  private final StringBuilder resultBuilder = new StringBuilder();

  @Override
  public void startVisualization() {
  }

  @Override
  public void startOriginal() {
  }

  @Override
  public void originalTextNode(final TAGTextNode t) {
    resultBuilder.append("[")
        .append(escapedText(t.getText()))
        .append("]");
  }

  @Override
  public void endOriginal() {
    resultBuilder.append("\n");
  }

  @Override
  public void startDiff() {
  }

  @Override
  public void startAligned() {
    resultBuilder.append("[|");
  }

  @Override
  public void alignedTextToken(final TAGToken t) {
    resultBuilder.append("[")
        .append(escapedContent(t))
        .append("]");
  }

  @Override
  public void endAligned() {
  }

  @Override
  public void startAddition() {
  }

  @Override
  public void addedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endAddition() {
    resultBuilder.append("]");
  }

  @Override
  public void startOmission() {
    resultBuilder.append("[");
  }

  @Override
  public void omittedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endOmission() {
    resultBuilder.append("|]");
  }

  @Override
  public void startReplacement() {
    resultBuilder.append("[");
  }

  @Override
  public void originalTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void replacementSeparator() {
    resultBuilder.append("|");
  }

  @Override
  public void editedTextToken(final TAGToken t) {
    resultBuilder.append(escapedContent(t));
  }

  @Override
  public void endReplacement() {
    resultBuilder.append("]");
  }

  @Override
  public void endDiff() {
    resultBuilder.append("\n");
  }

  @Override
  public void startEdited() {
  }

  @Override
  public void editedTextNode(final TAGTextNode t) {
    resultBuilder.append("[")
        .append(escapedText(t.getText()))
        .append("]");
  }

  @Override
  public void endEdited() {
    resultBuilder.append("\n");
  }

  @Override
  public void endVisualization() {
  }

  @Override
  public String getResult() {
    return resultBuilder.toString();
  }

  private String escapedText(final String text) {
    return text.replace("\n", "\\n");
  }

  private String escapedContent(final TAGToken t) {
    return escapedText(t.content);
  }
}
