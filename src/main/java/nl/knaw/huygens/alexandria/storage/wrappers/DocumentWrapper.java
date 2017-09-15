package nl.knaw.huygens.alexandria.storage.wrappers;

import nl.knaw.huygens.alexandria.storage.TAGDocument;
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

  public DocumentWrapper addMarkup(MarkupWrapper markup) {
    document.getMarkupIds().add(markup.getId());
    update();
    return this;
  }

  public Iterator<TAGTextNode> getTextNodeIterator() {
    return getTagTextNodeStream()//
        .iterator();
  }

  public void associateTextWithRange(TextNodeWrapper textNodeWrapper, MarkupWrapper markupWrapper) {
    document.getTextNodeIdToMarkupIds()
        .computeIfAbsent(
            textNodeWrapper.getId(),
            f -> new LinkedHashSet<>()).add(markupWrapper.getId());
  }

  public DocumentWrapper setFirstAndLastTextNode(TextNodeWrapper firstTextNode, TextNodeWrapper lastTextNode) {
    document.getTextNodeIds().clear();
    addTextNode(firstTextNode);
    if (firstTextNode != lastTextNode) {
      TextNodeWrapper next = firstTextNode.getNextTextNode();
      while (next != lastTextNode) {
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


  /* private methods */

  private Stream<TAGTextNode> getTagTextNodeStream() {
    return document.getTextNodeIds().stream()//
        .map(store::getTextNode);
  }

  private void update() {
    store.persist(document);
  }

  public Stream<MarkupWrapper> getMarkupStreamForTextNode(TextNodeWrapper tn) {
    return document.getMarkupIdsForTextNodeIds(tn.getId()).stream()//
        .map(store::getMarkup)//
        .map(m -> new MarkupWrapper(store, m));
  }

}