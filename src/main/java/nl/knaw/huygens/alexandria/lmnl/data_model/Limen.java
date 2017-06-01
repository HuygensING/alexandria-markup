package nl.knaw.huygens.alexandria.lmnl.data_model;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by Ronald Haentjens Dekker on 25/01/17.
 * <p>
 * A limen is a container for text nodes. A Document contains a Limen and each Annotation contains a Limen
 * And ranges.
 * For ease of use there is a convenience method here that maps TextNodes to text ranges;
 */
public class Limen {

  public final List<TextNode> textNodeList;
  public final List<Markup> markupList;
  private final Map<TextNode, Set<Markup>> textNodeToMarkup;

  public Limen() {
    this.textNodeList = new ArrayList<>();
    this.markupList = new ArrayList<>();
    this.textNodeToMarkup = new LinkedHashMap<>();
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

  public Limen addMarkup(Markup markup) {
    this.markupList.add(markup);
    return this;
  }

  public void associateTextWithRange(TextNode node, Markup markup) {
    textNodeToMarkup.computeIfAbsent(node, f -> new LinkedHashSet<>()).add(markup);
  }

  public void disAssociateTextWithRange(TextNode node, Markup markup) {
    textNodeToMarkup.computeIfAbsent(node, f -> new LinkedHashSet<>()).remove(markup);
  }

  public Iterator<TextNode> getTextNodeIterator() {
    return this.textNodeList.iterator();
  }

  public Set<Markup> getMarkups(TextNode node) {
    Set<Markup> markups = textNodeToMarkup.get(node);
    return markups == null ? new LinkedHashSet<>() : markups;
  }

  public boolean hasTextNodes() {
    return !textNodeList.isEmpty();
  }

  public boolean containsAtLeastHalfOfAllTextNodes(Markup markup) {
    int textNodeSize = textNodeList.size();
    return textNodeSize > 2 //
        && markup.textNodes.size() >= textNodeSize / 2d;
  }
}
