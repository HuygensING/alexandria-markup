package data_model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 */
public class TextRange {
  private final Limen owner;
  private final String tag;
  private final List<Annotation> annotations;
  private final List<TextNode> textNodes;

  public TextRange(Limen owner, String tag) {
    this.owner = owner;
    this.tag = tag;
    this.annotations = new ArrayList<>();
    this.textNodes = new ArrayList<>();
  }

  public TextRange addTextNode(TextNode node) {
    this.textNodes.add(node);
    this.owner.associateTextWithRange(node, this);
    return this;
  }

  public TextRange addAnnotation(Annotation annotation) {
    this.annotations.add(annotation);
    return this;
  }

  public String getTag() {
    return tag;
  }

  public TextRange setFirstAndLastTextNode(TextNode firstTextNode, TextNode lastTextNode) {
    this.textNodes.clear();
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

  public TextRange setOnlyTextNode(TextNode textNode) {
    this.textNodes.clear();
    addTextNode(textNode);
    return this;
  }
}
