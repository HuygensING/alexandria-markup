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

import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class TAGDocument {

  private final TAGStore store;
  private final TAGDocumentDTO documentDTO;

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

  public Stream<TAGTextNode> getTextNodeStream() {
    return getTagTextNodeStream()
        .map(tn -> new TAGTextNode(store, tn));
  }

  public Stream<TAGMarkup> getMarkupStream() {
    return documentDTO.getMarkupIds().stream()
        .map(store::getMarkupDTO)//
        .map(m -> new TAGMarkup(store, m));
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

  public void setOnlyTextNode(TAGTextNode annotationText) {
    documentDTO.getTextNodeIds().add(annotationText.getDbId());
    update();
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

  public void associateTextNodeWithMarkup(TAGTextNode tagTextNode, TAGMarkup tagMarkup) {
    associateTextNodeWithMarkup(tagTextNode, tagMarkup.getDbId());
    update();
  }

  public void disAssociateTextNodeWithMarkup(TAGTextNode node, TAGMarkup markup) {
    documentDTO.getTextNodeIdToMarkupIds()
        .get(node.getDbId())
        .remove(markup.getDbId());
    update();
  }

  public TAGDocument setFirstAndLastTextNode(TAGTextNode firstTextNode, TAGTextNode lastTextNode) {
    documentDTO.getTextNodeIds().clear();
    addTextNode(firstTextNode);
    if (!firstTextNode.getDbId().equals(lastTextNode.getDbId())) {
      TAGTextNode next = firstTextNode.getNextTextNodes().get(0);// TODO: handle divergence
      while (!next.getDbId().equals(lastTextNode.getDbId())) {
        addTextNode(next);
        next = next.getNextTextNodes().get(0);// TODO: handle divergence
      }
      addTextNode(next);
    }
    update();
    return this;
  }

  public TAGDocument addTextNode(TAGTextNode textNode) {
    List<Long> textNodeIds = documentDTO.getTextNodeIds();
    textNodeIds.add(textNode.getDbId());
    if (textNodeIds.size() == 1) {
      documentDTO.setFirstTextNodeId(textNode.getDbId());

    } else {
      Long textNodeId = textNodeIds.get(textNodeIds.size() - 2);
      TAGTextNodeDTO prevTextNode = store.getTextNodeDTO(textNodeId);
      TAGTextNode previousTextNode = new TAGTextNode(store, prevTextNode);
//      textNode.addPreviousTextNode(previousTextNode);
    }
    update();
    return this;
  }

  public Stream<TAGMarkup> getMarkupStreamForTextNode(TAGTextNode tn) {
    return documentDTO.getMarkupIdsForTextNodeIds(tn.getDbId()).stream()//
        .map(store::getMarkupDTO)//
        .map(m -> new TAGMarkup(store, m));
  }

  public void joinMarkup(TAGMarkupDTO markup1, TAGMarkup markup2) {
    markup1.getTextNodeIds().addAll(markup2.getDTO().getTextNodeIds());
    markup2.getTextNodeStream().forEach(tn -> {
      this.disAssociateTextNodeWithMarkup(tn, markup2);
      this.associateTextNodeWithMarkup(tn, markup1);
    });
    markup1.getAnnotationIds().addAll(markup2.getDTO().getAnnotationIds());
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

  private void associateTextNodeWithMarkup(TAGTextNode tagTextNode, TAGMarkupDTO markup) {
    associateTextNodeWithMarkup(tagTextNode, markup.getDbId());
  }

  private void associateTextNodeWithMarkup(TAGTextNode tagTextNode, Long id) {
    documentDTO.getTextNodeIdToMarkupIds()
        .computeIfAbsent(
            tagTextNode.getDbId(),
            f -> new LinkedHashSet<>()).add(id);
    update();
  }

  private TAGDocument addMarkupId(Long id) {
    documentDTO.getMarkupIds().add(id);
    update();
    return this;
  }

  public void addLayer(final String layerName, final TAGMarkup rootMarkup) {
    documentDTO.getTextGraph().setLayerRootMarkup(layerName, rootMarkup.getDbId());
  }

  public Set<String> getLayerNames() {
    return documentDTO.getTextGraph().getLayerNames();
  }
}
