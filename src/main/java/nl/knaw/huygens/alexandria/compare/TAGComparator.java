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
import static java.util.stream.Collectors.joining;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.ArrayList;
import java.util.List;

public class TAGComparator {
  private final List<String> diffLines = new ArrayList<>();

  public List<String> getDiffLines() {
    return diffLines;
  }

  public TAGComparator(DocumentWrapper originalDocument, TAGView tagView, DocumentWrapper otherDocument) {
    Scorer scorer = new ContentScorer();
    ViewAligner viewAligner = new ViewAligner(scorer);
    List<TAGToken> originalTokens = new Tokenizer(originalDocument, tagView).getTAGTokens();
    List<TAGToken> editedTokens = new Tokenizer(otherDocument, tagView).getTAGTokens();
    List<Segment> segments = viewAligner.align(originalTokens, editedTokens);
    if (segments.size() > 1) {
      for (Segment segment : segments) {
        switch (segment.type()) {
          case aligned:
            handleAligned(segment);
            break;

          case empty:
            handleEmpty(segment);
            break;

          case addition:
            handleAddition(segment);
            break;

          case omission:
            handleOmission(segment);
            break;

          case replacement:
            handleReplacement(segment);
            break;

          default:
            throw new RuntimeException("unexpected type:" + segment.type());
        }
      }
    }
  }


  private void handleOmission(Segment segment) {
    String omissionLine = toLine(segment.tokensA());
    diffLines.add("-" + omissionLine);
  }

  private void handleAddition(Segment segment) {
    String additionLine = toLine(segment.tokensB());
    diffLines.add("+" + additionLine);
  }

  private void handleReplacement(Segment segment) {
    handleOmission(segment);
    handleAddition(segment);
  }

  private void handleEmpty(Segment segment) {
  }

  private void handleAligned(Segment segment) {
    String alignedLine = toLine(segment.tokensA());
    diffLines.add(" " + alignedLine);
  }

  private String toLine(List<TAGToken> tagTokens) {
    return tagTokens.stream().map(TAGToken::toString).collect(joining(" "));
  }


}
