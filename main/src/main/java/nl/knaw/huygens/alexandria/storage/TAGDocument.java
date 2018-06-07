package nl.knaw.huygens.alexandria.storage;

/*
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

import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;

import java.util.*;
import java.util.stream.Stream;

public class TAGDocument {
  private final TAGStore store;
  private final TAGDocumentDTO documentDTO;
  private Map<String, Deque<TAGMarkup>> openMarkupStackForLayer = new HashMap<>();

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

  public TAGDocument addTextNode(TAGTextNode textNode) {
    List<Long> textNodeIds = documentDTO.getTextNodeIds();
    Long textNodeDbId = textNode.getDbId();
    textNodeIds.add(textNodeDbId);
    if (textNodeIds.size() == 1) {
      documentDTO.setFirstTextNodeId(textNodeDbId);
    }
    documentDTO.textGraph.appendTextNode(textNodeDbId);
    openMarkupStackForLayer.forEach((layerName, stack) -> {
          if (!stack.isEmpty()) {
            Long markupId = stack.peek().getDbId();
            documentDTO.textGraph
                .linkMarkupToTextNodeForLayer(markupId, textNodeDbId, layerName);
          }
        }
    );
    update();
    return this;
  }

  public Stream<TAGTextNode> getTextNodeStream() {
    return documentDTO.getTextNodeIds().stream()
        .map(store::getTextNode);
  }

  public Stream<TAGMarkup> getMarkupStream() {
    return documentDTO.getMarkupIds().stream()
        .map(store::getMarkup);
  }

  public TAGTextNode getFirstTextNode() {
    return store.getTextNode(documentDTO.getFirstTextNodeId());
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
    return getTagTextNodeStream()//
        .iterator();
  }

  public void associateTextNodeWithMarkupForLayer(TAGTextNode tagTextNode, TAGMarkup tagMarkup) {
    associateTextNodeWithMarkupForLayer(tagTextNode, tagMarkup, TAGML.DEFAULT_LAYER);
  }

  public void associateTextNodeWithMarkupForLayer(TAGTextNode tagTextNode, TAGMarkup tagMarkup, String layerName) {
    associateTextNodeWithMarkupForLayer(tagTextNode, tagMarkup.getDbId(), layerName);
    update();
  }

  public void disassociateTextNodeFromMarkupForLayer(TAGTextNode node, TAGMarkup markup, final String layerName) {
    disassociateTextNodeFromMarkupForLayer(node, markup.getDTO(), layerName);
    update();
  }

  public Stream<TAGMarkup> getMarkupStreamForTextNode(TAGTextNode tn) {
    return documentDTO.textGraph
        .getMarkupIdStreamForTextNodeId(tn.getDbId())
        .distinct()
        .map(store::getMarkup);
  }

  public void joinMarkup(TAGMarkupDTO markup1, TAGMarkup markup2) {
    // TODO
//    markup1.getTextNodeIds().addAll(markup2.getDTO().getTextNodeIds());
//    markup2.getTextNodeStream().forEach(tn -> {
//      this.disassociateTextNodeFromMarkupForLayer(tn, markup2, layerName);
//      this.associateTextNodeWithMarkupForLayer(tn, markup1, "");
//    });
    markup1.getAnnotationIds().addAll(markup2.getDTO().getAnnotationIds());
  }

  public void addLayer(final String layerName, final TAGMarkup rootMarkup) {
    documentDTO.textGraph.setLayerRootMarkup(layerName, rootMarkup.getDbId());
    openMarkupStackForLayer.put(layerName, new ArrayDeque<>());
    openMarkupStackForLayer.get(layerName).push(rootMarkup);
  }

  public Set<String> getLayerNames() {
    return documentDTO.textGraph.getLayerNames();
  }

  public void addMarkupToLayer(TAGMarkup markup, String layerName) {
    Deque<TAGMarkup> openMarkupStack = openMarkupStackForLayer.get(layerName);
    Long parentMarkupId = openMarkupStack.peek().getDbId();
    Long childMarkupId = markup.getDbId();
    documentDTO.textGraph.addChildMarkup(parentMarkupId, layerName, childMarkupId);
    openMarkupStack.push(markup);
  }

  public void closeMarkupInLayer(TAGMarkup markup, String layerName) {
    TAGMarkup lastOpenedMarkup = openMarkupStackForLayer.get(layerName).pop();
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkup(final TAGMarkup markup) {
    return getTextNodeStreamForMarkupInLayer(markup, TAGML.DEFAULT_LAYER);
  }

  public Stream<TAGTextNode> getTextNodeStreamForMarkupInLayer(final TAGMarkup markup, String layerName) {
    return documentDTO.textGraph
        .getTextNodeIdStreamForMarkupIdInLayer(markup.getDbId(), layerName)
        .map(store::getTextNode);
  }

  /* private methods */

  private Stream<TAGTextNodeDTO> getTagTextNodeStream() {
    return documentDTO.getTextNodeIds().stream()//
        .map(store::getTextNodeDTO);
  }

  private void update() {
    documentDTO.updateModificationDate();
    store.persist(documentDTO);
  }

  private void associateTextNodeWithMarkupForLayer(TAGTextNode tagTextNode, TAGMarkupDTO markup, String layerName) {
    associateTextNodeWithMarkupForLayer(tagTextNode, markup.getDbId(), layerName);
  }

  private void associateTextNodeWithMarkupForLayer(TAGTextNode tagTextNode, Long markupId, String layerName) {
//    documentDTO.getTextNodeIdToMarkupIds()
//        .computeIfAbsent(
//            tagTextNode.getDbId(),
//            f -> new LinkedHashSet<>()).add(markupId);
    documentDTO.textGraph.linkMarkupToTextNodeForLayer(markupId, tagTextNode.getDbId(), layerName);
    update();
  }

  private void disassociateTextNodeFromMarkupForLayer(TAGTextNode tagTextNode, TAGMarkupDTO markup, String layerName) {
    disassociateTextNodeFromMarkupForLayer(tagTextNode, markup.getDbId(), layerName);
  }

  private void disassociateTextNodeFromMarkupForLayer(TAGTextNode tagTextNode, Long markupId, String layerName) {
    documentDTO.textGraph.unlinkMarkupFromTextNodeForLayer(markupId, tagTextNode.getDbId(), layerName);
    update();
  }

  private TAGDocument addMarkupId(Long id) {
    documentDTO.getMarkupIds().add(id);
    update();
    return this;
  }

  public void linkTextNodes(final TAGTextNode textNode1, final TAGTextNode textNode2) {
    documentDTO.textGraph.linkTextNodes(textNode1.getDbId(), textNode2.getDbId());

  }
}
