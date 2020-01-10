package nl.knaw.huygens.alexandria.lmnl.modifier;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import nl.knaw.huygens.alexandria.data_model.Limen;
import nl.knaw.huygens.alexandria.data_model.Markup;
import nl.knaw.huygens.alexandria.data_model.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

class LMNLModifier {
  private final Logger LOG = LoggerFactory.getLogger(LMNLModifier.class);

  private final Limen limen;

  public LMNLModifier(Limen limen) {
    this.limen = limen;
  }

  public void addMarkup(Markup newMarkup, Position position) {
    // logTextNodes(limen.textNodeList);
    // logMarkups(limen.markupList);
    TextNodeCursor cursor = new TextNodeCursor(limen);
    int startOffset = position.getOffset();
    int endOffset = position.getOffset() + position.getLength();
    // get to starting TextNode
    // logTextNodes(limen.textNodeList);
    boolean findEndingTextNode = handleStartingTextNode(newMarkup, cursor, startOffset);
    // logMarkups(limen.markupList);
    // logTextNodes(limen.textNodeList);
    if (findEndingTextNode) {
      handleEndingTextNode(newMarkup, cursor, endOffset);
    }
    // logTextNodes(limen.textNodeList);
    // limen.markupList.add(newMarkup);
    // logMarkups(limen.markupList);
  }

  private boolean handleStartingTextNode(Markup newMarkup, TextNodeCursor cursor, int startOffset) {
    boolean findStartingTextNode = true;
    boolean findEndingTextNode = true;
    while (findStartingTextNode) {
      String currentText = cursor.getCurrentText();
      int currentTextNodeLength = cursor.getCurrentTextLength();
      int offsetAtEndOfCurrentTextNode = cursor.getOffsetAtEndOfCurrentTextNode();
      if (startOffset < offsetAtEndOfCurrentTextNode) {
        // newMarkup starts in this TextNode
        int tailLength = Math.min(offsetAtEndOfCurrentTextNode, offsetAtEndOfCurrentTextNode - startOffset);
        int headLength = currentTextNodeLength - tailLength;
        if (headLength == 0) {
          // newMarkup exactly covers current TextNode
          newMarkup.addTextNode(cursor.getCurrentTextNode());
          limen.associateTextWithRange(cursor.getCurrentTextNode(), newMarkup);
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
            limen.getMarkups(cursor.getCurrentTextNode()).forEach(tr -> {
              tr.addTextNode(newTailNode);
              limen.associateTextWithRange(newTailNode, tr);
            });
            newMarkup.addTextNode(newTailNode);
            limen.associateTextWithRange(newTailNode, newMarkup);
            limen.textNodeList.add(cursor.getTextNodeIndex() + 1, newTailNode);

          } else {
            // newMarkup.addTextNode(cursor.getCurrentTextNode());
            // limen.associateTextNodeWithMarkupForLayer(cursor.getCurrentTextNode(), newMarkup);
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

  private void handleEndingTextNode(Markup newMarkup, TextNodeCursor cursor, int endOffset) {
    boolean findEndingTextNode = true;
    while (findEndingTextNode) {
      int offsetAtEndOfCurrentTextNode = cursor.getOffsetAtEndOfCurrentTextNode();
      if (offsetAtEndOfCurrentTextNode < endOffset) {
        // this is not the TextNode where newMarkup ends, but it is part of newMarkup
        limen.associateTextWithRange(cursor.getCurrentTextNode(), newMarkup);
        findEndingTextNode = cursor.canAdvance();
        cursor.advance();

      } else {
        // this is the TextNode where newMarkup ends
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
            limen.getMarkups(cursor.getCurrentTextNode())//
                .stream()//
                .filter(tr -> !newMarkup.equals(tr))//
                .forEach(tr -> {
                  limen.associateTextWithRange(newTailNode, tr);
                  tr.addTextNode(newTailNode);
                });
            limen.textNodeList.add(cursor.getTextNodeIndex() + 1, newTailNode);

          } else {
            // limen.associateTextNodeWithMarkupForLayer(cursor.getCurrentTextNode(), newMarkup);
            // newMarkup.addTextNode(cursor.getCurrentTextNode());
            throw new RuntimeException("head=empty!");
          }
        }
        findEndingTextNode = false;
      }
    }
  }

  public void addMarkup(Markup newMarkup, Collection<Position> positions) {
    if (!newMarkup.hasId()) {
      throw new RuntimeException("Markup " + newMarkup.getTag() + " should have an id.");
    }
    positions.forEach(position -> {
      LOG.debug("position={}", position);
      logTextNodes(limen.textNodeList);
      logMarkups(limen.markupList);
      addMarkup(newMarkup, position);
    });

    logTextNodes(limen.textNodeList);
    logMarkups(limen.markupList);
    // LMNLImporter.joinDiscontinuedRanges(limen);
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
    LOG.debug("\nTextNodes:\n{}", textnodes);
  }

  private void logMarkups(List<Markup> list) {
    StringBuilder markups = new StringBuilder();
    list.forEach(tr -> {
      markups.append("[").append(tr.getTag()).append("}\n");
      tr.textNodes.forEach(tn -> markups.append("  \"").append(tn.getContent()).append("\"\n"));
    });
    LOG.debug("\nMarkups:\n{}", markups);
  }

  class TextNodeCursor {
    private TextNode currentTextNode;
    private int textNodeIndex = 0;
    private int offset = 0;

    TextNodeCursor(Limen limen) {
      currentTextNode = limen.textNodeList.get(0);
    }

    void advance() {
      offset += getCurrentTextLength();
      currentTextNode = currentTextNode.getNextTextNode();
      textNodeIndex++;
    }

    TextNode getCurrentTextNode() {
      return currentTextNode;
    }

    String getCurrentText() {
      return currentTextNode.getContent();
    }

    int getCurrentTextLength() {
      return getCurrentText().length();
    }

    int getTextNodeIndex() {
      return textNodeIndex;
    }

    boolean canAdvance() {
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
