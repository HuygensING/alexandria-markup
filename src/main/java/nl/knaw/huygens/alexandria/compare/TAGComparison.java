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

  public TAGComparison(DocumentWrapper originalDocument, TAGView tagView, DocumentWrapper otherDocument) {
    this.originalDocument = originalDocument;
    Scorer scorer = new ContentScorer();
    Segmenter segmenter = new ContentSegmenter();
    Tokenizer originalTokenizer = new Tokenizer(originalDocument, tagView);
    tokenProvenanceMap = originalTokenizer.getTokenProvenanceMap();
    List<TAGToken> originalTokens = originalTokenizer.getTAGTokens();
    List<TAGToken> editedTokens = new Tokenizer(otherDocument, null).getTAGTokens();
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

  private boolean isNotAligned(final Segment s) {
    return !s.type().equals(aligned);
  }

  private void handleAddition(final int i) {
    Segment segment = segments.get(i);
    List<TextNodeInfo> textNodeInfos = SegmentTool.computeTextNodeInfo(segment);

    for (TextNodeInfo textNodeInfo : textNodeInfos) {
      TextNode newTextNode = new TextNode(textNodeInfo.getText());
//      originalDocument.
//      textNodeInfo.getMarkupInfoList()
//          .stream()
//          .filter(MarkupInfo::isClosed)
//          .forEach(mi -> {
//            TAGMarkup m = new TAGMarkup(originalDocument.getDocument(), mi.getTag());
//            originalDocument.addMarkup(m);
//            originalDocument.associateTextNodeWithMarkup(newTextNode);
//          });

    }

    Optional<TokenProvenance> previousTokenProvenance = getPreviousTokenProvenance(i);
    Optional<TokenProvenance> nextTokenProvenance = getNextTokenProvenance(i);

    final List<TAGToken> tokens2Add = segment.tokensB();
//      final TAGStore store = originalDocument.getStore();
    StringBuilder stringToAdd = new StringBuilder();
    for (TAGToken token : tokens2Add) {
      if (token instanceof TextToken) {
        TextToken textToken = (TextToken) token;
        stringToAdd.append(textToken.content);

      } else if (token instanceof MarkupOpenToken) {
        // TODO

      } else if (token instanceof MarkupCloseToken) {
        // TODO

      } else {
        throw new RuntimeException("Unknown token type: " + token.getClass());
      }
    }

    if (previousTokenProvenance.isPresent() && previousTokenProvenance.get() instanceof TextTokenProvenance) {
      TextTokenProvenance previousTextTokenProvenance = (TextTokenProvenance) previousTokenProvenance.get();
      if (nextTokenProvenance.isPresent() && nextTokenProvenance.get() instanceof TextTokenProvenance) {
        TextTokenProvenance nextTextTokenProvenance = (TextTokenProvenance) nextTokenProvenance.get();
        TextNodeWrapper previousTextNodeWrapper = previousTextTokenProvenance.getTextNodeWrapper();
        if (previousTextNodeWrapper.equals(nextTextTokenProvenance.getTextNodeWrapper())) {
          String originalText = previousTextNodeWrapper.getText();
          int nextOffset = nextTextTokenProvenance.getOffset();
          String head = originalText.substring(0, nextOffset);
          String tail = originalText.substring(nextOffset);
          String mergedText = head + stringToAdd + tail;
          previousTextNodeWrapper.setText(mergedText);
          previousTextNodeWrapper.update();
        }
      } else {
        TextNodeWrapper previousTextNodeWrapper = previousTextTokenProvenance.getTextNodeWrapper();
        String originalText = previousTextNodeWrapper.getText();
        previousTextNodeWrapper.setText(originalText + " " + stringToAdd);
        previousTextNodeWrapper.update();
      }

    } else if (nextTokenProvenance.isPresent()) {
      if (nextTokenProvenance.get() instanceof TextTokenProvenance) {
        TextTokenProvenance nextTextTokenProvenance = (TextTokenProvenance) nextTokenProvenance.get();
        TextNodeWrapper nextTextNodeWrapper = nextTextTokenProvenance.getTextNodeWrapper();
        String originalText = nextTextNodeWrapper.getText();
        nextTextNodeWrapper.setText(stringToAdd + originalText);
        nextTextNodeWrapper.update();

      } else {
        // prepend new TextNode,
        TAGTextNode textNode = new TAGTextNode(stringToAdd.toString());
        Segment nextSegment = segments.get(i + 1);
        Optional<TAGToken> firstTextToken = nextSegment.tokensA()
            .stream()
            .filter(TextToken.class::isInstance)
            .findFirst();
        TAGTextNode nextTextNode = ((TextTokenProvenance) tokenProvenanceMap.get(firstTextToken.get()).get(0)).getTextNodeWrapper().getTextNode();
        originalDocument.insertTextNodeBefore(textNode, nextTextNode);
      }

    } else {
      TAGTextNode textNode = new TAGTextNode(stringToAdd.toString());
      Segment prevSegment = segments.get(i - 1);
      List<TAGToken> textTokens = prevSegment.tokensA()
          .stream()
          .filter(TextToken.class::isInstance)
          .collect(toList());
      TAGToken lastTextToken = textTokens.get(textTokens.size() - 1);
      List<TokenProvenance> tokenProvenances = tokenProvenanceMap.get(lastTextToken);
      TextTokenProvenance textTokenProvenance = (TextTokenProvenance) tokenProvenances.get(tokenProvenances.size() - 1);
      TAGTextNode prevTextNode = textTokenProvenance.getTextNodeWrapper().getTextNode();
      originalDocument.insertTextNodeAfter(textNode, prevTextNode);
    }
  }

  private Optional<TokenProvenance> getNextTokenProvenance(int i) {
    TokenProvenance nextTokenProvenance = null;
    if (i < segments.size() - 1) {
      Segment nextSegment = segments.get(i + 1);
      List<TAGToken> tagTokensA1 = nextSegment.tokensA();
      TAGToken nextToken = tagTokensA1.get(0);
      nextTokenProvenance = tokenProvenanceMap.get(nextToken).get(0);
    }
    return Optional.ofNullable(nextTokenProvenance);
  }

  private Optional<TokenProvenance> getPreviousTokenProvenance(int i) {
    TokenProvenance previousTokenProvenance = null;
    if (i > 0) {
      Segment previousSegment = segments.get(i - 1);
      List<TAGToken> tagTokensA = previousSegment.tokensA();
      TAGToken previousToken = tagTokensA.get(tagTokensA.size() - 1);
      List<TokenProvenance> previousTokenProvenances = tokenProvenanceMap.get(previousToken);
      previousTokenProvenance = previousTokenProvenances.get(previousTokenProvenances.size() - 1);
    }
    return Optional.ofNullable(previousTokenProvenance);
  }

  private void handleOmission(final Segment segment) {
    segment.tokensA()
        .stream()
        .filter(TextToken.class::isInstance)
        .forEach(token -> tokenProvenanceMap.get(token)
            .forEach(tokenProvenance -> {
              TextTokenProvenance textTokenProvenance = (TextTokenProvenance) tokenProvenance;
              TextNodeWrapper textNodeWrapper = textTokenProvenance.getTextNodeWrapper();
              String originalText = textNodeWrapper.getText();
              String head = originalText.substring(0, textTokenProvenance.getOffset());
              String tail = originalText.substring(textTokenProvenance.getOffset() + token.content.length());
              String newText = head + tail;
              textNodeWrapper.setText(newText);
              textNodeWrapper.update();
            }));
//    originalDocument.
  }

  private void handleReplacement(final Segment segment) {
    List<TAGToken> toRemove = segment.tokensA().stream()
        .filter(TextToken.class::isInstance)
        .collect(toList());

    List<TAGToken> toAdd = segment.tokensB().stream()
        .filter(TextToken.class::isInstance)
        .collect(toList());
    if (toRemove.size() == 1 && tokenProvenanceMap.get(toRemove.get(0)).size() == 1) {
      TAGToken token = toRemove.get(0);
      TextTokenProvenance textTokenProvenance = (TextTokenProvenance) tokenProvenanceMap.get(toRemove.get(0)).get(0);
      TextNodeWrapper textNodeWrapper = textTokenProvenance.getTextNodeWrapper();
      String originalText = textNodeWrapper.getText();
      String head = originalText.substring(0, textTokenProvenance.getOffset());
      String tail = originalText.substring(textTokenProvenance.getOffset() + token.content.length());
      String replace = toAdd.stream().map(t -> t.content).collect(joining());
      String newText = head + replace + tail;
      textNodeWrapper.setText(newText);
      textNodeWrapper.update();

    } else {
      throw new RuntimeException("TODO: unhandled situation!");
    }
  }

}
