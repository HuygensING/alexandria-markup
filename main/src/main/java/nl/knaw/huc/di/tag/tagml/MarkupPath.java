package nl.knaw.huc.di.tag.tagml;

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
