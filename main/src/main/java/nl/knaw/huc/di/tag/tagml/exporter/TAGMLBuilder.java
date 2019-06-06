package nl.knaw.huc.di.tag.tagml.exporter;

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

import nl.knaw.huc.di.tag.TAGVisitor;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.List;
import java.util.Set;

import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class TAGMLBuilder implements TAGVisitor {
  String result = "";
  StringBuilder tagmlBuilder = new StringBuilder();

  @Override
  public void setView(final TAGView tagView) {

  }

  @Override
  public void enterDocument(final TAGDocument document) {

  }

  @Override
  public void exitDocument(final TAGDocument document) {
    result = tagmlBuilder.toString()
        .replace(BRANCHES_START + BRANCH_START, TAGML.DIVERGENCE)
        .replace(BRANCH_END + BRANCH_START, TAGML.DIVIDER)
        .replace(BRANCH_END + BRANCHES_END, TAGML.CONVERGENCE)
    ;
  }

  @Override
  public void enterOpenTag(final TAGMarkup markup) {

  }

  @Override
  public void addAnnotation(String serializedAnnotation) {

  }

  @Override
  public String serializeAnnotationAssigner(String name) {
    return null;
  }

  @Override
  public void exitOpenTag(final TAGMarkup markup) {

  }

  @Override
  public void exitCloseTag(final TAGMarkup markup) {

  }

  @Override
  public void exitText(final String text, final boolean inVariation) {
    String escapedText = inVariation
        ? TAGML.escapeVariantText(text)
        : TAGML.escapeRegularText(text);
    tagmlBuilder.append(escapedText);
  }

  @Override
  public void enterTextVariation() {

  }

  @Override
  public void exitTextVariation() {

  }

  @Override
  public void setRelevantLayers(final Set<String> relevantLayers) {

  }

  @Override
  public String serializeStringAnnotationValue(String stringValue) {
    return null;
  }

  @Override
  public String serializeNumberAnnotationValue(Double numberValue) {
    return null;
  }

  @Override
  public String serializeBooleanAnnotationValue(Boolean booleanValue) {
    return null;
  }

  @Override
  public String serializeListAnnotationValue(List<String> serializedItems) {
    return null;
  }

  @Override
  public String serializeMapAnnotationValue(List<String> serializedMapItems) {
    return null;
  }
}
