package nl.knaw.huygens.alexandria.lmnl.data_model;

import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeRangeIndex {
  final Logger LOG = LoggerFactory.getLogger(NodeRangeIndex.class);

  private List<IndexPoint> indexPoints;
  private KdTree<IndexPoint> kdTree;
  private Limen limen;
  private Set<Integer> invertedTextRangesIndices = new HashSet<>();

  public NodeRangeIndex(Limen limen) {
    this.limen = limen;
  }

  public List<IndexPoint> getIndexPoints() {
    if (indexPoints == null) {
      indexPoints = new ArrayList<>();

      Map<TextRange, Integer> textRangeIndex = new HashMap<>(limen.textRangeList.size());
      for (int i = 0; i < limen.textRangeList.size(); i++) {
        textRangeIndex.put(limen.textRangeList.get(i), i);
      }
      List<TextRange> textRangesToInvert = limen.textRangeList.stream()//
          .filter(limen::containsAtLeastHalfOfAllTextNodes)//
          .collect(Collectors.toList());
      invertedTextRangesIndices = textRangesToInvert.stream()//
          .map(textRangeIndex::get)//
          .collect(Collectors.toSet());

      AtomicInteger textNodeIndex = new AtomicInteger(0);
      limen.textNodeList.forEach(tn -> {
        LOG.debug("TextNode={}", tn);
        int i = textNodeIndex.getAndIncrement();

        // all the TextRanges associated with this TextNode
        Set<TextRange> textRanges = limen.getTextRanges(tn);

        // all the TextRanges that should be inverted and are NOT associated with this TextNode
        List<TextRange> relevantInvertedTextRanges = textRangesToInvert.stream()//
            .filter(tr -> !textRanges.contains(tr))//
            .collect(Collectors.toList());

        // ignore those TextRanges associated with this TextNode that should be inverted
        textRanges.removeAll(textRangesToInvert);

        // add all the TextRanges that should be inverted and are NOT associated with this TextNode
        textRanges.addAll(relevantInvertedTextRanges);

        textRanges.stream()//
            .sorted(Comparator.comparingInt(textRangeIndex::get))//
            .forEach(tr -> {
              int j = textRangeIndex.get(tr);
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
    rangeIndices.addAll(invertedTextRangesIndices);
    getKdTree().indexpointsForTextNode(i)//
        .forEach(ip -> {
          int textRangeIndex = ip.getTextRangeIndex();
          if (invertedTextRangesIndices.contains(textRangeIndex)) {
            // this is an inverted textrange, so this indexpoint means that textnode i is NOT part of this range
            rangeIndices.remove(textRangeIndex);
          } else {
            rangeIndices.add(textRangeIndex);
          }
        });
    return rangeIndices;
  }

  public Set<Integer> getTextNodes(int i) {
    Set<Integer> textNodeIndices = new HashSet<>();

    Set<Integer> relevantTextNodeIndices = getKdTree().indexpointsForTextRange(i).stream()//
        .map(IndexPoint::getTextNodeIndex)//
        .collect(toSet());

    if (invertedTextRangesIndices.contains(i)) {
      // range i is inverted, so start with all textnodes, then subtract
      IntStream.range(0, limen.textNodeList.size()).forEach(textNodeIndices::add);
      textNodeIndices.removeAll(relevantTextNodeIndices);

    } else {
      // range i is not inverted, so start empty, then add
      textNodeIndices.addAll(relevantTextNodeIndices);
    }
    return textNodeIndices;
  }

  public Set<Integer> getRanges0(int i) {
    Set<Integer> rangeIndices = new HashSet<>();
    rangeIndices.addAll(invertedTextRangesIndices);
    StreamSupport.stream(getKdTree().spliterator(), true)//
        .filter(ip -> ip.getTextNodeIndex() == i)//
        .forEach(ip -> {
          int textRangeIndex = ip.getTextRangeIndex();
          if (invertedTextRangesIndices.contains(textRangeIndex)) {
            // this is an inverted textrange, so this indexpoint means that textnode i is NOT part of this range
            rangeIndices.remove(textRangeIndex);
          } else {
            rangeIndices.add(textRangeIndex);
          }
        });
    return rangeIndices;
  }

  public Set<Integer> getTextNodes0(int i) {
    Set<Integer> textNodeIndices = new HashSet<>();
    List<Integer> relevantTextNodeIndices = StreamSupport.stream(getKdTree().spliterator(), true)//
        .filter(ip -> ip.getTextRangeIndex() == i)//
        .map(IndexPoint::getTextNodeIndex)//
        .collect(Collectors.toList());

    if (invertedTextRangesIndices.contains(i)) {
      // range i is inverted, so start with all textnodes, then subtract
      IntStream.range(0, limen.textNodeList.size()).forEach(textNodeIndices::add);
      textNodeIndices.removeAll(relevantTextNodeIndices);

    } else {
      // range i is not inverted, so start empty, then add
      textNodeIndices.addAll(relevantTextNodeIndices);
    }
    return textNodeIndices;
  }

}
