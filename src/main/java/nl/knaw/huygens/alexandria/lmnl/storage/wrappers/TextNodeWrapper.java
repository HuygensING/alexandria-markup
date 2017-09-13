package nl.knaw.huygens.alexandria.lmnl.storage.wrappers;

import nl.knaw.huygens.alexandria.lmnl.storage.TAGStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;

public class TextNodeWrapper {
  private TAGStore store;
  private TAGTextNode textNode;

  public TextNodeWrapper(TAGStore store, TAGTextNode textNode) {
    this.store = store;
    this.textNode = textNode;
    update();
  }

  public Long getId() {
    return textNode.getId();
  }

  private TAGTextNode getTextNode() {
    return textNode;
  }

  public TextNodeWrapper setPreviousTextNode(TextNodeWrapper textNodeWrapper) {
    TextNodeWrapper previousTextNode = new TextNodeWrapper(store, textNodeWrapper.getTextNode());
    textNode.setPrevTextNodeId(previousTextNode.getId());
    if (previousTextNode != null && previousTextNode.getNextTextNode() == null) {
      previousTextNode.setNextTextNode(this);
    }
    update();
    return this;
  }

  private void setNextTextNode(TextNodeWrapper textNodeWrapper) {
    textNode.setNextTextNodeId(textNodeWrapper.getId());
    update();
  }

  public TextNodeWrapper getNextTextNode() {
    Long nextTextNodeId = textNode.getNextTextNodeId();
    if (nextTextNodeId == null) {
      return null;
    }
    TAGTextNode nextTextNode = store.getTextNode(nextTextNodeId);
    return new TextNodeWrapper(store, nextTextNode);
  }

  private void update() {
    store.persist(textNode);
  }
}
