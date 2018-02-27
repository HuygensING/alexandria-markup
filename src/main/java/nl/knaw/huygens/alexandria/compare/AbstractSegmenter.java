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

import static nl.knaw.huygens.alexandria.compare.Segment.Type.*;

abstract class AbstractSegmenter implements Segmenter {

  public List<Segment> calculateSegmentation(Score[][] editTable, List<TAGToken> tokensA, List<TAGToken> tokensB) {
    List<Segment> superWitness = new ArrayList<>();
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
      Boolean stateChange = lastCell.isMatch != currentCell.isMatch;
      if (stateChange) {
        addLastCellToSuperWitness(lastCell, tokensA, tokensB, x, y, superWitness);
        // change the pointer
        lastCell = editTable[y][x];
      }
    }
    // process the final cell in de EditGraphTable (additions/omissions at the beginning of the witnesses
    addLastCellToSuperWitness(lastCell, tokensA, tokensB, 0, 0, superWitness);
    // System.out.println(String.format("%d %d %d %d", lastX, lastY, 0, 0));
    return superWitness;
  }

  private void addLastCellToSuperWitness(Score lastCell,//
                                         List<TAGToken> tokensA, List<TAGToken> tokensB,//
                                         int currentX, int currentY,//
                                         List<Segment> superWitness) {
    int lastX = lastCell.x;
    int lastY = lastCell.y;
    List<TAGToken> segmentTokensA = tokensA.subList(currentX, lastX);
    List<TAGToken> segmentTokensB = tokensB.subList(currentY, lastY);

    if (lastCell.isMatch) {
      Segment segment = new Segment(segmentTokensA, segmentTokensB, aligned);
      superWitness.add(0, segment);
    } else {
      // if cell contains tokens from both witnesses its a replacement
      if (!segmentTokensA.isEmpty() && !segmentTokensB.isEmpty()) {
        Segment segment = new Segment(segmentTokensA, segmentTokensB, replacement);
        // insert the segment to the list at the first position (position "0")
        superWitness.add(0, segment);
      }
      // addition: no TokensA
      else if (segmentTokensA.isEmpty()) {
        Segment segment = new Segment(segmentTokensA, segmentTokensB, addition);
        superWitness.add(0, segment);
      }
      // omission: no TokensB
      else if (segmentTokensB.isEmpty()) {
        Segment segment = new Segment(segmentTokensA, segmentTokensB, omission);
        superWitness.add(0, segment);
      }
    }

  }

}
