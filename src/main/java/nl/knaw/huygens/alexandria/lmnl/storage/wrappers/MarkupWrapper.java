package nl.knaw.huygens.alexandria.lmnl.storage.wrappers;

import nl.knaw.huygens.alexandria.lmnl.storage.TAGStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGMarkup;

import java.util.List;
import java.util.stream.Stream;

public class MarkupWrapper {
  private TAGStore store;
  private TAGMarkup markup;

  public MarkupWrapper(TAGStore store, TAGMarkup markup) {
    this.store = store;
    this.markup = markup;
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
        .associateTextWithRange(textNodeWrapper, this);
    return this;
  }

  public MarkupWrapper setOnlyTextNode(TextNodeWrapper t1) {
    markup.getTextNodeIds().add(t1.getId());
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
    return this;
  }

  public MarkupWrapper addAnnotation(AnnotationWrapper annotation) {
    markup.getAnnotationIds().add(annotation.getId());
    return this;
  }


  public Stream<AnnotationWrapper> getAnnotations() {
    return markup.getAnnotationIds().stream()//
        .map(store::getAnnotation)//
        .map(annotation-> new AnnotationWrapper(store, annotation));
  }
}
