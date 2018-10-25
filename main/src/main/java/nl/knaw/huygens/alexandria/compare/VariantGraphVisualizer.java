package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup
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

import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prioritised_xml_collation.*;

import java.util.List;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

public class VariantGraphVisualizer {
  Logger LOG = LoggerFactory.getLogger(VariantGraphVisualizer.class);

  private final DiffVisualizer visualizer;

  VariantGraphVisualizer(DiffVisualizer visualizer) {
    this.visualizer = visualizer;
  }

  public void visualizeVariation(
      final String witness1, TAGDocument document1,
      final String witness2, TAGDocument document2,
      TAGView tagView) {
    List<TAGToken> originalTokens = new Tokenizer(document1, tagView)
        .getTAGTokens();
    List<TAGToken> originalTextTokens = originalTokens.stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(Collectors.toList());
    LOG.info("originalTextTokens={}", serializeTokens(originalTextTokens));
    List<TAGToken> originalMarkupTokens = originalTokens.stream()
        .filter(this::isMarkupToken)
        .collect(Collectors.toList());
    LOG.info("originalMarkupTokens={}", serializeTokens(originalMarkupTokens));

    List<TAGToken> editedTokens = new Tokenizer(document2, tagView)
        .getTAGTokens();
    List<TAGToken> editedTextTokens = editedTokens
        .stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(Collectors.toList());
    LOG.info("editedTextTokens={}", serializeTokens(editedTextTokens));
    List<TAGToken> editedMarkupTokens = editedTokens.stream()
        .filter(this::isMarkupToken)
        .collect(Collectors.toList());
    LOG.info("editedMarkupTokens={}", serializeTokens(editedMarkupTokens));

    SegmenterInterface textSegmenter = new ProvenanceAwareSegmenter(originalTextTokens, editedTextTokens);
    List<Segment> textSegments = new TypeAndContentAligner().alignTokens(originalTextTokens, editedTextTokens, textSegmenter);

//    SegmenterInterface markupSegmenter = new ProvenanceAwareSegmenter(originalMarkupTokens, editedMarkupTokens);
//    List<Segment> markupSegments = new TypeAndContentAligner().alignTokens(originalMarkupTokens, editedMarkupTokens, markupSegmenter);

    visualizer.startVisualization();

    visualizer.startOriginal(witness1);
    document1.getTextNodeStream().forEach(visualizer::originalTextNode);
    visualizer.endOriginal();

    visualizer.startDiff(witness1, witness2);
    textSegments.forEach(segment -> {
      switch (segment.type) {
        case aligned:
          visualizer.startAligned();
          visualizer.alignedTextTokens(segment.tokensWa, segment.tokensWb);
          visualizer.endAligned();
          break;

        case addition:
          visualizer.startAddition();
          segment.tokensWb.forEach(visualizer::addedTextToken);
          visualizer.endAddition();
          break;

        case omission:
          visualizer.startOmission();
          segment.tokensWa.forEach(visualizer::omittedTextToken);
          visualizer.endOmission();
          break;

        case replacement:
          visualizer.startReplacement();
          segment.tokensWa.forEach(visualizer::originalTextToken);
          visualizer.replacementSeparator();
          segment.tokensWb.forEach(visualizer::editedTextToken);
          visualizer.endReplacement();
          break;

        default:
          throw new RuntimeException("unexpected type:" + segment.type);
      }
    });
    visualizer.endDiff();

    visualizer.startEdited(witness2);
    document2.getTextNodeStream().forEach(visualizer::editedTextNode);
    visualizer.endEdited();

    visualizer.endVisualization();
  }

  private boolean isMarkupToken(final TAGToken tagToken) {
    return tagToken instanceof MarkupOpenToken
        || tagToken instanceof MarkupCloseToken;
  }

  private String serializeTokens(final List<TAGToken> originalTextTokens) {
    return originalTextTokens.stream()
        .map(t -> "[" + t.toString().replaceAll(" ", "_") + "]")
        .collect(joining(", "));
  }

}
