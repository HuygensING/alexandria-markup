package nl.knaw.huc.di.tag.model.graph.edges;

import com.sleepycat.persist.model.Persistent;

import java.util.Set;

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
@Persistent
public class LayerEdge implements Edge {
  private final EdgeType edgeType;
  private final String layerName;

  public LayerEdge() {
    edgeType = null;
    layerName = "";
  }

  public LayerEdge(final EdgeType edgeType, final String layerName) {
    this.edgeType = edgeType;
    this.layerName = layerName;
  }

  public boolean hasType(EdgeType type) {
    return type.equals(edgeType);
  }

  public boolean hasLayer(final String layerName) {
    return this.layerName.equals(layerName);
  }

  public boolean hasAnyLayerFrom(final Set<String> layers) {
    return layers.contains(this.layerName);
  }

  public String label() {
    return edgeType.name() + ":" + layerName;
  }

  public String getLayerName() {
    return layerName;
  }

}
