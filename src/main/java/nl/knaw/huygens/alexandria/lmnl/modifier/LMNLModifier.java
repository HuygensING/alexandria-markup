package nl.knaw.huygens.alexandria.lmnl.modifier;

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;

import java.util.Collection;
import java.util.List;

public class LMNLModifier {

  Logger LOG = LoggerFactory.getLogger(LMNLModifier.class);

  private Limen limen;

  public LMNLModifier(Limen limen) {
    this.limen = limen;
  }

  public void addTextRange(TextRange newTextRange, Position position) {
    logTextNodes(limen.textNodeList);
    logTextRanges(limen.textRangeList);

    TextNodeCursor cursor = new TextNodeCursor(limen);
    int startOffset = position.getOffset();
    int endOffset = position.getOffset() + position.getLength();
    boolean goOn = true;
    int currentOffset = 0;
    String currentText = null;
    int currentTextNodeLength = 0;
    // get to starting TextNode
    int offsetAtEndOfCurrentTextNode = currentOffset + currentTextNodeLength;
//    logTextNodes(limen.textNodeList);
    while (goOn) {
      currentText = cursor.getCurrentText();
      currentTextNodeLength = cursor.getCurrentTextLength();
      offsetAtEndOfCurrentTextNode = currentOffset + currentTextNodeLength;
      if (startOffset <= offsetAtEndOfCurrentTextNode) {
        int tailLength = Math.min(offsetAtEndOfCurrentTextNode, offsetAtEndOfCurrentTextNode - startOffset);
        int headLength = currentTextNodeLength - tailLength;
        if (headLength > 0) {
          if (tailLength > 0) {
            // detach head
            String headText = currentText.substring(0, headLength);
            String tailText = currentText.substring(headLength);
            cursor.getCurrentTextNode().setContent(headText);
            TextNode newTailNode = new TextNode(tailText);
            TextNode nextTextNode = cursor.getCurrentTextNode().getNextTextNode();
            newTailNode.setPreviousTextNode(cursor.getCurrentTextNode());
            newTailNode.setNextTextNode(nextTextNode);
            if (nextTextNode != null) {
              nextTextNode.setPreviousTextNode(newTailNode);
            }
            cursor.getCurrentTextNode().setNextTextNode(newTailNode);
            limen.getTextRanges(cursor.getCurrentTextNode()).forEach(tr -> {
              tr.addTextNode(newTailNode);
              limen.associateTextWithRange(newTailNode, tr);
            });
            newTextRange.addTextNode(newTailNode);
            limen.associateTextWithRange(newTailNode, newTextRange);
            limen.textNodeList.add(cursor.getTextNodeIndex() + 1, newTailNode);
          } else {
            newTextRange.addTextNode(cursor.getCurrentTextNode());
            limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
          }
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

    logTextRanges(limen.textRangeList);
//    logTextNodes(limen.textNodeList);
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
        int headLength = cursor.getCurrentTextLength() - tailLength;

        if (tailLength > 0) {
          if (headLength > 0) {
            // detach head
            String headText = cursor.getCurrentText().substring(0, headLength);
            String tailText = cursor.getCurrentText().substring(headLength);
            cursor.getCurrentTextNode().setContent(tailText);
            TextNode newHeadNode = new TextNode(headText);
            TextNode previousTextNode = cursor.getCurrentTextNode().getPreviousTextNode();
            newHeadNode.setNextTextNode(cursor.getCurrentTextNode());
            newHeadNode.setPreviousTextNode(previousTextNode);
            if (previousTextNode != null) {
              previousTextNode.setNextTextNode(newHeadNode);
            }
            cursor.getCurrentTextNode().setPreviousTextNode(newHeadNode);
            limen.getTextRanges(cursor.getCurrentTextNode()).forEach(tr -> {
              limen.associateTextWithRange(newHeadNode, tr);
              tr.addTextNode(newHeadNode);
            });
            limen.associateTextWithRange(newHeadNode, newTextRange);
            limen.textNodeList.add(cursor.getTextNodeIndex(), newHeadNode);
            limen.disAssociateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
            newTextRange.addTextNode(newHeadNode);
          } else {
            limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
            newTextRange.addTextNode(cursor.getCurrentTextNode());
          }
        }
        goOn = false;
      }
    }
//    logTextNodes(limen.textNodeList);
    limen.textRangeList.add(newTextRange);
    logTextRanges(limen.textRangeList);
  }

  public void addTextRange(TextRange newTextRange, Collection<Position> positions) {
    if (!newTextRange.hasId()) {
      throw new RuntimeException("TextRange " + newTextRange.getTag() + " should have an id.");
    }
    positions.forEach(position -> {
      LOG.info("position={}", position);
      logTextNodes(limen.textNodeList);
      logTextRanges(limen.textRangeList);
      addTextRange(newTextRange, position);
    });
    logTextNodes(limen.textNodeList);
    logTextRanges(limen.textRangeList);
//    LMNLImporter.joinDiscontinuedRanges(limen);
  }

  private void logTextNodes(List<TextNode> list) {
    StringBuilder textnodes = new StringBuilder();
    list.forEach(tn -> {
      if (tn.getPreviousTextNode() != null) {
        textnodes.append("\"").append(tn.getPreviousTextNode().getContent()).append("\" -> ");
      }
      textnodes.append("[").append(tn.getContent()).append("]");
      if (tn.getNextTextNode() != null) {
        textnodes.append(" -> \"").append(tn.getNextTextNode().getContent()).append("\"");
      }
      textnodes.append("\n");
    });
    LOG.info("\nTextNodes:\n{}", textnodes);
  }

  private void logTextRanges(List<TextRange> list) {
    StringBuilder textranges = new StringBuilder();
    list.forEach(tr -> {
      textranges.append("[").append(tr.getTag()).append("}\n");
      tr.textNodes.forEach(tn -> textranges.append("  \"").append(tn.getContent()).append("\"\n"));
    });
    LOG.info("\nTextRanges:\n{}", textranges);
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
