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
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.*;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;
import static nl.knaw.huygens.alexandria.compare.Segment.Type.aligned;

public class TAGComparison {
  private final List<Segment> segments;
  private final Map<TAGToken, List<TextNodeWrapper>> tokenToNodeMap;
  private DocumentWrapper originalDocument;

  public TAGComparison(DocumentWrapper originalDocument, TAGView tagView, DocumentWrapper otherDocument) {
    this.originalDocument = originalDocument;
    Scorer scorer = new ContentScorer();
    Segmenter segmenter = new ContentSegmenter();
    Tokenizer originalTokenizer = new Tokenizer(originalDocument, tagView);
    tokenToNodeMap = originalTokenizer.getTokenToNodeMap();
    List<TAGToken> originalTokens = originalTokenizer.getTAGTokens();
    List<TAGToken> editedTokens = new Tokenizer(otherDocument, tagView).getTAGTokens();
    segments = new ViewAligner(scorer, segmenter).align(originalTokens, editedTokens);
  }

  public List<Segment> getSegments() {
    return segments;
  }

  public List<String> getDiffLines() {
    final List<String> diffLines = new ArrayList<>();
    if (segments.size() > 1) {
      for (Segment segment : segments) {
        switch (segment.type()) {
          case aligned:
            handleAligned(segment, diffLines);
            break;

//          case empty:
//            handleEmpty(segment, diffLines);
//            break;

          case addition:
            handleAddition(segment, diffLines);
            break;

          case omission:
            handleOmission(segment, diffLines);
            break;

          case replacement:
            handleReplacement(segment, diffLines);
            break;

          default:
            throw new RuntimeException("unexpected type:" + segment.type());
        }
      }
    }
    return diffLines;
  }

  public Set<TextNodeWrapper> getTextNodeWrappersForSegment(Segment segment) {
    return segment.tokensA().stream()
        .map(this::getTextNodeWrappersForToken)
        .flatMap(List::stream)
        .collect(toSet());
  }

  private List<TextNodeWrapper> getTextNodeWrappersForToken(final TAGToken tagToken) {
    return tokenToNodeMap.get(tagToken);
  }

  public boolean hasDifferences() {
    return segments.stream().anyMatch(this::isNotAligned);
  }

  private void handleOmission(Segment segment, List<String> diffLines) {
    asLines(segment.tokensA()).forEach(l -> diffLines.add("-" + l));
  }

  private void handleAddition(Segment segment, List<String> diffLines) {
    asLines(segment.tokensB()).forEach(l -> diffLines.add("+" + l));
  }

  private void handleReplacement(Segment segment, List<String> diffLines) {
    handleOmission(segment, diffLines);
    handleAddition(segment, diffLines);
  }

//  private void handleEmpty(Segment segment, final List<String> diffLines) {
//  }

  private void handleAligned(Segment segment, final List<String> diffLines) {
    List<String> lines = asLines(segment.tokensA());
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

  public void mergeChanges() {
    segments.stream()
        .filter(this::isNotAligned)
        .forEach(s ->
            getTextNodeWrappersForSegment(s)
                .forEach(tn -> handleChanges(s, tn))
        );
  }

  private boolean isNotAligned(final Segment s) {
    return !s.type().equals(aligned);
  }

  private void handleChanges(final Segment segment, final TextNodeWrapper textNodeWrapper) {
    switch (segment.type()) {
//      case empty:
//        handleEmpty(segment, textNodeWrapper);
//        break;

      case addition:
        handleAddition(segment, textNodeWrapper);
        break;

      case omission:
        handleOmission(segment, textNodeWrapper);
        break;

      case replacement:
        handleReplacement(segment, textNodeWrapper);
        break;

      default:
        throw new RuntimeException("unexpected type:" + segment.type());
    }

  }

//  private void handleEmpty(final Segment segment, final TextNodeWrapper textNodeWrapper) {
//
//  }

  private void handleAddition(final Segment segment, final TextNodeWrapper textNodeWrapper) {

  }

  private void handleOmission(final Segment segment, final TextNodeWrapper textNodeWrapper) {

  }

  private void handleReplacement(final Segment segment, final TextNodeWrapper textNodeWrapper) {

  }

}
