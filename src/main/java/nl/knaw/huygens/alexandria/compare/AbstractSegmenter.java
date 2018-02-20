package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import java.util.ArrayList;
import java.util.List;

import static nl.knaw.huygens.alexandria.compare.Score.Type.*;

abstract class AbstractSegmenter implements Segmenter {
  private List<Segment> superwitness;

  public List<Segment> calculateSegmentation(List<TAGToken> tokensA, List<TAGToken> tokensB, Score[][] editTable) {
    this.superwitness = new ArrayList<>();
    // ScoreIterator iterates cells:
    ScoreIterator iterateTable = new ScoreIterator(editTable);
    // pointer is set in lower right corner at "lastCell"
    int lastY = editTable.length - 1;
    int lastX = editTable[0].length - 1;
    Score lastCell = editTable[lastY][lastX];
    // As long as the pointer can move up in the editTable
    while (iterateTable.hasNext()) {
      // move one cell up
      Score currentCell = iterateTable.next();
      int x = currentCell.x;
      int y = currentCell.y;
      // stateChange if the type of the lastCell is not the same as the currentCell
      Boolean stateChange = lastCell.match != currentCell.match;
      if (stateChange) {
//        System.out.println(lastCell.match + ", " + currentCell.match);
        addCellToSuperWitness(currentCell, tokensA, tokensB, lastX, lastY);
        // System.out.println(String.format("%d %d %d %d", lastX, lastY, x, y));
        // change the pointer
        lastY = y;
        lastX = x;
        lastCell = editTable[lastY][lastX];
      }
    }
    // process the final cell in de EditGraphTable (additions/omissions at the beginning of the witnesses
    Score currentCell = editTable[0][0];
    addCellToSuperWitness(currentCell, tokensA, tokensB, lastX, lastY);
    // System.out.println(String.format("%d %d %d %d", lastX, lastY, 0, 0));
    return superwitness;
  }

  private void addCellToSuperWitness(Score currentCell, List<TAGToken> tokensA, List<TAGToken> tokensB, int lastX, int lastY) {
    int x = currentCell.x;
    int y = currentCell.y;
    List<TAGToken> segmentTokensA = tokensA.subList(x, lastX);
    List<TAGToken> segmentTokensB = tokensB.subList(y, lastY);

    // if currentCell has tokens of type "match", lastcell is replacement (because stateChange)
    if (currentCell.match) {
      // if cell contains tokens from both witnesses its a replacement
      if (!segmentTokensA.isEmpty() && !segmentTokensB.isEmpty()) {
        Segment segment = new Segment(segmentTokensA, segmentTokensB, replacement);
        // insert the segment to the list at the first position (position "0")
        superwitness.add(0, segment);
      }
      // addition: no TokensA
      else if (segmentTokensA.isEmpty()) {
        Segment segment = new Segment(segmentTokensA, segmentTokensB, addition);
        superwitness.add(0, segment);
      }
      // omission: no TokensB
      else if (segmentTokensB.isEmpty()) {
        Segment segment = new Segment(segmentTokensA, segmentTokensB, omission);
        superwitness.add(0, segment);
      }
    }
    // aligned
    else {
      Segment segment = new Segment(segmentTokensA, segmentTokensB, aligned);
      superwitness.add(0, segment);
    }
  }

}
