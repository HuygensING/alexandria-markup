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

import nl.knaw.huygens.alexandria.data_model.TextNode;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.*;

import static java.util.stream.Collectors.*;
import static nl.knaw.huygens.alexandria.compare.Segment.Type.aligned;

public class TAGComparison {
  private final List<Segment> segments;
  private final Map<TAGToken, List<TokenProvenance>> tokenProvenanceMap;
  private final DocumentWrapper originalDocument;
  private final Map<TAGToken, TextNodeWrapper> map = new HashMap<>();

  public TAGComparison(DocumentWrapper originalDocument, TAGView tagView, DocumentWrapper otherDocument) {
    this.originalDocument = originalDocument;
    Scorer scorer = new ContentScorer();
    Segmenter segmenter = new ContentSegmenter();
    Tokenizer originalTokenizer = new Tokenizer(originalDocument, tagView);
    tokenProvenanceMap = originalTokenizer.getTokenProvenanceMap();
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
            reportAligned(segment, diffLines);
            break;

//          case empty:
//            handleEmpty(segment, diffLines);
//            break;

          case addition:
            reportAddition(segment, diffLines);
            break;

          case omission:
            reportOmission(segment, diffLines);
            break;

          case replacement:
            reportReplacement(segment, diffLines);
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
        .map(this::getTokenProvenance)
        .flatMap(List::stream)
        .map(p -> ((TextTokenProvenance) p).getTextNodeWrapper())
        .collect(toSet());
  }

  private List<TokenProvenance> getTokenProvenance(final TAGToken tagToken) {
    return tokenProvenanceMap.get(tagToken);
  }

  public boolean hasDifferences() {
    return segments.stream().anyMatch(this::isNotAligned);
  }

  private void reportOmission(Segment segment, List<String> diffLines) {
    asLines(segment.tokensA()).forEach(l -> diffLines.add("-" + l));
  }

  private void reportAddition(Segment segment, List<String> diffLines) {
    asLines(segment.tokensB()).forEach(l -> diffLines.add("+" + l));
  }

  private void reportReplacement(Segment segment, List<String> diffLines) {
    reportOmission(segment, diffLines);
    reportAddition(segment, diffLines);
  }

//  private void handleEmpty(Segment segment, final List<String> diffLines) {
//  }

  private void reportAligned(Segment segment, final List<String> diffLines) {
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
    for (int i = 0; i < segments.size(); i++) {
      Segment segment = segments.get(i);
      if (isNotAligned(segment)) {
        switch (segment.type()) {
          case addition:
            handleAddition(i);
            break;

          case omission:
            handleOmission(i);
            break;

          case replacement:
            handleReplacement(i);
            break;

          default:
            throw new RuntimeException("unexpected type:" + segment.type());
        }
      }
    }
  }

  private boolean isNotAligned(final Segment s) {
    return !s.type().equals(aligned);
  }

  private void handleAddition(final int i) {
    if (i > 0) {
      Segment previousSegment = segments.get(i - 1);
      List<TAGToken> tagTokensA = previousSegment.tokensA();
      TAGToken previousToken = tagTokensA.get(tagTokensA.size() - 1);
      List<TokenProvenance> previousTokenProvenances = tokenProvenanceMap.get(previousToken);
      TokenProvenance previousTokenProvenance = previousTokenProvenances.get(previousTokenProvenances.size() - 1);

    }
    Segment segment = segments.get(i);
    if (i < segments.size() - 1) {
      Segment nextSegment = segments.get(i + 1);
      List<TAGToken> tagTokensA = nextSegment.tokensA();
      TAGToken nextToken = tagTokensA.get(0);
      TokenProvenance nextTokenProvenance = tokenProvenanceMap.get(nextToken).get(0);
    }
    final List<TAGToken> tokens2Add = segment.tokensB();
    final TAGStore store = originalDocument.getStore();
    for (TAGToken token : tokens2Add) {

    }

  }

  private void handleOmission(final int i) {
    Segment segment = segments.get(i);
    List<TAGToken> toRemove = segment.tokensA();
    List<TextNodeWrapper> relevantTextNodes = toRemove.stream().map(this::toTextNodeWrapper).distinct().collect(toList());
//    originalDocument.

  }

  private TextNodeWrapper toTextNodeWrapper(final TAGToken tagToken) {
    return map.get(tagToken);
  }

  private void handleReplacement(final int i) {
    Segment segment = segments.get(i);
    List<TAGToken> toRemove = segment.tokensA();
    List<TAGToken> toAdd = segment.tokensB();

  }

}
