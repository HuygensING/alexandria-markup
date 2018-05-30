package nl.knaw.huygens.alexandria.storage;

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
import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import nl.knaw.huc.di.tag.model.graph.DirectedAcyclicGraph;
import nl.knaw.huc.di.tag.model.graph.edges.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.storage.Layer.EdgeType.markupToText;
import static nl.knaw.huygens.alexandria.storage.Layer.EdgeType.parentMarkupToChildMarkup;
import static nl.knaw.huygens.alexandria.storage.Layer.Edges.markupToText;
import static nl.knaw.huygens.alexandria.storage.Layer.Edges.parentMarkupToChildMarkup;

@Entity(version = 1)
public class Layer extends DirectedAcyclicGraph<Long> implements TAGObject {
  Logger LOG = LoggerFactory.getLogger(getClass());
  String id = "";

  @PrimaryKey(sequence = "layer_pk_sequence")
  private Long dbId;

  @Override
  public Long getDbId() {
    return dbId;
  }

  enum EdgeType {
    parentMarkupToChildMarkup,
    markupToText
  }

  static class MyEdge implements Edge {
    private EdgeType type;
    private Long id;

    MyEdge(EdgeType type, Long id) {
      this.type = type;
      this.id = id;
    }

    public EdgeType getType() {
      return type;
    }

    public boolean hasType(EdgeType edgeType) {
      return edgeType.equals(type);
    }
  }

  static class Edges {
    static AtomicLong edgeCounter = new AtomicLong();

    public static MyEdge parentMarkupToChildMarkup() {
      return new MyEdge(parentMarkupToChildMarkup, edgeCounter.getAndIncrement());
    }

    public static MyEdge markupToText() {
      return new MyEdge(markupToText, edgeCounter.getAndIncrement());
    }
  }

  public Layer(String id, Long rootMarkupId) {
    this.id = id;
    setRootNode(rootMarkupId);
  }

  public void addDescendantMarkup(Long parentId, Long childId) {
    addDirectedHyperEdge(parentMarkupToChildMarkup(), "", parentId, childId);
  }

  public void linkTextNodeToMarkup(Long textNodeId, Long markupId) {
//    if (!nodeExists(markupId)) {
//      throw new RuntimeException("markupNode " + markupId + " is not part of this layer, call addDescendantMarkup() first.");
//    }

    addDirectedHyperEdge(markupToText(), "", markupId, textNodeId);
  }

  public List<Long> getMarkupIdsForTextId(Long textNodeId) {
    List<Long> list = new ArrayList<>();
    List<Long> nodesToProcess = new ArrayList<>();
    nodesToProcess.add(textNodeId);
    while (!nodesToProcess.isEmpty()) {
      List<Long> parentMarkup = nodesToProcess.stream()
          .flatMap(node -> getIncomingEdgeStream(node)
              .map(this::getSource)
          ).collect(toList());
      list.addAll(parentMarkup);
      nodesToProcess = parentMarkup;
    }
    return list;
  }

  public List<Long> getTextIdsForMarkupId(Long markupId) {
    List<Long> list = new ArrayList<>();
    List<Long> nodesToProcess = new ArrayList<>();
    nodesToProcess.add(markupId);
    while (!nodesToProcess.isEmpty()) {
      List<Long> childMarkup = nodesToProcess.stream()
          .flatMap(node -> getOutgoingEdgeStream(node)
              .filter(e -> e.hasType(parentMarkupToChildMarkup))
              .map(this::getTarget)
          ).collect(toList());
      List<Long> textNodes = nodesToProcess.stream()
          .flatMap(node -> getOutgoingEdgeStream(node)
              .filter(e -> e.hasType(markupToText))
              .map(this::getTarget)
          ).collect(toList());

      list.addAll(textNodes);
      nodesToProcess = childMarkup;
    }
    return list;
  }

  private Stream<MyEdge> getOutgoingEdgeStream(Long nodeId) {
    return getOutgoingEdges(nodeId).stream().map(MyEdge.class::cast);
  }

  private Stream<MyEdge> getIncomingEdgeStream(Long nodeId) {
    return getIncomingEdges(nodeId).stream().map(MyEdge.class::cast);
  }

}
