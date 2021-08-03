package nl.knaw.huc.di.tag.model.graph;

/*-
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

import org.junit.jupiter.api.Test;

import nl.knaw.huc.di.tag.model.graph.edges.DocumentToTextEdge;
import nl.knaw.huc.di.tag.model.graph.edges.Edge;
import nl.knaw.huc.di.tag.model.graph.nodes.DocumentNode;
import nl.knaw.huc.di.tag.model.graph.nodes.Node;
import nl.knaw.huc.di.tag.model.graph.nodes.TextNode;

import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;

public class HyperGraphTest {

  @Test
  public void test() {
    HyperGraph<String, String> hg = new HyperGraph<>(HyperGraph.GraphType.UNORDERED);
    assertThat(hg).isNotNull();

    hg.addDirectedHyperEdge("edge", "label", "source", "target");
  }

  @Test
  public void test2() {
    HyperGraph<Node, Edge> hg = new HyperGraph<>(HyperGraph.GraphType.UNORDERED);
    assertThat(hg).isNotNull();

    final DocumentNode documentNode = new DocumentNode();
    hg.addNode(documentNode, DocumentNode.LABEL);

    TextNode textNode = new TextNode("text1");
    hg.addNode(textNode, textNode.getLabel());

    final DocumentToTextEdge firstTextEdge = new DocumentToTextEdge();
    hg.addDirectedHyperEdge(firstTextEdge, DocumentToTextEdge.LABEL, documentNode, textNode);
  }

  @Test
  public void testRemoveTargetsFromHyperEdge() {
    HyperGraph<String, String> hg = new HyperGraph<>(HyperGraph.GraphType.UNORDERED);

    String dogs = "branch1";
    String dog = "dog";
    String poodle = "poodle";
    String labrador = "labrador";
    String mammals = "branch2";
    String mammal = "mammal";
    hg.addDirectedHyperEdge(mammals, "contains", mammal, dog);
    hg.addDirectedHyperEdge(dogs, "contains", dog, poodle, labrador);

    String husky = "husky";
    hg.addTargetsToHyperEdge(dogs, husky);

    assertThat(hg.getOutgoingEdges(dog)).containsExactly(dogs);

    assertThat(hg.getTargets(dogs)).containsExactlyInAnyOrder(poodle, labrador, husky);

    hg.removeTargetsFromHyperEdge(dogs, poodle, labrador, husky);

    assertThat(hg.getOutgoingEdges(dog)).isEmpty();
  }
}
