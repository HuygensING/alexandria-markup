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
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptySet;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.compare.MarkupInfo.State.*;
import static nl.knaw.huygens.alexandria.compare.Segment.Type.addition;

public class SegmentToolTest extends AlexandriaBaseStoreTest {

  @Test
  public void testComputeTextNodeInfo() {
    List<TAGToken> tokensA = tokenize("");
    List<TAGToken> tokensB = tokenize("a{x][y}{z]bla bla [b}bla{b]");

    Segment segment = new Segment(tokensA, tokensB, addition);
    List<TextNodeInfo> textNodeInfos = SegmentTool.computeTextNodeInfo(segment);
    assertThat(textNodeInfos).hasSize(3); // 2 texts

    TextNodeInfo textNodeInfo0 = textNodeInfos.get(0);
    assertThat(textNodeInfo0.getText()).isEqualTo("a");

    List<MarkupInfo> markupInfo0 = textNodeInfo0.getMarkupInfoList();
    assertThat(markupInfo0).hasSize(2); // {x], {z]

    MarkupInfo markupInfo00 = markupInfo0.get(0);
    assertThat(markupInfo00.getTag()).isEqualTo("x");
    assertThat(markupInfo00.getState()).isEqualTo(openStart);

    MarkupInfo markupInfo01 = markupInfo0.get(0);
    assertThat(markupInfo01.getTag()).isEqualTo("z");
    assertThat(markupInfo01.getState()).isEqualTo(openStart);

    TextNodeInfo textNodeInfo1 = textNodeInfos.get(1);
    assertThat(textNodeInfo1.getText()).isEqualTo("bla bla ");

    List<MarkupInfo> markupInfo1 = textNodeInfo1.getMarkupInfoList();
    assertThat(markupInfo1).hasSize(1); // [y}
    MarkupInfo markupInfo10 = markupInfo1.get(0);
    assertThat(markupInfo10.getTag()).isEqualTo("y");
    assertThat(markupInfo10.getState()).isEqualTo(openEnd);

    TextNodeInfo textNodeInfo2 = textNodeInfos.get(2);
    assertThat(textNodeInfo2.getText()).isEqualTo("bla");

    List<MarkupInfo> markupInfo2 = textNodeInfo2.getMarkupInfoList();
    assertThat(markupInfo2).hasSize(1); // [b}

    MarkupInfo markupInfo20 = markupInfo1.get(0);
    assertThat(markupInfo20.getTag()).isEqualTo("b");
    assertThat(markupInfo20.getState()).isEqualTo(closed);

  }

  private List<TAGToken> tokenize(String string) {
    TAGView tagView = new TAGView(store).setMarkupToExclude(emptySet());
    LMNLImporter importer = new LMNLImporter(store);
    DocumentWrapper aDocument = importer.importLMNL(string);
    return new Tokenizer(aDocument, tagView).getTAGTokens();
  }

}
