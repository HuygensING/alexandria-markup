package nl.knaw.huygens.alexandria.storage.wrappers;

import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.dao.TAGDocument;

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

  public AnnotationWrapper addAnnotation(AnnotationWrapper annotationWrapper) {
    annotation.getAnnotationIds().add(annotationWrapper.getId());
    update();
    return this;
  }

  public TAGAnnotation getAnnotation() {
    return annotation;
  }

  private void update(){
    store.persist(annotation);
  }
}
