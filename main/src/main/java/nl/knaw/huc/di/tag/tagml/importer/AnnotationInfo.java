package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.storage.AnnotationType;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

public class AnnotationInfo {
  Long nodeId;
  AnnotationType type;
  String name;
  private String id;

  public AnnotationInfo(Long nodeId, AnnotationType type, String name) {
    this.nodeId = nodeId;
    this.type = type;
    this.name = name;
  }

  public Long getNodeId() {
    return nodeId;
  }

  public AnnotationType getType() {
    return type;
  }

  public String getName() {
    return name;
  }

  public boolean hasName(final String name) {
    return this.name.equals(name);
  }

  public boolean hasName() {
    return StringUtils.isNotEmpty(name);
  }

  public void setId(final String id) {
    this.id = id;
  }

  public Optional<String> getId() {
    return Optional.ofNullable(id);
  }
}
