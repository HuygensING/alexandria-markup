package nl.knaw.huygens.alexandria.lmnl.data_model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeRangeIndex {
  final Logger LOG = LoggerFactory.getLogger(NodeRangeIndex.class);

  private List<IndexPoint> indexPoints;
  private KdTree<IndexPoint> kdTree;
  private Limen limen;

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

      AtomicInteger textNodeIndex = new AtomicInteger(0);
      limen.textNodeList.forEach(tn -> {
        LOG.info("TextNode={}", tn);
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

}
