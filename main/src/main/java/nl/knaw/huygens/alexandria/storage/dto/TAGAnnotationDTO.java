package nl.knaw.huygens.alexandria.storage.dto;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import nl.knaw.huygens.alexandria.storage.AnnotationType;
import nl.knaw.huygens.alexandria.storage.DataAccessor;

@Entity(version = 3)
public class TAGAnnotationDTO implements TAGDTO {
  @PrimaryKey(sequence = DataAccessor.SEQUENCE)
  private Long id;

  private String key;
  private Object value;
  private AnnotationType type;

//  @SecondaryKey(relate = ONE_TO_ONE, relatedEntity = TAGDocumentDTO.class)
//  private Long documentId;

//  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGAnnotationDTO.class)
//  private final List<Long> annotationIds = new ArrayList<>();

  private TAGAnnotationDTO() {
  }

  public TAGAnnotationDTO(String key) {
    this.key = key;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Long getDbId() {
    return id;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getKey() {
    return key;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public AnnotationType getType() {
    return type;
  }

  public void setType(AnnotationType type) {
    this.type = type;
  }

//  public void setDocumentId(long documentId) {
//    this.documentId = documentId;
//  }

//  public Long getDocumentId() {
//    return documentId;
//  }

//  public TAGAnnotationDTO addAnnotation(TAGAnnotationDTO annotation) {
//    annotationIds.add(annotation.getResourceId());
//    return this;
//  }

//  public List<Long> getAnnotationIds() {
//    return annotationIds;
//  }

}
