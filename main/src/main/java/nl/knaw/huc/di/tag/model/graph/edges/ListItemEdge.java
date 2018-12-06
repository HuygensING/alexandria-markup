package nl.knaw.huc.di.tag.model.graph.edges;

/*-
 * #%L
 * main
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
import com.sleepycat.persist.model.Persistent;
import nl.knaw.huygens.alexandria.storage.AnnotationType;

@Persistent
public class ListItemEdge implements Edge {
  AnnotationType annotationType;

  private ListItemEdge() {
  }

  public ListItemEdge(AnnotationType type) {
    annotationType = type;
  }

  public AnnotationType getAnnotationType() {
    return annotationType;
  }


  public boolean hasType(AnnotationType type) {
    return annotationType.equals(type);
  }

  public String getLabel() {
    return EdgeType.hasItem.name();
  }

}
