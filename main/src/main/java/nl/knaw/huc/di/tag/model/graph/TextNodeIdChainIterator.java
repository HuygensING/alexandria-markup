package nl.knaw.huc.di.tag.model.graph;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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
import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class TextNodeIdChainIterator implements Iterator<Long> {
  private TextGraph textGraph;
  private List<TypedNode> nodesToProcess = new ArrayList<>();
  private Optional<Long> nextTextNodeId;
  private Set<Long> textHandled = new HashSet<>();

  public TextNodeIdChainIterator(final TextGraph textGraph, Long rootNode) {
    this.textGraph = textGraph;
    this.nodesToProcess.add(0, new TypedNode(NodeType.markup, rootNode));
    this.nextTextNodeId = calcNextTextNodeId();
  }

  @Override
  public boolean hasNext() {
    return nextTextNodeId.isPresent();
  }

  @Override
  public Long next() {
    Long nodeId = nextTextNodeId.get();
    textHandled.add(nodeId);
    nextTextNodeId = calcNextTextNodeId();
    return nodeId;
  }

  private Optional<Long> calcNextTextNodeId() {
    Optional<Long> nextTextNodeId = Optional.empty();
    if (nodesToProcess.isEmpty()) {
      nextTextNodeId = Optional.empty();

    } else {
      TypedNode nextTypedNode = nodesToProcess.remove(0);
      Long nextId = nextTypedNode.id;
      if (nextTypedNode.isText()) {
        if (textHandled.contains(nextId)) {
          nextTextNodeId = calcNextTextNodeId();
        } else {
          textHandled.add(nextId);
          nextTextNodeId = Optional.of(nextId);
        }
      } else {
        List<TypedNode> children = getChildren(nextId);
        nodesToProcess.addAll(0, children);
        nextTextNodeId = calcNextTextNodeId();
      }
    }
    return nextTextNodeId;
  }

  private List<TypedNode> getChildren(final Long id) {
    return textGraph.getOutgoingEdges(id).stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .flatMap(this::toTypedNodeStream)
        .collect(toList());
  }

  private Stream<TypedNode> toTypedNodeStream(final LayerEdge layerEdge) {
    NodeType targetType = layerEdge.hasType(EdgeType.hasText)
        ? NodeType.text
        : NodeType.markup;
    return textGraph.getTargets(layerEdge).stream()
        .map(id -> new TypedNode(targetType, id));
  }

}
