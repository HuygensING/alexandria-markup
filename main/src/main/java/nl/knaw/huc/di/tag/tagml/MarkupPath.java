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

import com.google.common.base.Preconditions;
import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MarkupPath {

  private String path;

  public MarkupPath(final TAGMarkup tagMarkup, final TAGDocument tagDocument, final TAGStore store) {
//    path = tagMarkup.getTag();

    String layer = "";
    if (!tagMarkup.getLayers().isEmpty()) {
      List<String> nonDefaultLayers = tagMarkup.getLayers().stream()
          .filter(l -> !l.equals(TAGML.DEFAULT_LAYER))
          .collect(toList());
      if (!nonDefaultLayers.isEmpty()) {
        Preconditions.checkState(nonDefaultLayers.size() == 1, nonDefaultLayers.toString() + " should have size 1");
        layer = nonDefaultLayers.get(0);
      }
    }

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
      if (parentMarkup.isEmpty()) {
        hasParents = false;

      } else {
        Preconditions.checkState(parentMarkup.size() == 1, parentMarkup.toString() + " should have size 1");
        int childIndex = 1;
        final String child = pathParts.get(0) + "[" + childIndex + "]";
        pathParts.remove(0);
        pathParts.add(0, child);
        TAGMarkup parent = parentMarkup.get(0); // there can be only one!
        pathParts.add(0, parent.getTag());
        markup = parent;
      }
    }
    path = String.join("/", pathParts);
    if (!layer.isEmpty()) {
      path += "|" + layer;
    }
  }

  public String getPath() {
    return path;
  }
}
