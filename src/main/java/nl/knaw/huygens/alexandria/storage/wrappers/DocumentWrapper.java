package nl.knaw.huygens.alexandria.storage.wrappers;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.compare.TAGToken;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public class DocumentWrapper {

  private final TAGStore store;
  private final TAGDocument document;
  private Stream<TAGToken> TAGTokenStream;

  public DocumentWrapper(TAGStore store, TAGDocument document) {
    this.store = store;
    this.document = document;
  }

  public TAGDocument getDocument() {
    return document;
  }

  public long getId() {
    return document.getId();
  }

  public Stream<TextNodeWrapper> getTextNodeStream() {
    return getTagTextNodeStream()
        .map(tn -> new TextNodeWrapper(store, tn));
  }

  public Stream<MarkupWrapper> getMarkupStream() {
    return document.getMarkupIds().stream()
        .map(store::getMarkup)//
        .map(m -> new MarkupWrapper(store, m));
  }

  public boolean hasTextNodes() {
    return !document.getTextNodeIds().isEmpty();
  }

  public boolean containsAtLeastHalfOfAllTextNodes(MarkupWrapper markupWrapper) {
    return document.containsAtLeastHalfOfAllTextNodes(markupWrapper.getMarkup());
  }

  public void setOnlyTextNode(TextNodeWrapper annotationText) {
    document.getTextNodeIds().add(annotationText.getId());
    update();
  }

  public DocumentWrapper addMarkup(TAGMarkup markup) {
    Long id = markup.getId();
    return addMarkupId(id);
  }

  public DocumentWrapper addMarkup(MarkupWrapper markup) {
    Long id = markup.getId();
    return addMarkupId(id);
  }

  private DocumentWrapper addMarkupId(Long id) {
    document.getMarkupIds().add(id);
    update();
    return this;
  }

  public Iterator<TAGTextNode> getTextNodeIterator() {
    return getTagTextNodeStream()//
        .iterator();
  }

  public void associateTextNodeWithMarkup(TextNodeWrapper textNodeWrapper, MarkupWrapper markupWrapper) {
    associateTextNodeWithMarkup(textNodeWrapper, markupWrapper.getId());
    update();
  }

  private void associateTextNodeWithMarkup(TextNodeWrapper textNodeWrapper, TAGMarkup markup) {
    associateTextNodeWithMarkup(textNodeWrapper, markup.getId());
  }

  private void associateTextNodeWithMarkup(TextNodeWrapper textNodeWrapper, Long id) {
    document.getTextNodeIdToMarkupIds()
        .computeIfAbsent(
            textNodeWrapper.getId(),
            f -> new LinkedHashSet<>()).add(id);
    update();
  }

  private void disAssociateTextNodeWithMarkup(TextNodeWrapper node, MarkupWrapper markup) {
    document.getTextNodeIdToMarkupIds()
        .computeIfAbsent(
            node.getId(),
            f -> new LinkedHashSet<>()).remove(markup.getId());
    update();
  }

  public DocumentWrapper setFirstAndLastTextNode(TextNodeWrapper firstTextNode, TextNodeWrapper lastTextNode) {
    document.getTextNodeIds().clear();
    addTextNode(firstTextNode);
    if (!firstTextNode.getId().equals(lastTextNode.getId())) {
      TextNodeWrapper next = firstTextNode.getNextTextNode();
      while (!next.getId().equals(lastTextNode.getId())) {
        addTextNode(next);
        next = next.getNextTextNode();
      }
      addTextNode(next);
    }
    update();
    return this;
  }

  public TextNodeWrapper insertTextNodeBefore(TAGTextNode newTextNode, TAGTextNode nextTextNode) {
    TextNodeWrapper newTextNodeWrapper = new TextNodeWrapper(store, newTextNode);
    List<Long> textNodeIds = document.getTextNodeIds();
    Long newId = newTextNode.getId();
    Long nextId = nextTextNode.getId();
    int offset = textNodeIds.indexOf(nextId);
    textNodeIds.add(offset, newId);
    newTextNode.setNextTextNodeId(nextId);
    ofNullable(nextTextNode.getPrevTextNodeId())
        .ifPresent(newTextNode::setPrevTextNodeId);
    nextTextNode.setPrevTextNodeId(newId);
    // TODO: add implied markup?
    update();
    return newTextNodeWrapper;
  }

  public DocumentWrapper addTextNode(TextNodeWrapper textNode) {
    List<Long> textNodeIds = document.getTextNodeIds();
    textNodeIds.add(textNode.getId());
    if (textNodeIds.size() > 1) {
      Long prevTextNodeId = textNodeIds.get(textNodeIds.size() - 2);
      TAGTextNode prevTextNode = store.getTextNode(prevTextNodeId);
      TextNodeWrapper previousTextNode = new TextNodeWrapper(store, prevTextNode);
      textNode.setPreviousTextNode(previousTextNode);
    }
    update();
    return this;
  }

  public Stream<MarkupWrapper> getMarkupStreamForTextNode(TextNodeWrapper tn) {
    return document.getMarkupIdsForTextNodeIds(tn.getId()).stream()//
        .map(store::getMarkup)//
        .map(m -> new MarkupWrapper(store, m));
  }

  public void joinMarkup(TAGMarkup markup1, MarkupWrapper markup2) {
    markup1.getTextNodeIds().addAll(markup2.getMarkup().getTextNodeIds());
    markup2.getTextNodeStream().forEach(tn -> {
      this.disAssociateTextNodeWithMarkup(tn, markup2);
      this.associateTextNodeWithMarkup(tn, markup1);
    });
    markup1.getAnnotationIds().addAll(markup2.getMarkup().getAnnotationIds());
  }

  /* private methods */

  private Stream<TAGTextNode> getTagTextNodeStream() {
    return document.getTextNodeIds().stream()//
        .map(store::getTextNode);
  }

  private void update() {
    store.persist(document);
  }

  public TAGStore getStore() {
    return store;
  }

}
