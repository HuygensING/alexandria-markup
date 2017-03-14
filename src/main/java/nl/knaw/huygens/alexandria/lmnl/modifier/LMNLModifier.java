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
    int offsetAtEndOfCurrentTextNode = currentOffset + currentTextNodeLength;
    while (goOn) {
      currentText = cursor.getCurrentText();
      currentTextNodeLength = cursor.getCurrentTextLength();
      offsetAtEndOfCurrentTextNode = currentOffset + currentTextNodeLength;
      if (startOffset <= offsetAtEndOfCurrentTextNode) {
        int tailLength = Math.min(offsetAtEndOfCurrentTextNode, offsetAtEndOfCurrentTextNode - startOffset);
        int headLength = currentTextNodeLength - tailLength;
        String headText = currentText.substring(0, headLength);
        String tailText = currentText.substring(headLength);
        if (headLength > 0) {
          // detach head
          cursor.getCurrentTextNode().setContent(headText);
          TextNode newTextNode = new TextNode(tailText);
          TextNode nextTextNode = cursor.getCurrentTextNode().getNextTextNode();
          newTextNode.setPreviousTextNode(cursor.getCurrentTextNode());
          newTextNode.setNextTextNode(nextTextNode);
          cursor.getCurrentTextNode().setNextTextNode(newTextNode);
          limen.getTextRanges(cursor.getCurrentTextNode()).forEach(tr -> limen.associateTextWithRange(newTextNode, tr));
          limen.associateTextWithRange(newTextNode, newTextRange);
          limen.textNodeList.add(cursor.getTextNodeIndex() + 1, newTextNode);
          cursor.advance();
        }
        goOn = false;

      } else {
        goOn = cursor.canAdvance();
        cursor.advance();
        currentOffset += currentTextNodeLength;
        offsetAtEndOfCurrentTextNode = currentOffset + currentTextNodeLength;
      }

    }

    goOn = true;
    while (goOn) {
      if (offsetAtEndOfCurrentTextNode < endOffset) {
        limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
        goOn = cursor.canAdvance();
        cursor.advance();
        currentOffset += currentTextNodeLength;
        offsetAtEndOfCurrentTextNode = currentOffset + currentTextNodeLength;

      } else {
        int tailLength = offsetAtEndOfCurrentTextNode - endOffset;

//        LOG.info("cursor.getCurrentTextLength()={}", cursor.getCurrentTextLength());
//        LOG.info("textSize={}", offsetAtEndOfCurrentTextNode);
        int headLength = cursor.getCurrentTextLength() - tailLength;
        String headText = cursor.getCurrentText().substring(0, headLength);
        String tailText = cursor.getCurrentText().substring(headLength);

        if (tailLength > 0) {
          // detach head
          cursor.getCurrentTextNode().setContent(tailText);
          TextNode newTextNode = new TextNode(headText);
          TextNode previousTextNode = cursor.getCurrentTextNode().getPreviousTextNode();
          newTextNode.setNextTextNode(cursor.getCurrentTextNode());
          newTextNode.setPreviousTextNode(previousTextNode);
          cursor.getCurrentTextNode().setPreviousTextNode(newTextNode);
          limen.getTextRanges(cursor.getCurrentTextNode()).forEach(tr -> limen.associateTextWithRange(newTextNode, tr));
          limen.associateTextWithRange(newTextNode, newTextRange);
          limen.textNodeList.add(cursor.getTextNodeIndex(), newTextNode);
          limen.disAssociateTextWithRange(cursor.getCurrentTextNode(),newTextRange);
        }
        goOn = false;

      }
    }

  }

  class TextNodeCursor {
    private TextNode currentTextNode;
    private int textNodeIndex = 0;

    public TextNodeCursor(Limen limen) {
      currentTextNode = limen.textNodeList.get(0);
    }

    public void advance() {
      currentTextNode = currentTextNode.getNextTextNode();
      textNodeIndex++;
    }

    public TextNode getCurrentTextNode() {
      return currentTextNode;
    }

    public String getCurrentText() {
      return currentTextNode.getContent();
    }

    public int getCurrentTextLength() {
      return getCurrentText().length();
    }

    public int getTextNodeIndex() {
      return textNodeIndex;
    }

    public boolean canAdvance() {
      return currentTextNode.getNextTextNode() != null;
    }
  }

}
