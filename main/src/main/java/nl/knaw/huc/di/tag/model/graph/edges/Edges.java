package nl.knaw.huc.di.tag.model.graph.edges;

/*-
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
import nl.knaw.huc.di.tag.model.graph.TextChainEdge;

import static nl.knaw.huc.di.tag.model.graph.edges.EdgeType.hasMarkup;
import static nl.knaw.huc.di.tag.model.graph.edges.EdgeType.hasText;

public class Edges {

  public static LayerEdge parentMarkupToChildMarkup(String layerName) {
    return new LayerEdge(hasMarkup, layerName);
  }

  public static TextChainEdge textChainEdge() {
    return new TextChainEdge();
  }

  public static LayerEdge markupToText(final String layerName) {
    return new LayerEdge(hasText, layerName);
  }
}
