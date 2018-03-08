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
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.compare.MarkupInfo.State.*;
import static nl.knaw.huygens.alexandria.compare.Segment.Type.addition;

public class SegmentToolTest extends AlexandriaBaseStoreTest {

  @Test
  public void testComputeTextNodeInfo() {
    // ""
    List<TAGToken> tokensA = new ArrayList<>();
    // "a{x][y}{z]bla bla [b}bla{b]"
    List<TAGToken> tokensB = new ArrayList<>(asList(
        t("a"),
        mc("x"),
        mo("y"),
        mc("z"),
        t("bla "),
        t("bla "),
        mo("b"),
        t("bla"),
        mc("b")
    ));

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

    MarkupInfo markupInfo01 = markupInfo0.get(1);
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
    assertThat(markupInfo2).hasSize(2); // [y}, [b}

    MarkupInfo markupInfo20 = markupInfo2.get(0);
    assertThat(markupInfo20.getTag()).isEqualTo("y");
    assertThat(markupInfo20.getState()).isEqualTo(openEnd);

    MarkupInfo markupInfo21 = markupInfo2.get(1);
    assertThat(markupInfo21.getTag()).isEqualTo("b");
    assertThat(markupInfo21.getState()).isEqualTo(closed);
  }

  private static TextToken t(String content) {
    return new TextToken(content);
  }

  private static MarkupOpenToken mo(String content) {
    return new MarkupOpenToken(content);
  }

  private static MarkupCloseToken mc(String content) {
    return new MarkupCloseToken(content);
  }

}
