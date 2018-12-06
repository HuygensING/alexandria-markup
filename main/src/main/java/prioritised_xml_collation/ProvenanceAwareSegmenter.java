package prioritised_xml_collation;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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
import nl.knaw.huygens.alexandria.compare.ExtendedTextToken;

import java.util.ArrayList;
import java.util.List;

// based on AlignmentAndTypeSegmenter
public class ProvenanceAwareSegmenter implements SegmenterInterface {
  private final List<TAGToken> tokensA;
  private final List<TAGToken> tokensB;

  public ProvenanceAwareSegmenter(final List<TAGToken> tokensA, final List<TAGToken> tokensB) {
    // since tokensA and tokensB are private in editGraphTable, set them in this constructor
    this.tokensA = tokensA;
    this.tokensB = tokensB;
  }

  public List<Segment> calculateSegmentation(EditGraphTable editTable) {
    ArrayList<Segment> superwitness = new ArrayList<>();
    // We set last cell to the first iterable cell. (lower right corner)
    Cell lastCell = editTable.iterator().next();
    Cell currentCell = null;
    // CellIterator iterates cells:
    // As long as the pointer can move up in the editTable
    for (final Cell anEditTable : editTable) {
      // move one cell up
      currentCell = anEditTable;
      // stateChange if the type of the lastCell is not the same as the currentCell
      Boolean stateChange = lastCell.match != currentCell.match;

      // provenanceChange if the a or b tokens from lastcell have any different textNodeIds compared with the currentCell
      boolean provenanceChangeInA = provenanceChanged(tokensA, currentCell.x, lastCell.x);
      boolean provenanceChangeInB = provenanceChanged(tokensB, currentCell.y, lastCell.y);

      Boolean provenanceChange = provenanceChangeInA || provenanceChangeInB;

      if (stateChange || provenanceChange) {
        // insert the segment to the superwitness list at the first position (position "0")
        superwitness.add(0, editTable.createSegmentOfCells(currentCell, lastCell));
        // change the pointer
        lastCell = currentCell;
      }
    }
    // process the final cell in de EditGraphTable (additions/omissions at the beginning of the witnesses)
    if (lastCell != currentCell) {
      // insert the segment to the superwitness list at the first position (position "0")
      superwitness.add(0, editTable.createSegmentOfCells(currentCell, lastCell));
    }
    return superwitness;
  }

  private boolean provenanceChanged(final List<TAGToken> tokens, final int x0, int x1) {
    if (x0 < 1 || x1 < 1) {
      return true;
    }
    ExtendedTextToken tagToken0 = (ExtendedTextToken) tokens.get(x0 - 1);
    ExtendedTextToken tagToken1 = (ExtendedTextToken) tokens.get(x1 - 1);
    return !(tagToken0.getTextNodeIds().equals(tagToken1.getTextNodeIds()));
  }

}
