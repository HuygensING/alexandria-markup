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

import java.util.List;

import static java.util.stream.Collectors.toList;

public class MarkupPath {

  private String path;

  public MarkupPath(final TAGMarkup tagMarkup, final TAGDocument tagDocument, final TAGStore store) {
    path = tagMarkup.getTag();
    TextGraph textGraph = tagDocument.getDTO().textGraph;
    final List<TAGMarkup> parentMarkup = textGraph
        .getIncomingEdges(tagMarkup.getDbId())
        .stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .filter(e -> e.hasType(EdgeType.hasMarkup))
        .map(textGraph::getSource)
        .map(store::getMarkup)
        .collect(toList());
    parentMarkup.size();

  }

  public String getPath() {
    return path;
  }
}
