package nl.knaw.huc.di.tag.model.graph;

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

import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

class TextNodeIdIterator implements Iterator<Long> {
  private final TextGraph textGraph;
  private final List<TypedNode> nodesToProcess = new ArrayList<>();
  private final Set<Long> textHandled = new HashSet<>();
  private final Set<String> layers;

  private Optional<Long> nextTextNodeId;

  public TextNodeIdIterator(final TextGraph textGraph, final Long markupId, final Set<String> layers) {
    this.textGraph = textGraph;
    this.layers = layers;
    this.nodesToProcess.add(0, new TypedNode(NodeType.markup, markupId));
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
    return getContinuedMarkupIds(id).stream()
        .flatMap(markupId ->
            textGraph.getOutgoingEdges(markupId).stream()
                .filter(LayerEdge.class::isInstance)
                .map(LayerEdge.class::cast)
                .filter(e -> e.hasAnyLayerFrom(layers))
                .flatMap(this::toTypedNodeStream)
        ).collect(toList());
  }

  private List<Long> getContinuedMarkupIds(final Long id) {
    List<Long> precedingMarkupIds = getPrecedingMarkupIds(id);
    List<Long> continuedMarkupIds = new ArrayList<>(precedingMarkupIds);
    continuedMarkupIds.add(id);
    List<Long> succeedingMarkupIds = getSucceedingMarkupIds(id);
    continuedMarkupIds.addAll(succeedingMarkupIds);
    return continuedMarkupIds;
  }

  private List<Long> getPrecedingMarkupIds(final Long id) {
    List<Long> precedingMarkupIds = new ArrayList<>();
    textGraph.getPrecedingMarkupId(id)
        .ifPresent(precedingMarkupId -> {
          precedingMarkupIds.add(0, precedingMarkupId);
          precedingMarkupIds.addAll(0, getPrecedingMarkupIds(precedingMarkupId));
        });
    return precedingMarkupIds;
  }

  private List<Long> getSucceedingMarkupIds(final Long id) {
    List<Long> succeedingMarkupIds = new ArrayList<>();
    textGraph.getContinuedMarkupId(id)
        .ifPresent(continuedMarkupId -> {
          succeedingMarkupIds.add(continuedMarkupId);
          succeedingMarkupIds.addAll(getSucceedingMarkupIds(continuedMarkupId));
        });
    return succeedingMarkupIds;
  }

  private List<TypedNode> getContinuationChildren(final Long id) {
    Optional<Long> continuedMarkup = textGraph.getContinuedMarkupId(id);
    return continuedMarkup.isPresent()
        ? getChildren(continuedMarkup.get())
        : new ArrayList<>();
  }

  private Stream<TypedNode> toTypedNodeStream(final LayerEdge layerEdge) {
    NodeType targetType = layerEdge.hasType(EdgeType.hasText)
        ? NodeType.text
        : NodeType.markup;
    return textGraph.getTargets(layerEdge).stream()
        .map(id -> new TypedNode(targetType, id));
  }

}
