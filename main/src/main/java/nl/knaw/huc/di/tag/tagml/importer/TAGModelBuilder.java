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
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface TAGModelBuilder {
  void exitDocument(final Map<String, String> namespaces);

  boolean isFirstTag();

  void addLayer(String newLayerId, TAGMarkupDAO markup, String parentLayer);

  void openMarkupInLayer(TAGMarkupDAO markup, String layerId);

  void persist(TAGMarkupDAO markup);

  void enterRichTextValue();

  void exitRichTextValue();

  TAGMarkupDAO addMarkup(String tagName);

  void closeMarkupInLayer(TAGMarkupDAO markup, String layerName);

  TAGMarkupDAO getMarkup(Long rootMarkupId);

  TAGTextNodeDAO getLastTextNode();

  Stream<TAGMarkupDAO> getMarkupStreamForTextNode(TAGTextNodeDAO previousTextNode);

  TAGMarkupDAO resumeMarkup(TAGMarkupDAO suspendedMarkup, Set<String> layers);

  void associateTextNodeWithMarkupForLayer(TAGTextNodeDAO tn, TAGMarkupDAO markup, String layerName);

  void addRefAnnotation(TAGMarkupDAO markup, String aName, String refId);

  void addBasicAnnotation(TAGMarkupDAO markup, TAGMLParser.BasicAnnotationContext actx);

  TAGMarkupDAO createMarkup(String extendedTag);

  void addMarkup(TAGMarkupDAO markup);

  List<TAGMarkupDAO> getRelevantOpenMarkup(Deque<TAGMarkupDAO> allOpenMarkup);

  TAGTextNodeDAO createConnectedTextNode(String s, Deque<TAGMarkupDAO> allOpenMarkup);

  TAGDocumentDAO getDocument();

  Long persist(TAGDTO tagdto);

  ErrorListener getErrorListener();
}
