package nl.knaw.huygens.alexandria.storage;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.storage.dto.TAGAnnotationDTO;

public class TAGAnnotation {
  private final TAGStore store;
  private final TAGAnnotationDTO annotation;

  public TAGAnnotation(TAGStore store, TAGAnnotationDTO annotation) {
    this.store = store;
    this.annotation = annotation;
    update();
  }

  public Long getDbId() {
    return annotation.getDbId();
  }

  public String getKey() {
    return annotation.getKey();
  }

  public boolean hasKey(String key) {
    return annotation.getKey().equals(key);
  }

  public AnnotationType getType() {
    return annotation.getType();
  }

  public void setType(AnnotationType type) {
    annotation.setType(type);
  }

  public Object getValue() {
    return annotation.getValue();
  }

  public <T> T getTypedValue(Class<T> typeClass) {
    return (T) getValue();
  }

  public TAGAnnotationDTO getDTO() {
    return annotation;
  }

  private void update() {
    store.persist(annotation);
  }

  @Override
  public String toString() {
    // TODO process different annotation types
    return annotation.getKey() + '=' + getValue();
  }

}
