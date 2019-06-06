package nl.knaw.huygens.alexandria.storage;

/*
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
import nl.knaw.huc.di.tag.model.graph.edges.Edge;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huc.di.tag.model.graph.edges.MarkupToTextHyperEdge;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER;

public class TAGDocumentDAO {
  Logger LOG = LoggerFactory.getLogger(TAGDocumentDAO.class);
  private final TAGStore store;
  private final TAGDocument documentDTO;
  private final Map<String, Deque<TAGMarkupDAO>> openMarkupStackForLayer = new HashMap<>();

  public TAGDocumentDAO(TAGStore store, TAGDocument documentDTO) {
    this.store = store;
    this.documentDTO = documentDTO;
  }

  public TAGDocument getDTO() {
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

  public TAGDocumentDAO addTextNode(TAGTextNodeDAO textNode, List<TAGMarkupDAO> lastOpenedMarkup) {
    List<Long> textNodeIds = documentDTO.getTextNodeIds();
    Long textNodeDbId = textNode.getDbId();
    textNodeIds.add(textNodeDbId);
    if (textNodeIds.size() == 1) {
      documentDTO.setFirstTextNodeId(textNodeDbId);
    }
    if (lastOpenedMarkup != null) {
      lastOpenedMarkup.forEach(m -> {
            Long dbId = m.getDbId();
            Set<String> relevantLayers = new HashSet<>(m.getLayers());
            if (relevantLayers.size() > 1) {
              relevantLayers.remove(DEFAULT_LAYER);
            }
            relevantLayers.forEach(l -> documentDTO.textGraph
                .linkMarkupToTextNodeForLayer(dbId, textNodeDbId, l)
            );
          }
      );
    }
    update();
    return this;
  }

  public Stream<TAGTextNodeDAO> getTextNodeStream() {
    return documentDTO.getTextNodeIds().stream()
        .map(store::getTextNode);
  }

  public Stream<TAGMarkupDAO> getMarkupStream() {
    return documentDTO.getMarkupIds().stream()
        .map(store::getMarkup);
  }

  public TAGTextNodeDAO getFirstTextNode() {
    return store.getTextNode(documentDTO.getFirstTextNodeId());
  }

  public TAGTextNodeDAO getLastTextNode() {
//    TextGraph textGraph = documentDTO.textGraph;
//    Long rootMarkup = textGraph.getLayerRootMap().get(TAGML.DEFAULT_LAYER);
//    List<Edge> outgoingEdges = (List<Edge>) textGraph.getOutgoingEdges(rootMarkup);
//    final Edge last = outgoingEdges.get(outgoingEdges.size() - 1);
    // TODO: make this more efficient
    List<TAGTextNodeDAO> textNodes = getTextNodeStream().collect(toList());
    return textNodes.get(textNodes.size() - 1);
  }

  public boolean hasTextNodes() {
    return documentDTO.getFirstTextNodeId() != null;
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TAGMarkupDAO tagMarkupDAO) {
    return documentDTO.containsAtLeastHalfOfAllTextNodes(tagMarkupDAO.getDTO());
  }

  public TAGDocumentDAO addMarkup(TAGMarkup markup) {
    Long id = markup.getDbId();
    return addMarkupId(id);
  }

  public TAGDocumentDAO addMarkup(TAGMarkupDAO markup) {
    Long id = markup.getDbId();
    return addMarkupId(id);
  }

  public Iterator<TAGTextNode> getTextNodeIterator() {
    return getTagTextNodeStream()//
        .iterator();
  }

  public void associateTextNodeWithMarkupForLayer(TAGTextNodeDAO tagTextNodeDAO, TAGMarkupDAO tagMarkupDAO) {
    associateTextNodeWithMarkupForLayer(tagTextNodeDAO, tagMarkupDAO, DEFAULT_LAYER);
  }

  public void associateTextNodeWithMarkupForLayer(TAGTextNodeDAO tagTextNodeDAO, TAGMarkupDAO tagMarkupDAO, String layerName) {
    associateTextNodeWithMarkupForLayer(tagTextNodeDAO, tagMarkupDAO.getDbId(), layerName);
    update();
  }

  public void disassociateTextNodeFromMarkupForLayer(TAGTextNodeDAO node, TAGMarkupDAO markup, final String layerName) {
    disassociateTextNodeFromMarkupForLayer(node, markup.getDTO(), layerName);
    update();
  }

  public Stream<TAGMarkupDAO> getMarkupStreamForTextNode(TAGTextNodeDAO tn) {
    return documentDTO.textGraph
        .getMarkupIdStreamForTextNodeId(tn.getDbId())
        .distinct()
        .map(store::getMarkup)
        ;
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

  public void addLayer(final String layerName, final TAGMarkupDAO rootMarkup, final String parentLayer) {
    documentDTO.textGraph.setLayerRootMarkup(layerName, rootMarkup.getDbId());
    openMarkupStackForLayer.put(layerName, new ArrayDeque<>());
    openMarkupStackForLayer.get(layerName).push(rootMarkup);
    if (parentLayer != null) {
      Deque<TAGMarkupDAO> openMarkupStack = openMarkupStackForLayer.get(parentLayer);
      linkToParentMarkup(rootMarkup, parentLayer, openMarkupStack);
      documentDTO.textGraph.getParentLayerMap().put(layerName, parentLayer);
    }
  }

  private void linkToParentMarkup(final TAGMarkupDAO rootMarkup, final String parentLayer, final Deque<TAGMarkupDAO> openMarkupStack) {
    if (openMarkupStack != null && !openMarkupStack.isEmpty()) {
      Long parentMarkupId = openMarkupStack.peek().getDbId();
      Long childMarkupId = rootMarkup.getDbId();
      if (!Objects.equals(parentMarkupId, childMarkupId)) {
        TextGraph textGraph = documentDTO.textGraph;
        boolean edgeExists = textGraph.getOutgoingEdges(parentMarkupId)
            .stream()
            .filter(LayerEdge.class::isInstance)
            .map(LayerEdge.class::cast)
            .filter(e -> e.hasLayer(parentLayer))
            .anyMatch(e -> {
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

  public void openMarkupInLayer(TAGMarkupDAO markup, String layerName) {
//    LOG.debug("layer={}", layerName);
    openMarkupStackForLayer.putIfAbsent(layerName, new ArrayDeque<>());
    Deque<TAGMarkupDAO> openMarkupStack = openMarkupStackForLayer.get(layerName);
    linkToParentMarkup(markup, layerName, openMarkupStack);
    openMarkupStack.push(markup);
  }

  public void closeMarkupInLayer(TAGMarkupDAO markup, String layerName) {
    Deque<TAGMarkupDAO> tagMarkupDAOS = openMarkupStackForLayer.get(layerName);
    if (!tagMarkupDAOS.isEmpty()) {
      TAGMarkupDAO lastOpenedMarkup = tagMarkupDAOS.pop();
    }
  }

  public Stream<TAGTextNodeDAO> getTextNodeStreamForMarkup(final TAGMarkupDAO markup) {
    return getTextNodeStreamForMarkupInLayers(markup, getLayerNames());
  }

  public Stream<TAGTextNodeDAO> getTextNodeStreamForMarkupInLayer(final TAGMarkupDAO markup, final String layer) {
    Set<String> layers = new HashSet<>();
    layers.add(layer);
    return getTextNodeStreamForMarkupInLayers(markup, layers);
  }

  public Stream<TAGTextNodeDAO> getTextNodeStreamForMarkupInLayers(final TAGMarkupDAO markup, Set<String> layers) {
    return documentDTO.textGraph
        .getTextNodeIdStreamForMarkupIdInLayers(markup.getDbId(), layers)
        .map(store::getTextNode);
  }

  public void linkParentlessLayerRootsToDocument() {
    documentDTO.textGraph.linkParentlessLayerRootsToDocument();
  }

  /* private methods */

  private Stream<TAGTextNode> getTagTextNodeStream() {
    return documentDTO.getTextNodeIds().stream()//
        .map(store::getTextNodeDTO);
  }

  private void update() {
    documentDTO.updateModificationDate();
    store.persist(documentDTO);
  }

  private void associateTextNodeWithMarkupForLayer(TAGTextNodeDAO tagTextNodeDAO, TAGMarkup markup, String layerName) {
    associateTextNodeWithMarkupForLayer(tagTextNodeDAO, markup.getDbId(), layerName);
  }

  private void associateTextNodeWithMarkupForLayer(TAGTextNodeDAO tagTextNodeDAO, Long markupId, String layerName) {
//    documentDTO.getTextNodeIdToMarkupIds()
//        .computeIfAbsent(
//            tagTextNode.getDbId(),
//            f -> new LinkedHashSet<>()).add(markupId);
    documentDTO.textGraph.linkMarkupToTextNodeForLayer(markupId, tagTextNodeDAO.getDbId(), layerName);
    update();
  }

  private void disassociateTextNodeFromMarkupForLayer(TAGTextNodeDAO tagTextNodeDAO, TAGMarkup markup, String layerName) {
    disassociateTextNodeFromMarkupForLayer(tagTextNodeDAO, markup.getDbId(), layerName);
  }

  private void disassociateTextNodeFromMarkupForLayer(TAGTextNodeDAO tagTextNodeDAO, Long markupId, String layerName) {
    documentDTO.textGraph.unlinkMarkupFromTextNodeForLayer(markupId, tagTextNodeDAO.getDbId(), layerName);
    update();
  }

  private TAGDocumentDAO addMarkupId(Long id) {
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

  // If there is no markup unique to the default layer, remove the default layer
  public void removeDefaultLayerIfUnused() {
    Long defaultRootMarkupId = documentDTO.textGraph.getLayerRootMap().get(DEFAULT_LAYER);
    if (defaultRootMarkupId != null) {
      boolean defaultLayerIsUnused = documentDTO.textGraph
          .getOutgoingEdges(defaultRootMarkupId)
          .stream()
          .noneMatch(this::isInDefaultLayer);
      if (defaultLayerIsUnused) {
        documentDTO.textGraph.getLayerRootMap().remove(DEFAULT_LAYER);
        TAGMarkupDAO markup = store.getMarkup(defaultRootMarkupId);
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
}
