package nl.knaw.huygens.alexandria.storage.wrappers;

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

import nl.knaw.huygens.alexandria.storage.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNodeDTO;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

public class TAGDocument {

  private final TAGStore store;
  private final TAGDocumentDTO document;

  public TAGDocument(TAGStore store, TAGDocumentDTO document) {
    this.store = store;
    this.document = document;
  }

  public TAGDocumentDTO getDocument() {
    return document;
  }

  public long getDbId() {
    return document.getDbId();
  }

  public Stream<TAGTextNode> getTextNodeStream() {
    return getTagTextNodeStream()
        .map(tn -> new TAGTextNode(store, tn));
  }

  public Stream<TAGMarkup> getMarkupStream() {
    return document.getMarkupIds().stream()
        .map(store::getMarkup)//
        .map(m -> new TAGMarkup(store, m));
  }

  public TAGTextNode getFirstTextNode() {
    return store.getTextNodeWrapper(document.getFirstTextNodeId());
  }

  public boolean hasTextNodes() {
    return document.getFirstTextNodeId() != null;
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TAGMarkup TAGMarkup) {
    return document.containsAtLeastHalfOfAllTextNodes(TAGMarkup.getMarkup());
  }

  public void setOnlyTextNode(TAGTextNode annotationText) {
    document.getTextNodeIds().add(annotationText.getDbId());
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

  public void associateTextNodeWithMarkup(TAGTextNode TAGTextNode, TAGMarkup TAGMarkup) {
    associateTextNodeWithMarkup(TAGTextNode, TAGMarkup.getDbId());
    update();
  }

  public void disAssociateTextNodeWithMarkup(TAGTextNode node, TAGMarkup markup) {
    document.getTextNodeIdToMarkupIds()
        .get(node.getDbId())
        .remove(markup.getDbId());
    update();
  }

  public TAGDocument setFirstAndLastTextNode(TAGTextNode firstTextNode, TAGTextNode lastTextNode) {
    document.getTextNodeIds().clear();
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
    List<Long> textNodeIds = document.getTextNodeIds();
    textNodeIds.add(textNode.getDbId());
    if (textNodeIds.size() == 1) {
      document.setFirstTextNodeId(textNode.getDbId());

    } else {
      Long textNodeId = textNodeIds.get(textNodeIds.size() - 2);
      TAGTextNodeDTO prevTextNode = store.getTextNode(textNodeId);
      TAGTextNode previousTextNode = new TAGTextNode(store, prevTextNode);
//      textNode.addPreviousTextNode(previousTextNode);
    }
    update();
    return this;
  }

  public Stream<TAGMarkup> getMarkupStreamForTextNode(TAGTextNode tn) {
    return document.getMarkupIdsForTextNodeIds(tn.getDbId()).stream()//
        .map(store::getMarkup)//
        .map(m -> new TAGMarkup(store, m));
  }

  public void joinMarkup(TAGMarkupDTO markup1, TAGMarkup markup2) {
    markup1.getTextNodeIds().addAll(markup2.getMarkup().getTextNodeIds());
    markup2.getTextNodeStream().forEach(tn -> {
      this.disAssociateTextNodeWithMarkup(tn, markup2);
      this.associateTextNodeWithMarkup(tn, markup1);
    });
    markup1.getAnnotationIds().addAll(markup2.getMarkup().getAnnotationIds());
  }

  /* private methods */

  private Stream<TAGTextNodeDTO> getTagTextNodeStream() {
    return document.getTextNodeIds().stream()//
        .map(store::getTextNode);
  }

  private void update() {
    document.updateModificationDate();
    store.persist(document);
  }

  private void associateTextNodeWithMarkup(TAGTextNode TAGTextNode, TAGMarkupDTO markup) {
    associateTextNodeWithMarkup(TAGTextNode, markup.getDbId());
  }

  private void associateTextNodeWithMarkup(TAGTextNode TAGTextNode, Long id) {
    document.getTextNodeIdToMarkupIds()
        .computeIfAbsent(
            TAGTextNode.getDbId(),
            f -> new LinkedHashSet<>()).add(id);
    update();
  }

  private TAGDocument addMarkupId(Long id) {
    document.getMarkupIds().add(id);
    update();
    return this;
  }

}
