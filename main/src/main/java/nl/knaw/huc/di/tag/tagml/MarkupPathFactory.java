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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class MarkupPathFactory {

  private final TAGDocument tagDocument;
  private final TAGStore store;

  public MarkupPathFactory(final TAGDocument tagDocument, final TAGStore store) {
    this.tagDocument = tagDocument;
    this.store = store;
  }

  public String getPath(final TAGMarkup tagMarkup) {
    String layer = "";
    if (!tagMarkup.getLayers().isEmpty()) {
      List<String> nonDefaultLayers =
          tagMarkup.getLayers().stream()
              .filter(l -> !l.equals(TAGML.DEFAULT_LAYER))
              .collect(toList());
      if (!nonDefaultLayers.isEmpty()) {
        //        Preconditions.checkState(nonDefaultLayers.size() == 1, nonDefaultLayers.toString()
        // + " should have size 1");
        layer = join(",", nonDefaultLayers);
      }
    }

    List<String> pathParts = new ArrayList<>();
    pathParts.add(tagMarkup.getTag());
    TextGraph textGraph = tagDocument.getDTO().textGraph;

    boolean hasParents = true;
    TAGMarkup markup = tagMarkup;
    while (hasParents) {
      Long childId = markup.getDbId();
      final Set<TAGMarkup> parentMarkup =
          textGraph.getIncomingEdges(childId).stream()
              .filter(LayerEdge.class::isInstance)
              .map(LayerEdge.class::cast)
              .filter(e -> e.hasType(EdgeType.hasMarkup))
              .map(textGraph::getSource)
              .filter(l -> !textGraph.isRootNode(l))
              .map(store::getMarkup)
              .collect(toSet());
      if (parentMarkup.isEmpty()) {
        hasParents = false;

      } else {
        Preconditions.checkState(
            parentMarkup.size() == 1, parentMarkup.toString() + " should have size 1");
        TAGMarkup parent = parentMarkup.iterator().next(); // there can be only one!
        String childTag = markup.getTag();
        List<Long> twins =
            textGraph
                .getOutgoingEdges(
                    parent.getDbId()) // children with the same tag as the original markup
                .stream()
                .filter(LayerEdge.class::isInstance)
                .map(LayerEdge.class::cast)
                .filter(e -> e.hasType(EdgeType.hasMarkup))
                .map(textGraph::getTargets)
                .flatMap(Collection::stream)
                .map(store::getMarkup)
                .filter(m -> m.hasTag(childTag))
                .map(TAGMarkup::getDbId)
                .distinct()
                .collect(toList());
        if (twins.size() > 1) {
          int childIndex = twins.indexOf(childId) + 1;
          final String child = pathParts.get(0) + "[" + childIndex + "]";
          pathParts.set(0, child);
        }
        pathParts.add(0, parent.getTag());
        markup = parent;
      }
    }

    String path = join("/", pathParts);
    if (!layer.isEmpty()) {
      path += "|" + layer;
    }
    return path;
  }
}
