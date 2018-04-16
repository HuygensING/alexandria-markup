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

import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import prioritised_xml_collation.Segment;
import prioritised_xml_collation.TAGToken;
import prioritised_xml_collation.TypeAndContentAligner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.joining;

public class TAGComparison {
  private final List<String> diffLines = new ArrayList<>();

  public TAGComparison(DocumentWrapper originalDocument, TAGView tagView, DocumentWrapper otherDocument) {
    List<TAGToken> originalTokens = new Tokenizer(originalDocument, tagView).getTAGTokens();
    List<TAGToken> editedTokens = new Tokenizer(otherDocument, tagView).getTAGTokens();
    List<Segment> segments = new TypeAndContentAligner().alignTokens(originalTokens, editedTokens);
    if (segments.size() > 1) {
      for (Segment segment : segments) {
        switch (segment.type) {
          case aligned:
            handleAligned(segment);
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
            throw new RuntimeException("unexpected type:" + segment.type);
        }
      }
    }
  }

  public List<String> getDiffLines() {
    return diffLines;
  }

  public boolean hasDifferences() {
    return !diffLines.isEmpty();
  }

  private void handleOmission(Segment segment) {
    asLines(segment.tokensWa).forEach(l -> diffLines.add("-" + l));
  }

  private void handleAddition(Segment segment) {
    asLines(segment.tokensWb).forEach(l -> diffLines.add("+" + l));
  }

  private void handleReplacement(Segment segment) {
    handleOmission(segment);
    handleAddition(segment);
  }

  private void handleAligned(Segment segment) {
    List<String> lines = asLines(segment.tokensWa);
    diffLines.add(" " + lines.get(0));
    if (lines.size() > 2) {
      diffLines.add(" ...");
    }
    if (lines.size() > 1) {
      diffLines.add(" " + lines.get(lines.size() - 1));
    }
  }

  private List<String> asLines(List<TAGToken> tagTokens) {
    return Arrays.asList(tagTokens.stream()//
        .map(TAGToken::toString)//
        .collect(joining(""))//
        .split("\n"));
  }


}
