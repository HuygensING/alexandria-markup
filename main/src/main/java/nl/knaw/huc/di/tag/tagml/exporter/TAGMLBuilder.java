package nl.knaw.huc.di.tag.tagml.exporter;

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

import nl.knaw.huc.di.tag.TAGVisitor;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;

public class TAGMLBuilder implements TAGVisitor {
  String result = "";
  StringBuilder tagmlBuilder = new StringBuilder();

  @Override
  public void enterDocument(final TAGDocument document) {

  }

  @Override
  public void exitDocument(final TAGDocument document) {
    result = tagmlBuilder.toString()
        .replace("[:branches>[:branch>", TAGML.DIVERGENCE)
        .replace("<:branch][:branch>", TAGML.DIVIDER)
        .replace("<:branch]<:branches]", TAGML.CONVERGENCE)
    ;
  }

  @Override
  public void enterOpenTag(final TAGMarkup markup) {

  }

  @Override
  public void addAnnotation(final AnnotationInfo annotationInfo) {

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
}
