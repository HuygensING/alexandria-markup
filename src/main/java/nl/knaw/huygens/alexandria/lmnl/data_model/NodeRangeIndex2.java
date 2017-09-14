package nl.knaw.huygens.alexandria.lmnl.data_model;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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


import static java.util.stream.Collectors.toSet;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dao.TAGMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

public class NodeRangeIndex2 {
  final Logger LOG = LoggerFactory.getLogger(NodeRangeIndex2.class);

  private List<IndexPoint> indexPoints;
  private KdTree<IndexPoint> kdTree;
  private TAGStore store;
  private TAGDocument document;
  private Set<Integer> invertedMarkupsIndices = new HashSet<>();

  public NodeRangeIndex2(TAGStore store, TAGDocument document) {
    this.store = store;
    this.document = document;
  }

  public List<IndexPoint> getIndexPoints() {
    if (indexPoints == null) {
      indexPoints = new ArrayList<>();

      Map<Long, Integer> markupIndex = new HashMap<>(document.getMarkupIds().size());
      for (int i = 0; i < document.getMarkupIds().size(); i++) {
        markupIndex.put(document.getMarkupIds().get(i), i);
      }
      List<TAGMarkup> markupsToInvert = document.getMarkupIds().stream()//
          .filter(document::containsAtLeastHalfOfAllTextNodes)//
          .map(store::getMarkup)//
          .collect(Collectors.toList());
      invertedMarkupsIndices = markupsToInvert.stream()//
          .map(markupIndex::get)//
          .collect(Collectors.toSet());

      AtomicInteger textNodeIndex = new AtomicInteger(0);
      document.getTextNodeIds().stream().map(store::getTextNode).forEach(tn -> {
        LOG.debug("TextNode={}", tn);
        int i = textNodeIndex.getAndIncrement();

        // all the Markups associated with this TextNode
        Set<TAGMarkup> markups = store.getMarkupsForTextNode(tn);

        // all the Markups that should be inverted and are NOT associated with this TextNode
        List<TAGMarkup> relevantInvertedMarkups = markupsToInvert.stream()//
            .filter(tr -> !markups.contains(tr))//
            .collect(Collectors.toList());

        // ignore those Markups associated with this TextNode that should be inverted
        markups.removeAll(markupsToInvert);

        // add all the Markups that should be inverted and are NOT associated with this TextNode
        markups.addAll(relevantInvertedMarkups);

        markups.stream()//
            .sorted(Comparator.comparingInt(markupIndex::get))//
            .forEach(tr -> {
              int j = markupIndex.get(tr);
              IndexPoint point = new IndexPoint(i, j);
              indexPoints.add(point);
            });
      });
    }
    return indexPoints;
  }

  public KdTree<IndexPoint> getKdTree() {
    if (kdTree == null) {
      kdTree = new KdTree<>(getIndexPoints());
    }
    return kdTree;
  }

  public Set<Integer> getRanges(int i) {
    Set<Integer> rangeIndices = new HashSet<>();
    rangeIndices.addAll(invertedMarkupsIndices);
    getKdTree().indexpointsForTextNode(i)//
        .forEach(ip -> {
          int markupIndex = ip.getMarkupIndex();
          if (invertedMarkupsIndices.contains(markupIndex)) {
            // this is an inverted markup, so this indexpoint means that textnode i is NOT part of this markup set
            rangeIndices.remove(markupIndex);
          } else {
            rangeIndices.add(markupIndex);
          }
        });
    return rangeIndices;
  }

  public Set<Integer> getTextNodes(int i) {
    Set<Integer> textNodeIndices = new HashSet<>();

    Set<Integer> relevantTextNodeIndices = getKdTree().indexpointsForMarkup(i).stream()//
        .map(IndexPoint::getTextNodeIndex)//
        .collect(toSet());

    if (invertedMarkupsIndices.contains(i)) {
      // range i is inverted, so start with all textnodes, then subtract
      IntStream.range(0, document.getTextNodeIds().size()).forEach(textNodeIndices::add);
      textNodeIndices.removeAll(relevantTextNodeIndices);

    } else {
      // range i is not inverted, so start empty, then add
      textNodeIndices.addAll(relevantTextNodeIndices);
    }
    return textNodeIndices;
  }

  public Set<Integer> getRanges0(int i) {
    Set<Integer> rangeIndices = new HashSet<>();
    rangeIndices.addAll(invertedMarkupsIndices);
    StreamSupport.stream(getKdTree().spliterator(), true)//
        .filter(ip -> ip.getTextNodeIndex() == i)//
        .forEach(ip -> {
          int markupIndex = ip.getMarkupIndex();
          if (invertedMarkupsIndices.contains(markupIndex)) {
            // this is an inverted markup, so this indexpoint means that textnode i is NOT part of this markup set
            rangeIndices.remove(markupIndex);
          } else {
            rangeIndices.add(markupIndex);
          }
        });
    return rangeIndices;
  }

  public Set<Integer> getTextNodes0(int i) {
    Set<Integer> textNodeIndices = new HashSet<>();
    List<Integer> relevantTextNodeIndices = StreamSupport.stream(getKdTree().spliterator(), true)//
        .filter(ip -> ip.getMarkupIndex() == i)//
        .map(IndexPoint::getTextNodeIndex)//
        .collect(Collectors.toList());

    if (invertedMarkupsIndices.contains(i)) {
      // range i is inverted, so start with all textnodes, then subtract
      IntStream.range(0, document.getTextNodeIds().size()).forEach(textNodeIndices::add);
      textNodeIndices.removeAll(relevantTextNodeIndices);

    } else {
      // range i is not inverted, so start empty, then add
      textNodeIndices.addAll(relevantTextNodeIndices);
    }
    return textNodeIndices;
  }

}
