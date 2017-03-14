package nl.knaw.huygens.alexandria.lmnl.modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;

public class LMNLModifier {

  Logger LOG = LoggerFactory.getLogger(LMNLModifier.class);

  private Limen limen;

  public LMNLModifier(Limen limen) {
    this.limen = limen;
  }

  public void addTextRange(TextRange newTextRange, Position position) {
    TextNodeCursor cursor = new TextNodeCursor(limen);
    int startOffset = position.getOffset();
    int endOffset = position.getOffset() + position.getLength();
    boolean goOn = true;
    int currentOffset = 0;
    String currentText = null;
    int currentTextNodeLength = 0;
    // get to starting TextNode
    while (goOn) {
      currentText = cursor.getCurrentText();
      currentTextNodeLength = cursor.getCurrentTextLength();
      if (currentOffset + currentTextNodeLength >= startOffset) {
        int textSize = currentOffset + currentTextNodeLength;
        int tailLength = Math.min(textSize, textSize - startOffset + 1);
        int headLength = currentTextNodeLength - tailLength;
        String headText = currentText.substring(0, headLength);
        String tailText = currentText.substring(headLength);
        if (headLength > 0) {
          // detach head
          cursor.getCurrentTextNode().setContent(headText);
          TextNode newTextNode = new TextNode(tailText);
          newTextNode.setPreviousTextNode(cursor.getCurrentTextNode());
          newTextNode.setNextTextNode(cursor.getCurrentTextNode()).getNextTextNode();
          cursor.getCurrentTextNode().setNextTextNode(newTextNode);
          limen.getTextRanges(cursor.getCurrentTextNode()).forEach(tr -> limen.associateTextWithRange(newTextNode, tr));
          limen.associateTextWithRange(newTextNode, newTextRange);
          limen.textNodeList.add(cursor.getTextNodeIndex() + 1, newTextNode);
        }
        goOn = false;

      } else {
        goOn = cursor.canAdvance();
        cursor.advance();
        currentOffset += currentTextNodeLength;
      }

    }

    goOn = true;
    while (goOn) {
      if (currentOffset + currentTextNodeLength < endOffset) {
        limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
        goOn = cursor.canAdvance();
        cursor.advance();
        currentOffset += currentTextNodeLength;

      } else {
        int textSize = currentOffset + currentTextNodeLength;
        int tailLength = textSize - endOffset;

        LOG.info("cursor.getCurrentTextLength()={}", cursor.getCurrentTextLength());
        LOG.info("textSize={}", textSize);
        int headLength = cursor.getCurrentTextLength() - tailLength;
        String headText = cursor.getCurrentText().substring(0, headLength);
        String tailText = cursor.getCurrentText().substring(headLength);

        if (tailLength > 0) {
          // detach head
          cursor.getCurrentTextNode().setContent(tailText);
          TextNode newTextNode = new TextNode(headText);
          newTextNode.setNextTextNode(cursor.getCurrentTextNode());
          newTextNode.setPreviousTextNode(cursor.getCurrentTextNode().getPreviousTextNode());
          cursor.getCurrentTextNode().setPreviousTextNode(newTextNode);
          limen.getTextRanges(cursor.getCurrentTextNode()).forEach(tr -> limen.associateTextWithRange(newTextNode, tr));
          limen.associateTextWithRange(newTextNode, newTextRange);
          limen.textNodeList.add(cursor.getTextNodeIndex(), newTextNode);
        }
        goOn = false;

      }
    }

  }

  class TextNodeCursor {
    private TextNode currentTextNode;
    private String currentText;
    private int currentTextLength;
    private int textNodeIndex = 0;

    public TextNodeCursor(Limen limen) {
      currentTextNode = limen.textNodeList.get(0);
      setCurrentTextAndLength();
    }

    private void setCurrentTextAndLength() {
      currentText = currentTextNode.getContent();
      currentTextLength = currentText.length();
    }

    public void advance() {
      currentTextNode = currentTextNode.getNextTextNode();
      setCurrentTextAndLength();
      textNodeIndex++;
    }

    public TextNode getCurrentTextNode() {
      return currentTextNode;
    }

    public String getCurrentText() {
      return currentText;
    }

    public int getCurrentTextLength() {
      return currentTextLength;
    }

    public int getTextNodeIndex() {
      return textNodeIndex;
    }

    public boolean canAdvance() {
      return currentTextNode.getNextTextNode() != null;
    }

  }

}
