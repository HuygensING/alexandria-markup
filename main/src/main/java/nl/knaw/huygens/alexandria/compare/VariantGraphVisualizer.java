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
import prioritised_xml_collation.*;

import java.util.List;
import java.util.stream.Collectors;

public class VariantGraphVisualizer {

  private DiffVisualizer visualizer;

  VariantGraphVisualizer(DiffVisualizer visualizer) {
    this.visualizer = visualizer;
  }

  public void visualizeVariation(TAGDocument document1, TAGDocument document2, TAGView tagView) {
    List<TAGToken> originalTextTokens = new Tokenizer(document1, tagView)
        .getTAGTokens()
        .stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(Collectors.toList());
    List<TAGToken> editedTextTokens = new Tokenizer(document2, tagView)
        .getTAGTokens()
        .stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(Collectors.toList());
    SegmenterInterface segmenter = new AlignedNonAlignedSegmenter();
    List<Segment> segments = new TypeAndContentAligner().alignTokens(originalTextTokens, editedTextTokens, segmenter);

    visualizer.startVisualization();

    visualizer.startOriginal();
    document1.getTextNodeStream().forEach(visualizer::originalTextNode);
    visualizer.endOriginal();

    visualizer.startDiff();
    segments.forEach(segment -> {
      switch (segment.type) {
        case aligned:
          visualizer.startAligned();
          segment.tokensWa.forEach(visualizer::alignedTextToken);
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

    visualizer.startEdited();
    document2.getTextNodeStream().forEach(visualizer::editedTextNode);
    visualizer.endEdited();

    visualizer.endVisualization();
  }

}
