package nl.knaw.huygens.alexandria.storage.wrappers;

import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.stream.Stream;

public class AnnotationWrapper {
  private TAGStore store;
  private TAGAnnotation annotation;

  public AnnotationWrapper(TAGStore store, TAGAnnotation annotation) {
    this.store = store;
    this.annotation = annotation;
    update();
  }

  public DocumentWrapper getDocument() {
    TAGDocument document = store.getDocument(annotation.getDocumentId());
    return new DocumentWrapper(store, document);
  }

  public Long getId() {
    return annotation.getId();
  }

  public String getTag() {
    return annotation.getTag();
  }

  public AnnotationWrapper addAnnotation(AnnotationWrapper annotationWrapper) {
    annotation.getAnnotationIds().add(annotationWrapper.getId());
    update();
    return this;
  }

  public Stream<AnnotationWrapper> getAnnotationStream() {
    return annotation.getAnnotationIds().stream()//
        .map(store::getAnnotationWrapper);
  }

  public TAGAnnotation getAnnotation() {
    return annotation;
  }

  private void update() {
    store.persist(annotation);
  }

}
