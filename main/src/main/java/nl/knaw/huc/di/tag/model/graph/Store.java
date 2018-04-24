package nl.knaw.huc.di.tag.model.graph;

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
import nl.knaw.huc.di.tag.model.graph.edges.DocumentToTextEdge;
import nl.knaw.huc.di.tag.model.graph.edges.Edge;
import nl.knaw.huc.di.tag.model.graph.nodes.*;

public class Store {

  static final HyperGraph<Node, Edge> hg = new HyperGraph<Node, Edge>(HyperGraph.GraphType.ORDERED);

  public static DocumentNode createDocumentNode() {
    DocumentNode documentNode = new DocumentNode();
    hg.addNode(documentNode, DocumentNode.LABEL);
    return documentNode;
  }

  public static TextNode createTextNode(String content) {
    TextNode textNode = new TextNode(content);
    hg.addNode(textNode, TextNode.LABEL);
    return textNode;
  }

  public static MilestoneNode createMilestoneNode() {
    MilestoneNode milestoneNode = new MilestoneNode();
    hg.addNode(milestoneNode, MilestoneNode.LABEL);
    return milestoneNode;
  }

  public static TextDivergenceNode createTextDivergenceNode() {
    TextDivergenceNode textDivergenceNode = new TextDivergenceNode();
    hg.addNode(textDivergenceNode, TextDivergenceNode.LABEL);
    return textDivergenceNode;
  }

  public static TextConvergenceNode createTextConvergenceNode() {
    return new TextConvergenceNode();
  }

  public static MarkupNode createMarkupNode(String name) {
    return new MarkupNode(name);
  }

  public static AnnotationNode createAnnotationNode(String name, Object value) {
    return new AnnotationNode(name, value);
  }

  public void setFirstTextNode(DocumentNode documentNode, TextNode firstTextNode) {
    DocumentToTextEdge d2t = new DocumentToTextEdge();
    hg.addDirectedHyperEdge(d2t, DocumentToTextEdge.LABEL, documentNode, firstTextNode);
  }

  // store: nodes + adjacencyLists

}
