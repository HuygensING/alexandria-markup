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

import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import static nl.knaw.huygens.alexandria.compare.Score.Type.aligned;
import static nl.knaw.huygens.alexandria.compare.Score.Type.replacement;
import static nl.knaw.huygens.alexandria.compare.SegmentMatcher.sM;
import static nl.knaw.huygens.alexandria.compare.TAGTokenMatcher.*;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ViewAlignerTest extends AlexandriaBaseStoreTest {

  @Test
  public void testSegmentReplaced() {
    store.runInTransaction(() -> {
      LMNLImporter importer = new LMNLImporter(store);
      DocumentWrapper document1 = importer.importLMNL("[TEI}[s}a{s]{TEI]");
      DocumentWrapper document2 = importer.importLMNL("[TEI}[s}c{s]{TEI]");
      Set<String> tei = new HashSet<>(Collections.singletonList("TEI"));
      TAGView ignoreTEI = new TAGView(store).setMarkupToExclude(tei);
      List<TAGToken> tokens1 = new Tokenizer(document1, ignoreTEI).getTAGTokens();
      List<TAGToken> tokens2 = new Tokenizer(document2, ignoreTEI).getTAGTokens();
      Scorer scorer = new ContentScorer();
      ViewAligner viewAligner = new ViewAligner(scorer);
      List<Segment> segments = viewAligner.align(tokens1, tokens2);

      SegmentMatcher expected0 = sM(aligned).tokensA(markupOpen("s")).tokensB(markupOpen("s"));
      assertThat(segments.get(0)).matches(expected0);

      SegmentMatcher expected1 = sM(replacement).tokensA(text("a")).tokensB(text("c"));
      assertThat(segments.get(1)).matches(expected1);

      SegmentMatcher expected2 = sM(aligned).tokensA(markupClose("s")).tokensB(markupClose("s"));
      assertThat(segments.get(2)).matches(expected2);
    });
  }

}
