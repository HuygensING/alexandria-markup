package nl.knaw.huygens.alexandria.storage;

/*
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

import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.Edge;
import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huc.di.tag.model.graph.edges.MarkupToTextHyperEdge;
import nl.knaw.huc.di.tag.tagml.importer.RangePair;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER;

public class TAGDocument {
  Logger LOG = LoggerFactory.getLogger(TAGDocument.class);
  public final TAGStore store;

  private final TAGDocumentDTO documentDTO;
  private final Map<String, Deque<TAGMarkup>> openMarkupStackForLayer = new HashMap<>();

  public TAGDocument(TAGStore store, TAGDocumentDTO documentDTO) {
    this.store = store;
    this.documentDTO = documentDTO;
  }

  public TAGDocumentDTO getDTO() {
    return documentDTO;
  }

  public long getDbId() {
    return documentDTO.getDbId();
  }

  public Date getCreationDate() {
    return documentDTO.getCreationDate();
  }

  public Date getModificationDate() {
    return documentDTO.getModificationDate();
  }

  public Map<Long, RangePair> getMarkupRangeMap() {
    return documentDTO.getMarkupRangeMap();
  }

  public void setMarkupRangeMap(Map<Long, RangePair> markupRangeMap) {
    documentDTO.setMarkupRangeMap(markupRangeMap);
  }

  public TAGDocument addTextNode(TAGTextNode textNode, List<TAGMarkup> lastOpenedMarkup) {
    List<Long> textNodeIds = documentDTO.getTextNodeIds();
    Long textNodeDbId = textNode.getDbId();
    textNodeIds.add(textNodeDbId);
    if (textNodeIds.size() == 1) {
      documentDTO.setFirstTextNodeId(textNodeDbId);
    }
    if (lastOpenedMarkup != null) {
      lastOpenedMarkup.forEach(
          m -> {
            Long dbId = m.getDbId();
            Set<String> relevantLayers = new HashSet<>(m.getLayers());
            if (relevantLayers.size() > 1) {
              relevantLayers.remove(DEFAULT_LAYER);
            }
            relevantLayers.forEach(
                l -> documentDTO.textGraph.linkMarkupToTextNodeForLayer(dbId, textNodeDbId, l));
          });
    }
    update();
    return this;
  }

  public Stream<TAGTextNode> getTextNodeStream() {
    return documentDTO.getTextNodeIds().stream().map(store::getTextNode);
  }

  public Stream<TAGMarkup> getMarkupStream() {
    return documentDTO.getMarkupIds().stream().map(store::getMarkup);
  }

  public TAGTextNode getFirstTextNode() {
    return store.getTextNode(documentDTO.getFirstTextNodeId());
  }

  public TAGTextNode getLastTextNode() {
    //    TextGraph textGraph = documentDTO.textGraph;
    //    Long rootMarkup = textGraph.getLayerRootMap().get(TAGML.DEFAULT_LAYER);
    //    List<Edge> outgoingEdges = (List<Edge>) textGraph.getOutgoingEdges(rootMarkup);
    //    final Edge last = outgoingEdges.get(outgoingEdges.size() - 1);
    // TODO: make this more efficient
    List<TAGTextNode> textNodes = getTextNodeStream().collect(toList());
    return textNodes.get(textNodes.size() - 1);
  }

  public boolean hasTextNodes() {
    return documentDTO.getFirstTextNodeId() != null;
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TAGMarkup tagMarkup) {
    return documentDTO.containsAtLeastHalfOfAllTextNodes(tagMarkup.getDTO());
  }

  public TAGDocument addMarkup(TAGMarkupDTO markup) {
    Long id = markup.getDbId();
    return addMarkupId(id);
  }

  public TAGDocument addMarkup(TAGMarkup markup) {
    Long id = markup.getDbId();
    return addMarkupId(id);
  }

  public Iterator<TAGTextNodeDTO> getTextNodeIterator() {
    return getTagTextNodeStream() //
        .iterator();
  }

  public void associateTextNodeWithMarkupForLayer(TAGTextNode tagTextNode, TAGMarkup tagMarkup) {
    associateTextNodeWithMarkupForLayer(tagTextNode, tagMarkup, DEFAULT_LAYER);
  }

  public void associateTextNodeWithMarkupForLayer(
      TAGTextNode tagTextNode, TAGMarkup tagMarkup, String layerName) {
    associateTextNodeWithMarkupForLayer(tagTextNode, tagMarkup.getDbId(), layerName);
    update();
  }

  public void disassociateTextNodeFromMarkupForLayer(
      TAGTextNode node, TAGMarkup markup, final String layerName) {
    disassociateTextNodeFromMarkupForLayer(node, markup.getDTO(), layerName);
    update();
  }

  public Stream<TAGMarkup> getMarkupStreamForTextNode(TAGTextNode tn) {
    return documentDTO
        .textGraph
        .getMarkupIdStreamForTextNodeId(tn.getDbId())
        .distinct()
        .map(store::getMarkup);
  }

  //  public void joinMarkup(TAGMarkupDTO markup1, TAGMarkup markup2) {
  //    // TODO
  ////    markup1.getTextNodeIds().addAll(markup2.getDTO().getTextNodeIds());
  ////    markup2.getTextNodeStream().forEach(tn -> {
  ////      this.disassociateTextNodeFromMarkupForLayer(tn, markup2, layerName);
  ////      this.associateTextNodeWithMarkupForLayer(tn, markup1, "");
  ////    });
  //    markup1.getAnnotationIds().addAll(markup2.getDTO().getAnnotationIds());
  //  }

  public void addLayer(
      final String layerName, final TAGMarkup rootMarkup, final String parentLayer) {
    documentDTO.textGraph.setLayerRootMarkup(layerName, rootMarkup.getDbId());
    openMarkupStackForLayer.put(layerName, new ArrayDeque<>());
    openMarkupStackForLayer.get(layerName).push(rootMarkup);
    if (parentLayer != null) {
      Deque<TAGMarkup> openMarkupStack = openMarkupStackForLayer.get(parentLayer);
      linkToParentMarkup(rootMarkup, parentLayer, openMarkupStack);
      documentDTO.textGraph.getParentLayerMap().put(layerName, parentLayer);
    }
  }

  private void linkToParentMarkup(
      final TAGMarkup rootMarkup,
      final String parentLayer,
      final Deque<TAGMarkup> openMarkupStack) {
    if (openMarkupStack != null && !openMarkupStack.isEmpty()) {
      Long parentMarkupId = openMarkupStack.peek().getDbId();
      Long childMarkupId = rootMarkup.getDbId();
      if (!Objects.equals(parentMarkupId, childMarkupId)) {
        TextGraph textGraph = documentDTO.textGraph;
        boolean edgeExists =
            textGraph.getOutgoingEdges(parentMarkupId).stream()
                .filter(LayerEdge.class::isInstance)
                .map(LayerEdge.class::cast)
                .filter(e -> e.hasLayer(parentLayer))
                .anyMatch(
                    e -> {
                      Collection<Long> targets = textGraph.getTargets(e);
                      return targets.size() == 1 && targets.contains(childMarkupId);
                    });
        if (!edgeExists) {
          textGraph.addChildMarkup(parentMarkupId, parentLayer, childMarkupId);
        }
      }
    }
  }

  public Set<String> getLayerNames() {
    return documentDTO.textGraph.getLayerNames();
  }

  public void openMarkupInLayer(TAGMarkup markup, String layerName) {
    //    LOG.debug("layer={}", layerName);
    openMarkupStackForLayer.putIfAbsent(layerName, new ArrayDeque<>());
    Deque<TAGMarkup> openMarkupStack = openMarkupStackForLayer.get(layerName);
    linkToParentMarkup(markup, layerName, openMarkupStack);
    openMarkupStack.push(markup);
  }

  public void closeMarkupInLayer(TAGMarkup markup, String layerName) {
    Deque<TAGMarkup> tagMarkups = openMarkupStackForLayer.get(layerName);
    if (!tagMarkups.isEmpty()) {
      TAGMarkup lastOpenedMarkup = tagMarkups.pop();
    }
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkup(final TAGMarkup markup) {
    return getTextNodeStreamForMarkupInLayers(markup, getLayerNames());
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkupInLayer(
      final TAGMarkup markup, final String layer) {
    Set<String> layers = new HashSet<>();
    layers.add(layer);
    return getTextNodeStreamForMarkupInLayers(markup, layers);
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkupInLayers(
      final TAGMarkup markup, Set<String> layers) {
    return documentDTO
        .textGraph
        .getTextNodeIdStreamForMarkupIdInLayers(markup.getDbId(), layers)
        .map(store::getTextNode);
  }

  public void linkParentlessLayerRootsToDocument() {
    documentDTO.textGraph.linkParentlessLayerRootsToDocument();
  }

  /* private methods */

  private Stream<TAGTextNodeDTO> getTagTextNodeStream() {
    return documentDTO.getTextNodeIds().stream() //
        .map(store::getTextNodeDTO);
  }

  private void update() {
    documentDTO.updateModificationDate();
    store.persist(documentDTO);
  }

  private void associateTextNodeWithMarkupForLayer(
      TAGTextNode tagTextNode, TAGMarkupDTO markup, String layerName) {
    associateTextNodeWithMarkupForLayer(tagTextNode, markup.getDbId(), layerName);
  }

  private void associateTextNodeWithMarkupForLayer(
      TAGTextNode tagTextNode, Long markupId, String layerName) {
    //    documentDTO.getTextNodeIdToMarkupIds()
    //        .computeIfAbsent(
    //            tagTextNode.getResourceId(),
    //            f -> new LinkedHashSet<>()).add(markupId);
    documentDTO.textGraph.linkMarkupToTextNodeForLayer(markupId, tagTextNode.getDbId(), layerName);
    update();
  }

  private void disassociateTextNodeFromMarkupForLayer(
      TAGTextNode tagTextNode, TAGMarkupDTO markup, String layerName) {
    disassociateTextNodeFromMarkupForLayer(tagTextNode, markup.getDbId(), layerName);
  }

  private void disassociateTextNodeFromMarkupForLayer(
      TAGTextNode tagTextNode, Long markupId, String layerName) {
    documentDTO.textGraph.unlinkMarkupFromTextNodeForLayer(
        markupId, tagTextNode.getDbId(), layerName);
    update();
  }

  private TAGDocument addMarkupId(Long id) {
    documentDTO.getMarkupIds().add(id);
    update();
    return this;
  }

  public void setNamespaces(final Map<String, String> namespaces) {
    documentDTO.setNamespaces(namespaces);
  }

  public Map<String, String> getNamespaces() {
    return documentDTO.getNamespaces();
  }

  public Optional<URL> getSchemaLocation() throws MalformedURLException {
    final String schemaLocationString = documentDTO.getSchemaLocation();
    if (schemaLocationString == null) {
      return Optional.empty();
    } else {
      return Optional.of(new URL(schemaLocationString));
    }
  }

  public void setSchemaLocation(URI schemaLocation) {
    documentDTO.setSchemaLocation(schemaLocation.toString());
  }

  // If there is no markup unique to the default layer, remove the default layer
  public void removeDefaultLayerIfUnused() {
    Long defaultRootMarkupId = documentDTO.textGraph.getLayerRootMap().get(DEFAULT_LAYER);
    if (defaultRootMarkupId != null) {
      boolean defaultLayerIsUnused =
          documentDTO.textGraph.getOutgoingEdges(defaultRootMarkupId).stream()
              .noneMatch(this::isInDefaultLayer);
      if (defaultLayerIsUnused) {
        documentDTO.textGraph.getLayerRootMap().remove(DEFAULT_LAYER);
        TAGMarkup markup = store.getMarkup(defaultRootMarkupId);
        markup.getLayers().remove(DEFAULT_LAYER);
        store.persist(markup.getDTO());
        update();
      }
    }
  }

  private boolean isInDefaultLayer(final Edge edge) {
    return edge instanceof LayerEdge
        ? ((LayerEdge) edge).hasLayer(DEFAULT_LAYER)
        : edge instanceof MarkupToTextHyperEdge;
  }

  public Stream<Long> getChildMarkupIdStream(Long markupId, String layer) {
    return documentDTO.textGraph.getOutgoingEdges(markupId).stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .filter(le -> le.hasLayer(layer))
        .filter(le -> le.hasType(EdgeType.hasMarkup))
        .map(documentDTO.textGraph::getTargets)
        .flatMap(Collection::stream);
  }
}
