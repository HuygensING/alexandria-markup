package nl.knaw.huc.di.tag.model.graph;

/*-
 * #%L
 * alexandria-markup-core
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

import com.sleepycat.persist.model.NotPersistent;
import com.sleepycat.persist.model.Persistent;
import nl.knaw.huc.di.tag.model.graph.edges.*;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.model.graph.edges.EdgeType.hasText;
import static nl.knaw.huc.di.tag.model.graph.edges.Edges.markupContinuation;
import static nl.knaw.huygens.alexandria.StreamUtil.stream;

@Persistent
public class TextGraph extends HyperGraph<Long, Edge> {
  @NotPersistent
  Logger LOG = LoggerFactory.getLogger(getClass());

  String id = "";
  Long documentNode;
  Map<String, Long> layerRootMap = new LinkedHashMap<>();
  Map<String, String> parentLayerMap = new HashMap<>();
  Long firstTextNodeId;

  public TextGraph() {
    super(GraphType.ORDERED);
  }

  public TextGraph setLayerRootMarkup(final String layerName, final Long markupNodeId) {
    layerRootMap.put(layerName, markupNodeId);
//    addChildMarkup(documentNode, TAGML.DEFAULT_LAYER, markupNodeId);
    return this;
  }

  public TextGraph addChildMarkup(final Long parentMarkupId, final String layerName, final Long childMarkupId) {
    final LayerEdge edge = Edges.parentMarkupToChildMarkup(layerName);
    addDirectedHyperEdge(edge, edge.label(), parentMarkupId, childMarkupId);
    return this;
  }

  public TextGraph linkMarkupToTextNodeForLayer(final Long markupId, final Long textNodeId, final String layerName) {
//    List<LayerEdge> existingEdges = existingMarkupToTextNodeEdgesForMarkupAndLayer(markupId, layerName);
//    if (existingEdges.isEmpty()) {
    final LayerEdge edge = Edges.markupToText(layerName);
    addDirectedHyperEdge(edge, edge.label(), markupId, textNodeId);

//    } else if (existingEdges.size() > 1) {
//      throw new RuntimeException("There should be only one outgoing markupToText hyperedge for this layer!");
//
//    } else {
//      final LayerEdge existingEdge = existingEdges.get(0);
//      addTargetsToHyperEdge(existingEdge, textNodeId);
//    }
    return this;
  }

  private List<LayerEdge> existingMarkupToTextNodeEdgesForMarkupAndLayer(final Long markupId, final String layerName) {
    return getOutgoingEdges(markupId).stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .filter(e -> e.hasType(hasText))
        .filter(e -> e.hasLayer(layerName))
        .collect(toList());
  }

  public void unlinkMarkupFromTextNodeForLayer(final Long markupId, final Long textNodeId, final String layerName) {
    List<LayerEdge> existingEdges = existingMarkupToTextNodeEdgesForMarkupAndLayer(markupId, layerName);
    if (existingEdges.isEmpty()) {
      throw new RuntimeException("No edge found to unlink!");

    } else if (existingEdges.size() > 1) {
      throw new RuntimeException("There should be only one outgoing markupToText hyperedge for this layer!");

    } else {
      final LayerEdge existingEdge = existingEdges.get(0);
      removeTargetsFromHyperEdge(existingEdge, textNodeId);
    }
  }

  public Stream<Long> getTextNodeIdStream() {
    return stream(new TextNodeIdChainIterator(this, documentNode));
  }

  public Set<String> getLayerNames() {
    return layerRootMap.keySet();
  }

  public Map<String, Long> getLayerRootMap() {
    return layerRootMap;
  }

  public Stream<Long> getTextNodeIdStreamForLayer(final String layerName) {
    return getTextNodeIdStream().filter(id -> belongsToLayer(id, layerName));
  }

  private boolean belongsToLayer(final Long id, final String layerName) {
    return getIncomingEdges(id).stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .anyMatch(e -> e.hasLayer(layerName));
  }

  public Stream<Long> getMarkupIdStreamForTextNodeId(final Long textNodeId, final String layerName) {
    return stream(new Iterator<Long>() {
      Optional<Long> next = getParentMarkup(textNodeId);

      private Optional<Long> getParentMarkup(Long nodeId) {
        return getIncomingEdges(nodeId).stream()
            .filter(LayerEdge.class::isInstance)
            .map(LayerEdge.class::cast)
            .filter(e -> e.hasLayer(layerName))
            .map(e -> getSource(e))
            .findFirst();
      }

      @Override
      public boolean hasNext() {
        return next.isPresent();
      }

      @Override
      public Long next() {
        Long nodeId = next.get();
        next = getParentMarkup(nodeId);
        return nodeId;
      }
    });
  }

  public Stream<Long> getMarkupIdStreamForTextNodeId0(final Long textNodeId) {
    return stream(new Iterator<Long>() {
      Deque<Long> markupToProcess = new ArrayDeque<>(getParentMarkupList(textNodeId));
      Optional<Long> next = calcNext();
      Set<Long> markupHandled = initializeMarkupHandled();

      private Set<Long> initializeMarkupHandled() {
        HashSet<Long> set = new HashSet<>();
        set.add(documentNode);
        return set;
      }

      private Optional<Long> calcNext() {
        return markupToProcess.isEmpty()
            ? Optional.empty()
            : Optional.of(markupToProcess.pop());
      }

      private List<Long> getParentMarkupList(Long nodeId) {
        return getIncomingEdges(nodeId).stream()
            .filter(LayerEdge.class::isInstance)
            .map(LayerEdge.class::cast)
            .map(e -> getSource(e))
            .collect(toList());
      }

      @Override
      public boolean hasNext() {
        return next.isPresent();
      }

      @Override
      public Long next() {
        Long nodeId = next.get();
        markupHandled.add(nodeId);
        List<Long> parentMarkupList = getParentMarkupList(nodeId);
        parentMarkupList.removeAll(markupHandled);
        markupToProcess.addAll(parentMarkupList);
        next = calcNext();
        return nodeId;
      }
    });
  }

  public Stream<Long> getMarkupIdStreamForTextNodeId(final Long textNodeId) {
    Set<Long> markupIds = new HashSet<>();
    Set<Long> nodesHandled = new HashSet<>();
    nodesHandled.add(documentNode);
    Deque<Long> nodesToProcess = new ArrayDeque<>(singleton(textNodeId));
    // collect all markupNodeIds that are connected to the given textNodeId
    while (!nodesToProcess.isEmpty()) {
      Long nodeId = nodesToProcess.pop();
      markupIds.add(nodeId);
      nodesHandled.add(nodeId);
      getIncomingEdges(nodeId).stream()
          .filter(LayerEdge.class::isInstance)
          .map(LayerEdge.class::cast)
          .map(this::getSource)
          .filter(id -> !nodesHandled.contains(id))
          .forEach(nodesToProcess::add);
    }
    markupIds.remove(textNodeId);

    // For each connected node we have to determine the distance from the root ( = depth)
    Map<Long, Integer> nodeDepth = new HashMap<>();
    nodesToProcess.addAll(layerRootMap.values());
    layerRootMap.values().forEach(v -> nodeDepth.put(v, 0));
    nodesHandled.clear();
    while (!nodesToProcess.isEmpty()) {
      Long nodeId = nodesToProcess.pop();
//      LOG.info("nodeId={}", nodeId);
      nodesHandled.add(nodeId);
      int nextDepth = nodeDepth.get(nodeId) + 1;
      getOutgoingEdges(nodeId).stream()
          .filter(LayerEdge.class::isInstance)
          .map(LayerEdge.class::cast)
          .map(this::getTargets)
          .flatMap(Collection::stream)
//          .peek(System.out::println)
          .filter(id -> !nodesHandled.contains(id))
          .filter(markupIds::contains)
          .forEach(n -> {
            nodesToProcess.add(n);
            nodeDepth.put(n, nextDepth);
          });
    }

    final Comparator<Long> maxDistanceComparator = comparing(nodeDepth::get);
    Comparator<Long> comparator = maxDistanceComparator.thenComparing(identity());
    return markupIds.stream().sorted(comparator);
  }

  public Stream<Long> getMarkupIdStreamForTextNodeIdOrderedByDistanceFromTextNode(final Long textNodeId) {
    // TODO: This traversal revisits markup nodes because the maxDistance might have changed.
    //       The maxDistance is needed for the ordering, to get the markupIds back ordered by the maximum distance from the textnode, closest first
    Set<Long> markupIds = new HashSet<>();
    Map<Long, Integer> maxDistance = new HashMap<>();
    List<Long> parentMarkupIds = getIncomingEdges(textNodeId).stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .map(this::getSource)
        .collect(toList());
    parentMarkupIds.remove(documentNode);
    parentMarkupIds.forEach(m -> maxDistance.put(m, 1));
    Set<Long> markupHandled = new HashSet<>();
    markupHandled.add(documentNode);
    Deque<Long> markupToProcess = new ArrayDeque<>(parentMarkupIds);
    while (!markupToProcess.isEmpty()) {
      Long nodeId = markupToProcess.pop();
      markupIds.add(nodeId);
      markupHandled.add(nodeId);
      List<Long> parentMarkupList = getIncomingEdges(nodeId).stream()
          .filter(LayerEdge.class::isInstance)
          .map(LayerEdge.class::cast)
          .map(this::getSource)
          .collect(toList());
      parentMarkupList.remove(documentNode);
      int currentDistance = maxDistance.get(nodeId);
      int newMaxParentDistance = currentDistance + 1;
      parentMarkupList.forEach(m -> {
        if (!maxDistance.containsKey(m) || maxDistance.get(m) < newMaxParentDistance) {
          maxDistance.put(m, newMaxParentDistance);
        }
      });
      markupToProcess.addAll(parentMarkupList);
    }
    final Comparator<Long> maxDistanceComparator = comparing(maxDistance::get);
    Comparator<Long> comparator = maxDistanceComparator.thenComparing(reverseOrder());
    return markupIds.stream().sorted(comparator);
  }

  public Stream<Long> getTextNodeIdStreamForMarkupIdInLayer(final Long markupId, final String layer) {
    Set<String> layers = new HashSet<>();
    layers.add(layer);
    return getTextNodeIdStreamForMarkupIdInLayers(markupId, layers);
  }

  public Stream<Long> getTextNodeIdStreamForMarkupIdInLayers(final Long markupId, final Set<String> layers) {
    return stream(new TextNodeIdIterator(this, markupId, layers));
  }

  public TextGraph setFirstTextNodeId(final Long firstTextNodeId) {
    this.firstTextNodeId = firstTextNodeId;
    return this;
  }

  public Long getFirstTextNodeId() {
    return this.firstTextNodeId;
  }

  public Map<String, String> getParentLayerMap() {
    return parentLayerMap;
  }

  public void setDocumentRoot(final Long node) {
    documentNode = node;
  }

  public void linkParentlessLayerRootsToDocument() {
    layerRootMap.values().stream()
        .distinct()
        .filter(r -> getIncomingEdges(r).stream().noneMatch(e -> e instanceof LayerEdge && ((LayerEdge) e).hasType(EdgeType.hasMarkup)))
        .forEach(r -> addChildMarkup(documentNode, TAGML.DEFAULT_LAYER, r));
  }

  public void continueMarkup(TAGMarkup suspendedMarkup, TAGMarkup resumedMarkup) {
    ContinuationEdge edge = markupContinuation();
    addDirectedHyperEdge(edge, edge.getLabel(), suspendedMarkup.getDbId(), resumedMarkup.getDbId());
  }

  public Optional<Long> getContinuedMarkupId(final Long id) {
    return getOutgoingEdges(id).stream()
        .filter(ContinuationEdge.class::isInstance)
        .map(this::getTargets)
        .flatMap(Collection::stream)
        .findFirst();
  }

  public Optional<Long> getPrecedingMarkupId(final Long id) {
    return getIncomingEdges(id).stream()
        .filter(ContinuationEdge.class::isInstance)
        .map(this::getSource)
        .findFirst();
  }

  public void addAnnotationEdge(final Long sourceNode, final AnnotationInfo targetAnnotation) {
    AnnotationEdge edge = new AnnotationEdge(targetAnnotation.getType(), targetAnnotation.getName());
    Long annotationValueNode = targetAnnotation.getNodeId();
    addDirectedHyperEdge(edge, edge.getLabel(), sourceNode, annotationValueNode);
  }

  public void addListItem(Long sourceNode, AnnotationInfo targetAnnotation) {
    ListItemEdge edge = new ListItemEdge(targetAnnotation.getType());
    Long annotationValueNode = targetAnnotation.getNodeId();
    addDirectedHyperEdge(edge, edge.getLabel(), sourceNode, annotationValueNode);
  }

}
