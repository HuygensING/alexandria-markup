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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huc.di.tag.model.graph.edges.Edge;

// nu zou ik wel topological sort willen hebben
// teveel gedoe, kan ook gewoon een root node maken
public class DirectedAcyclicGraph<N> extends HyperGraph<N, Edge> {
  private static final Logger LOG = LoggerFactory.getLogger(DirectedAcyclicGraph.class);
  private N root;

  protected DirectedAcyclicGraph() {
    super(GraphType.ORDERED);
  }

  @Override
  public void addNode(N node, String label) {
    super.addNode(node, label);
  }

  protected void setRootNode(N root) {
    this.root = root;
  }

  //  // Question: do we want labels here?
  //  public void addDirectedEdge(N source, N target) {
  //    TraditionalEdge edge = new TraditionalEdge(sigils);
  //    super.addDirectedHyperEdge(edge, "", source, target);
  //  }

  public List<N> traverse() {
    Set<N> visitedNodes = new HashSet<>();
    Stack<N> nodesToVisit = new Stack<>();
    nodesToVisit.add(root);
    List<N> result = new ArrayList<>();
    while (!nodesToVisit.isEmpty()) {
      N pop = nodesToVisit.pop();
      if (!visitedNodes.contains(pop)) {
        result.add(pop);
        Collection<Edge> outgoingEdges = this.getOutgoingEdges(pop);
        visitedNodes.add(pop);
        for (Edge e : outgoingEdges) {
          N target = this.getTarget(e);
          if (target == null) {
            throw new RuntimeException("edge target is null for edge " + pop + "->");
          }
          nodesToVisit.add(target);
        }
      } else {
        LOG.debug("revisiting node {}", pop);
      }
    }
    return result;
  }

  public N getTarget(Edge e) {
    Collection<N> nodes = super.getTargets(e);
    if (nodes.size() != 1) {
      throw new RuntimeException("trouble!");
    }
    return nodes.iterator().next();
  }
}
