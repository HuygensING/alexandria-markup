package nl.knaw.huygens.alexandria.storage;

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
import nl.knaw.huc.di.tag.model.graph.edges.ContinuationEdge;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

public abstract class AbstractTAGStore implements TAGStore {

  public Stream<TAGMarkup> getMarkupStream(TAGDocument document) {
    return document.getMarkupIds().stream()
        .map(this::getMarkup);
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkup(TAGDocument document, final TAGMarkup markup) {
    return getTextNodeStreamForMarkupInLayers(document, markup, document.textGraph.getLayerNames());
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkupInLayers(TAGDocument document, final TAGMarkup markup, Set<String> layers) {
    return document.textGraph
        .getTextNodeIdStreamForMarkupIdInLayers(markup.getDbId(), layers)
        .map(this::getTextNode);
  }

  public boolean isSuspended(TAGDocument document, TAGMarkup markup) {
    return document.textGraph
        .getOutgoingEdges(markup.getDbId())
        .stream()
        .anyMatch(ContinuationEdge.class::isInstance);
  }

  public boolean isAnonymous(TAGDocument document, TAGMarkup markup) {
    List<TAGTextNode> textNodesForMarkup = getTextNodeStreamForMarkup(document, markup)
        .collect(toList());
    return textNodesForMarkup.size() == 1 // markup has just 1 textnode
        && textNodesForMarkup.get(0).getText().isEmpty();  // and it's empty
  }

}
