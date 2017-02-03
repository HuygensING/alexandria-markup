package data_model;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 * <p>
 * A text node has textual content associated with it (as a UNICODE string).
 * A text node can also have multiple ranges on it... (see the Limen)
 */
public class TextNode {
  private String content = "";
  private TextNode previousTextNode = null; // for the leftmost (first) TextNode, this is null;
  private TextNode nextTextNode = null;     // for the rightmost (last) TextNode, this is null;

  public TextNode(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public TextNode setPreviousTextNode(TextNode previousTextNode) {
    this.previousTextNode = previousTextNode;
    if (previousTextNode != null && previousTextNode.getNextTextNode() == null) {
      previousTextNode.setNextTextNode(this);
    }
    return this;
  }

  public TextNode getPreviousTextNode() {
    return previousTextNode;
  }

  public TextNode setNextTextNode(TextNode nextTextNode) {
    this.nextTextNode = nextTextNode;
    if (nextTextNode != null && nextTextNode.getPreviousTextNode() == null) {
      nextTextNode.setPreviousTextNode(this);
    }
    return this;
  }

  public TextNode getNextTextNode() {
    return nextTextNode;
  }

  @Override
  public String toString() {
    return "\"" + content.replace("\n", "\\n") + "\"";
  }
}


