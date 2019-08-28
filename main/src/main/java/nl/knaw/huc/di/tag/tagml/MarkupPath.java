package nl.knaw.huc.di.tag.tagml;

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

import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class MarkupPath {

  private String path;

  public MarkupPath(final TAGMarkup tagMarkup, final TAGDocument tagDocument, final TAGStore store) {
//    path = tagMarkup.getTag();
    List<String> pathParts = new ArrayList<>();
    pathParts.add(tagMarkup.getTag());
    TextGraph textGraph = tagDocument.getDTO().textGraph;

    boolean hasParents = true;
    TAGMarkup markup = tagMarkup;
    while (hasParents) {
      final List<TAGMarkup> parentMarkup = textGraph
          .getIncomingEdges(markup.getDbId())
          .stream()
          .filter(LayerEdge.class::isInstance)
          .map(LayerEdge.class::cast)
          .filter(e -> e.hasType(EdgeType.hasMarkup))
          .map(textGraph::getSource)
          .filter(l -> !textGraph.isRootNode(l))
          .map(store::getMarkup)
          .collect(toList());
      if (!parentMarkup.isEmpty()) {
        pathParts.add(0, parentMarkup.get(0).getTag());
        markup = parentMarkup.get(0);
      } else {
        hasParents = false;
      }
    }
    path = pathParts.stream().collect(joining("/"));
  }

  public String getPath() {
    return path;
  }
}
