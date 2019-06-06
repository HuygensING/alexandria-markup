package nl.knaw.huc.di.tag.tagml.importer;

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

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGDocumentDAO;
import nl.knaw.huygens.alexandria.storage.TAGMarkupDAO;
import nl.knaw.huygens.alexandria.storage.TAGTextNodeDAO;
import nl.knaw.huygens.alexandria.storage.dto.TAGElement;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class TAGKnowledgeModelBuilder implements TAGModelBuilder {

  private ErrorListener errorListener;

  public TAGKnowledgeModelBuilder(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void exitDocument(final Map<String, String> namespaces) {

  }

  @Override
  public boolean isFirstTag() {
    return false;
  }

  @Override
  public void addLayer(final String newLayerId, final TAGMarkupDAO markup, final String parentLayer) {
  }

  @Override
  public TAGMarkupDAO addMarkup(final String tagName) {
    return new TAGMarkupDAO(null, null);
  }

  @Override
  public void addMarkup(final TAGMarkupDAO markup) {

  }

  @Override
  public TAGMarkupDAO createMarkup(final String extendedTag) {
    return null;
  }

  @Override
  public TAGMarkupDAO getMarkup(final Long rootMarkupId) {
    return null;
  }

  @Override
  public void openMarkupInLayer(final TAGMarkupDAO markup, final String layerId) {

  }

  @Override
  public void closeMarkupInLayer(final TAGMarkupDAO markup, final String layerName) {

  }

  @Override
  public void enterRichTextValue() {

  }

  @Override
  public void exitRichTextValue() {

  }

  @Override
  public TAGTextNodeDAO getLastTextNode() {
    return null;
  }

  @Override
  public Stream<TAGMarkupDAO> getMarkupStreamForTextNode(final TAGTextNodeDAO previousTextNode) {
    return null;
  }

  @Override
  public TAGMarkupDAO resumeMarkup(final TAGMarkupDAO suspendedMarkup, final Set<String> layers) {
    return null;
  }

  @Override
  public TAGTextNodeDAO createConnectedTextNode(final String s, final Deque<TAGMarkupDAO> allOpenMarkup) {
    return null;
  }

  @Override
  public void associateTextNodeWithMarkupForLayer(final TAGTextNodeDAO tn, final TAGMarkupDAO markup, final String layerName) {

  }

  @Override
  public void addRefAnnotation(final TAGMarkupDAO markup, final String aName, final String refId) {

  }

  @Override
  public void addBasicAnnotation(final TAGMarkupDAO markup, final TAGMLParser.BasicAnnotationContext actx) {

  }

  @Override
  public List<TAGMarkupDAO> getRelevantOpenMarkup(final Deque<TAGMarkupDAO> allOpenMarkup) {
    return null;
  }

  @Override
  public TAGDocumentDAO getDocument() {
    return null;
  }

  @Override
  public void persist(final TAGMarkupDAO markup) {

  }

  @Override
  public Long persist(final TAGElement tagElement) {
    return null;
  }

  @Override
  public ErrorListener getErrorListener() {
    return errorListener;
  }

  public String asTurtle() {
    return "";
  }
}
