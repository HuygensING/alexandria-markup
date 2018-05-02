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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import static com.sleepycat.persist.model.Relationship.ONE_TO_MANY;
import static com.sleepycat.persist.model.Relationship.ONE_TO_ONE;
import com.sleepycat.persist.model.SecondaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(version = 1)
public class TAGAnnotation implements TAGObject {
  @PrimaryKey(sequence = "annotation_pk_sequence")
  private Long id;

  private String tag;

  @SecondaryKey(relate = ONE_TO_ONE, relatedEntity = TAGDocument.class)
  private Long documentId;

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGAnnotation.class)
  private final List<Long> annotationIds = new ArrayList<>();

  private TAGAnnotation() {
  }

  public TAGAnnotation(String tag) {
    this.tag = tag;
  }

  public void setId(long id) {
    this.id = id;
  }

  public Long getDbId() {
    return id;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public String getTag() {
    return tag;
  }

  public TAGAnnotation addAnnotation(TAGAnnotation annotation) {
    annotationIds.add(annotation.getDbId());
    return this;
  }

  public List<Long> getAnnotationIds() {
    return annotationIds;
  }

  public void setDocumentId(long documentId) {
    this.documentId = documentId;
  }

  public Long getDocumentId() {
    return documentId;
  }

}
