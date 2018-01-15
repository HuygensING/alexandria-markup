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
