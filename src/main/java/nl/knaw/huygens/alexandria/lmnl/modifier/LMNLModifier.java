package nl.knaw.huygens.alexandria.lmnl.modifier;

import java.util.Collection;
import java.util.List;

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
    // logTextNodes(limen.textNodeList);
    // logTextRanges(limen.textRangeList);
    TextNodeCursor cursor = new TextNodeCursor(limen);
    int startOffset = position.getOffset();
    int endOffset = position.getOffset() + position.getLength();
    // get to starting TextNode
    // logTextNodes(limen.textNodeList);
    boolean findEndingTextNode = handleStartingTextNode(newTextRange, cursor, startOffset);
    // logTextRanges(limen.textRangeList);
    // logTextNodes(limen.textNodeList);
    if (findEndingTextNode) {
      handleEndingTextNode(newTextRange, cursor, endOffset);
    }
    // logTextNodes(limen.textNodeList);
    // limen.textRangeList.add(newTextRange);
    // logTextRanges(limen.textRangeList);
  }

  private boolean handleStartingTextNode(TextRange newTextRange, TextNodeCursor cursor, int startOffset) {
    boolean findStartingTextNode = true;
    boolean findEndingTextNode = true;
    while (findStartingTextNode) {
      String currentText = cursor.getCurrentText();
      int currentTextNodeLength = cursor.getCurrentTextLength();
      int offsetAtEndOfCurrentTextNode = cursor.getOffsetAtEndOfCurrentTextNode();
      if (startOffset < offsetAtEndOfCurrentTextNode) {
        // newTextRange starts in this TextNode
        int tailLength = Math.min(offsetAtEndOfCurrentTextNode, offsetAtEndOfCurrentTextNode - startOffset);
        int headLength = currentTextNodeLength - tailLength;
        if (headLength == 0) {
          // newTextRange exactly covers current TextNode
          newTextRange.addTextNode(cursor.getCurrentTextNode());
          limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
          findEndingTextNode = false;
        } else {
          if (tailLength > 0) {
            // detach tail
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
            // newTextRange.addTextNode(cursor.getCurrentTextNode());
            // limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
            throw new RuntimeException("tail=empty!");
          }
        }
        findStartingTextNode = false;

      } else {
        findStartingTextNode = cursor.canAdvance();
      }
      cursor.advance();
    }
    return findEndingTextNode;
  }

  private void handleEndingTextNode(TextRange newTextRange, TextNodeCursor cursor, int endOffset) {
    boolean findEndingTextNode = true;
    while (findEndingTextNode) {
      int offsetAtEndOfCurrentTextNode = cursor.getOffsetAtEndOfCurrentTextNode();
      if (offsetAtEndOfCurrentTextNode < endOffset) {
        // this is not the TextNode where newTextRange ends, but it is part of newTextRange
        limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
        findEndingTextNode = cursor.canAdvance();
        cursor.advance();

      } else {
        // this is the TextNode where newTextRange ends
        int tailLength = offsetAtEndOfCurrentTextNode - endOffset;
        int headLength = cursor.getCurrentTextLength() - tailLength;

        if (tailLength > 0) {
          if (headLength > 0) {
            // detach tail
            String headText = cursor.getCurrentText().substring(0, headLength);
            String tailText = cursor.getCurrentText().substring(headLength);
            cursor.getCurrentTextNode().setContent(headText);
            TextNode newTailNode = new TextNode(tailText);
            TextNode nextTextNode = cursor.getCurrentTextNode().getNextTextNode();
            newTailNode.setNextTextNode(nextTextNode);
            newTailNode.setPreviousTextNode(cursor.getCurrentTextNode());
            cursor.getCurrentTextNode().setNextTextNode(newTailNode);
            limen.getTextRanges(cursor.getCurrentTextNode())//
                .stream()//
                .filter(tr -> !newTextRange.equals(tr))//
                .forEach(tr -> {
                  limen.associateTextWithRange(newTailNode, tr);
                  tr.addTextNode(newTailNode);
                });
            limen.textNodeList.add(cursor.getTextNodeIndex() + 1, newTailNode);

          } else {
            // limen.associateTextWithRange(cursor.getCurrentTextNode(), newTextRange);
            // newTextRange.addTextNode(cursor.getCurrentTextNode());
            throw new RuntimeException("head=empty!");
          }
        }
        findEndingTextNode = false;
      }
    }
  }

  public void addTextRange(TextRange newTextRange, Collection<Position> positions) {
    if (!newTextRange.hasId()) {
      throw new RuntimeException("TextRange " + newTextRange.getTag() + " should have an id.");
    }
    positions.forEach(

        position -> {
          LOG.info("position={}", position);
          logTextNodes(limen.textNodeList);
          logTextRanges(limen.textRangeList);
          addTextRange(newTextRange, position);
        });

    logTextNodes(limen.textNodeList);
    logTextRanges(limen.textRangeList);
    // LMNLImporter.joinDiscontinuedRanges(limen);
  }

  private void logTextNodes(List<TextNode> list) {
    StringBuilder textnodes = new StringBuilder();
    list.forEach(

        tn -> {
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
    list.forEach(

        tr -> {
          textranges.append("[").append(tr.getTag()).append("}\n");
          tr.textNodes.forEach(tn -> textranges.append("  \"").append(tn.getContent()).append("\"\n"));
        });
    LOG.info("\nTextRanges:\n{}", textranges);
  }

  class TextNodeCursor {
    private TextNode currentTextNode;
    private int textNodeIndex = 0;
    private int offset = 0;

    public TextNodeCursor(Limen limen) {
      currentTextNode = limen.textNodeList.get(0);
    }

    public void advance() {
      offset += getCurrentTextLength();
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

    public int getOffset() {
      return offset;
    }

    private int getOffsetAtEndOfCurrentTextNode() {
      return offset + getCurrentTextLength();
    }
  }

}
