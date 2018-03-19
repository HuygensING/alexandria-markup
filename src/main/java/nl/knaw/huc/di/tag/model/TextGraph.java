package nl.knaw.huc.di.tag.model;

/*-
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
import java.util.*;

public class TextGraph<T extends TAGTextNode, M extends TAGMarkup> extends HyperGraph<Node, Edge> {

  private TextDelimiterNode textStartNode = new TextDelimiterNode();
  private TextDelimiterNode textEndNode = new TextDelimiterNode();
  private Map<M, MarkupNode> markupNodeIndex = new HashMap<>();

  protected TextGraph() {
    super(GraphType.ORDERED);
  }

//  public TextGraph<T, M> addTextNode(T textNode) {
//    this.textNodeList.add(textNode);
//    return this;
//  }
//
//  public TextGraph<T, M> addMarkup(M markup) {
//    this.markupList.add(markup);
//    return this;
//  }
//
//  public void associateTextNodeWithMarkup(T node, M markup) {
//    textNodeToMarkup.computeIfAbsent(node, f -> new LinkedHashSet<>()).add(markup);
//  }
//
//  public void disAssociateTextNodeFromMarkup(T node, M markup) {
//    textNodeToMarkup.computeIfAbsent(node, f -> new LinkedHashSet<>()).remove(markup);
//  }
//
//  public Set<M> getMarkups(T node) {
//    Set<M> markups = textNodeToMarkup.get(node);
//    return markups == null ? new LinkedHashSet<>() : markups;
//  }
//
//  public boolean hasTextNodes() {
//    return !textNodeList.isEmpty();
//  }

}
