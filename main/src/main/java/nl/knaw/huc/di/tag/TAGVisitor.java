package nl.knaw.huc.di.tag;

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

import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.List;
import java.util.Set;

public interface TAGVisitor {
  void setView(TAGView tagView);

  void enterDocument(TAGDocument document);

  void exitDocument(TAGDocument document);

  void enterOpenTag(TAGMarkup markup);

  void addAnnotation(String serializedAnnotation);

  String serializeAnnotationAssigner(String name);

  String serializeStringAnnotationValue(String stringValue);

  String serializeNumberAnnotationValue(Double numberValue);

  String serializeBooleanAnnotationValue(Boolean booleanValue);

  String serializeListAnnotationValue(List<String> serializedItems);

  String serializeMapAnnotationValue(List<String> serializedMapItems);

  void exitOpenTag(TAGMarkup markup);

  void exitCloseTag(TAGMarkup markup);

  void exitText(String escapedText, final boolean inVariation);

  void enterTextVariation();

  void exitTextVariation();

  void setRelevantLayers(Set<String> relevantLayers);
}
