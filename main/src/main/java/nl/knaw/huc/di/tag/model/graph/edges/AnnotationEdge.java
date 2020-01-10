package nl.knaw.huc.di.tag.model.graph.edges;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import com.sleepycat.persist.model.Persistent;
import nl.knaw.huygens.alexandria.storage.AnnotationType;

@Persistent(version=1)
public class AnnotationEdge implements Edge {
  private AnnotationType annotationType;
  private String field;
  private String id;

  private AnnotationEdge() {
  }

  public AnnotationEdge(AnnotationType type, String field, final String id) {
    annotationType = type;
    this.field = field;
    this.id = id;
  }

  public AnnotationType getAnnotationType() {
    return annotationType;
  }

  public String getField() {
    return field;
  }

  public boolean hasField(String field) {
    return this.field.equals(field);
  }

  public boolean hasType(AnnotationType type) {
    return annotationType.equals(type);
  }

  public String getLabel() {
    return EdgeType.hasAnnotation.name();
  }

  public String getId() {
    return id;
  }
}
