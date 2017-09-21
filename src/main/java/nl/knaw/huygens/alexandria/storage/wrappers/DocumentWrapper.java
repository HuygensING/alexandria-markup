package nl.knaw.huygens.alexandria.storage.wrappers;

import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

public class DocumentWrapper {

  private TAGStore store;
  private TAGDocument document;

  public DocumentWrapper(TAGStore store, TAGDocument document) {
    this.store = store;
    this.document = document;
//    update();
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

  public void associateTextNodeWithMarkup(TextNodeWrapper textNodeWrapper, TAGMarkup markup) {
    associateTextNodeWithMarkup(textNodeWrapper, markup.getId());
  }

  private void associateTextNodeWithMarkup(TextNodeWrapper textNodeWrapper, Long id) {
    document.getTextNodeIdToMarkupIds()
        .computeIfAbsent(
            textNodeWrapper.getId(),
            f -> new LinkedHashSet<>()).add(id);
    update();
  }

  public void disAssociateTextNodeWithMarkup(TextNodeWrapper node, MarkupWrapper markup) {
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

  public DocumentWrapper addTextNode(TextNodeWrapper textNode) {
    List<Long> textNodeIds = document.getTextNodeIds();
    textNodeIds.add(textNode.getId());
    if (textNodeIds.size() > 1) {
      Long textNodeId = textNodeIds.get(textNodeIds.size() - 2);
      TAGTextNode prevTextNode = store.getTextNode(textNodeId);
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


}
