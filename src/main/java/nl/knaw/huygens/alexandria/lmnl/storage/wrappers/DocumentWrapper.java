package nl.knaw.huygens.alexandria.lmnl.storage.wrappers;

import nl.knaw.huygens.alexandria.lmnl.storage.TAGStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

public class DocumentWrapper {

  private TAGStore store;
  private TAGDocument document;

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

  public Stream<TextNodeWrapper> getTextNodes() {
    return getTagTextNodeStream()
        .map(tn -> new TextNodeWrapper(store, tn));
  }

  public Stream<MarkupWrapper> getMarkups() {
    return document.getMarkupIds().stream()
        .map(store::getMarkup)//
        .map(m -> new MarkupWrapper(store, m));
  }

  public boolean hasTextNodes() {
    return !document.getTextNodeIds().isEmpty();
  }

  public boolean containsAtLeastHalfOfAllTextNodes(Long aLong) {
    throw new NotImplementedException();
  }

  public void setOnlyTextNode(TextNodeWrapper annotationText) {
    document.getTextNodeIds().add(annotationText.getId());
  }

  public DocumentWrapper addMarkup(MarkupWrapper markup) {
    document.getMarkupIds().add(markup.getId());
    return this;
  }

  public Iterator<TAGTextNode> getTextNodeIterator() {
    return getTagTextNodeStream()//
        .iterator();
  }

  public void associateTextWithRange(TextNodeWrapper textNodeWrapper, MarkupWrapper markupWrapper) {
    throw new NotImplementedException();
//    document.textNodeToMarkup.computeIfAbsent(node, f -> new LinkedHashSet<>()).add(markup);
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
    return this;
  }


  /* private methods */

  private Stream<TAGTextNode> getTagTextNodeStream() {
    return document.getTextNodeIds().stream()//
        .map(store::getTextNode);
  }


}
