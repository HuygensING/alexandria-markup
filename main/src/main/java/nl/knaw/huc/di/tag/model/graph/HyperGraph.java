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

/*
 * Generic Hypergraph definition
 * Directed, labelled, hyperedges (one-to-many)
 * Are we going to make the child nodes ordered?
 * @author: Ronald Haentjens Dekker
 */

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.sleepycat.persist.model.NotPersistent;
import com.sleepycat.persist.model.Persistent;

import static java.util.Arrays.asList;

@Persistent
public class HyperGraph<N, H> {
  private final GraphType graphType;
  @NotPersistent private final Function<N, Collection<H>> mappingFunction;
  private final Set<N> nodes;
  private final Map<N, Collection<H>> incomingEdges;
  private final Map<N, Collection<H>> outgoingEdges;
  private final Map<H, N> sourceNode;
  private final Map<H, Collection<N>> targetNodes;
  private final Map<H, String> edgeLabels;
  private final Map<N, String> nodeLabels;

  protected HyperGraph() {
    this(GraphType.ORDERED);
  }

  protected HyperGraph(GraphType graphType) {
    this.graphType = graphType;
    this.nodes = new HashSet<>();
    this.incomingEdges = new HashMap<>();
    this.outgoingEdges = new HashMap<>();
    this.sourceNode = new HashMap<>();
    this.targetNodes = new HashMap<>();
    this.edgeLabels = new HashMap<>();
    this.nodeLabels = new HashMap<>();
    // create switch
    if (GraphType.ORDERED == this.graphType) {
      this.mappingFunction = e -> new ArrayList<>();
    } else {
      this.mappingFunction = e -> new HashSet<>();
    }
  }

  protected void addNode(N node, String label) {
    this.nodes.add(node);
    this.nodeLabels.put(node, label);
  }

  @SafeVarargs
  protected final void addDirectedHyperEdge(H edge, String label, N source, N... targets) {
    // TODO: check whether source node is in nodes
    // NOTE: The way it is done now, is that nodes are not added explicitly to the graph
    // NOTE: but rather indirectly through the edges.
    // set source
    sourceNode.put(edge, source);
    // set targets
    if (GraphType.ORDERED == this.graphType) {
      List<N> targetList = new ArrayList<>(asList(targets));

      targetNodes.put(edge, targetList);
    } else {
      // convert Array target to set
      Set<N> targetSet = new HashSet<>(asList(targets));
      targetNodes.put(edge, targetSet);
    }
    // set incoming
    for (N target : targets) {
      incomingEdges.computeIfAbsent(target, mappingFunction).add(edge);
    }
    // set outgoing
    outgoingEdges.computeIfAbsent(source, mappingFunction).add(edge);
    // set label
    edgeLabels.put(edge, label);
  }

  @SafeVarargs
  protected final void addTargetsToHyperEdge(H edge, N... targets) {
    if (!targetNodes.containsKey(edge)) {
      throw new RuntimeException("unknown hyperedge " + edge);
    }
    Collection<N> collection = targetNodes.get(edge);
    collection.addAll(asList(targets));
    for (N target : targets) {
      incomingEdges.computeIfAbsent(target, mappingFunction).add(edge);
    }
  }

  @SafeVarargs
  protected final void removeTargetsFromHyperEdge(H edge, N... targets) {
    if (!targetNodes.containsKey(edge)) {
      throw new RuntimeException("unknown hyperedge " + edge);
    }
    Collection<N> collection = targetNodes.get(edge);
    collection.removeAll(asList(targets));
    for (N target : targets) {
      incomingEdges.get(target).remove(edge);
    }
    if (collection.isEmpty()) {
      removeHyperEdge(edge);
    }
  }

  public Collection<N> getTargets(H e) {
    return targetNodes.get(e);
  }

  public N getSource(H e) {
    return sourceNode.get(e);
  }

  public Collection<H> getOutgoingEdges(N node) {
    return outgoingEdges.getOrDefault(node, Collections.EMPTY_LIST);
  }

  public Collection<H> getIncomingEdges(N node) {
    return incomingEdges.getOrDefault(node, Collections.EMPTY_LIST);
  }

  public enum GraphType {
    ORDERED,
    UNORDERED
  }

  protected boolean nodeExists(N node) {
    return nodes.contains(node);
  }

  private void removeHyperEdge(final H edge) {
    N sourceNode = this.sourceNode.get(edge);
    outgoingEdges.get(sourceNode).remove(edge);
    targetNodes.remove(edge);
    edgeLabels.remove(edge);
  }
}
