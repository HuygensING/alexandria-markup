package nl.knaw.huygens.alexandria.storage.wrappers;

import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarkupWrapper {
  private TAGStore store;
  private TAGMarkup markup;

  public MarkupWrapper(TAGStore store, TAGMarkup markup) {
    this.store = store;
    this.markup = markup;
    update();
  }

  public Long getId() {
    return markup.getId();
  }

  public String getTag() {
    return markup.getTag();
  }

  public MarkupWrapper addTextNode(TextNodeWrapper textNodeWrapper) {
    markup.getTextNodeIds().add(textNodeWrapper.getId());
    Long ownerId = markup.getDocumentId();
    new DocumentWrapper(store, store.getDocument(ownerId))//
        .associateTextNodeWithMarkup(textNodeWrapper, this);
    update();
    return this;
  }

  public MarkupWrapper setOnlyTextNode(TextNodeWrapper t1) {
    markup.getTextNodeIds().add(t1.getId());
    update();
    return this;
  }

  public MarkupWrapper setFirstAndLastTextNode(TextNodeWrapper first, TextNodeWrapper last) {
    markup.getTextNodeIds().clear();
    addTextNode(first);
    if (first != last) {
      TextNodeWrapper next = first.getNextTextNode();
      while (next != last) {
        addTextNode(next);
        next = next.getNextTextNode();
      }
      addTextNode(next);
    }
    update();
    return this;
  }

  public MarkupWrapper addAnnotation(AnnotationWrapper annotation) {
    markup.getAnnotationIds().add(annotation.getId());
    update();
    return this;
  }

  public Stream<AnnotationWrapper> getAnnotationStream() {
    return markup.getAnnotationIds().stream()//
        .map(store::getAnnotation)//
        .map(annotation -> new AnnotationWrapper(store, annotation));
  }

  public Stream<TextNodeWrapper> getTextNodeStream() {
    return markup.getTextNodeIds().stream()//
        .map(store::getTextNode)//
        .map(textNode -> new TextNodeWrapper(store, textNode));
  }

  private void update() {
    store.persist(markup);
  }

  public boolean isAnonymous() {
    return markup.getTextNodeIds().size() == 1//
        && "".equals(getTextNodeStream().findFirst().map(TextNodeWrapper::getText));
  }

  public TAGMarkup getMarkup() {
    return markup;
  }

  public boolean isContinuous() {
    boolean isContinuous = true;
    List<TextNodeWrapper> textNodes = getTextNodeStream().collect(Collectors.toList());
    TextNodeWrapper textNode = textNodes.get(0);
    TextNodeWrapper expectedNext = textNode.getNextTextNode();
    for (int i = 1; i < textNodes.size(); i++) {
      textNode = textNodes.get(i);
      if (!textNode.equals(expectedNext)) {
        isContinuous = false;
        break;
      }
      expectedNext = textNode.getNextTextNode();
    }
    return isContinuous;

  }

  public String getExtendedTag() {
    return markup.getExtendedTag();
  }
}
