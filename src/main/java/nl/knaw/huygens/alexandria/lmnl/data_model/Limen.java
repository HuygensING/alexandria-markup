package nl.knaw.huygens.alexandria.lmnl.data_model;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by Ronald Haentjens Dekker on 25/01/17.
 * <p>
 * A limen is a container for text nodes. A Document contains a Limen and each Annotation contains a Limen
 * And ranges.
 * For ease of use there is a convenience method here that maps TextNodes to text ranges;
 */
public class Limen {

  public final List<TextNode> textNodeList;
  public final List<TextRange> textRangeList;
  private final Map<TextNode, Set<TextRange>> textNodeToTextRange;

  public Limen() {
    this.textNodeList = new ArrayList<>();
    this.textRangeList = new ArrayList<>();
    this.textNodeToTextRange = new LinkedHashMap<>();
  }

  public Limen addTextNode(TextNode textNode) {
    this.textNodeList.add(textNode);
    if (textNodeList.size() > 1) {
      TextNode previousTextNode = textNodeList.get(textNodeList.size() - 2);
      textNode.setPreviousTextNode(previousTextNode);
    }
    return this;
  }

  public Limen setOnlyTextNode(TextNode textNode) {
    this.textNodeList.clear();
    this.textNodeList.add(textNode);
    return this;
  }

  public Limen setFirstAndLastTextNode(TextNode firstTextNode, TextNode lastTextNode) {
    textNodeList.clear();
    addTextNode(firstTextNode);
    if (firstTextNode != lastTextNode) {
      TextNode next = firstTextNode.getNextTextNode();
      while (next != lastTextNode) {
        addTextNode(next);
        next = next.getNextTextNode();
      }
      addTextNode(next);
    }
    return this;
  }

  public Limen addTextRange(TextRange textRange) {
    this.textRangeList.add(textRange);
    return this;
  }

  public void associateTextWithRange(TextNode node, TextRange textRange) {
    textNodeToTextRange.computeIfAbsent(node, f -> new LinkedHashSet<>()).add(textRange);
  }

  public Iterator<TextNode> getTextNodeIterator() {
    return this.textNodeList.iterator();
  }

  public Set<TextRange> getTextRanges(TextNode node) {
    Set<TextRange> textRanges = textNodeToTextRange.get(node);
    return textRanges == null ? new LinkedHashSet<>() : textRanges;
  }

  public boolean hasTextNodes() {
    return !textNodeList.isEmpty();
  }

  public List<IndexPoint> getIndexPoints() {
    Map<TextRange, Integer> textRangeIndex = new HashMap<>(textRangeList.size());
    for (int i = 0; i < textRangeList.size(); i++) {
      textRangeIndex.put(textRangeList.get(i), i);
    }
    List<TextRange> textRangesToInvert = textRangeList.stream()//
        .filter(this::containsAtLeastHalfOfAllTextNodes)//
        .collect(Collectors.toList());

    List<IndexPoint> list = new ArrayList<>();
    AtomicInteger textNodeIndex = new AtomicInteger(0);
    textNodeList.forEach(tn -> {
      int i = textNodeIndex.getAndIncrement();

      // all the TextRanges associated with this TextNode
      Set<TextRange> textRanges = getTextRanges(tn);

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
            list.add(point);
          });
    });

    return list;
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TextRange textRange) {
    int textNodeSize = textNodeList.size();
    return textNodeSize > 2 //
        && textRange.textNodes.size() >= textNodeSize / 2d;
  }
}
