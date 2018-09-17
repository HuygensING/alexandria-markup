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

public class VariantGraphVisualizer {

  public String visualizeVariation(TAGDocument document1, TAGDocument document2, TAGView tagView) {
    List<TAGToken> originalTokens = new Tokenizer(document1, tagView).getTAGTokens();
    List<TAGToken> editedTokens = new Tokenizer(document2, tagView).getTAGTokens();
    SegmenterInterface segmenter = new AlignedNonAlignedSegmenter();
    List<Segment> segments = new TypeAndContentAligner().alignTokens(originalTokens, editedTokens, segmenter);

    StringBuilder dotBuilder = new StringBuilder();

    return dotBuilder.toString();
  }
}
