package nl.knaw.huygens.alexandria.storage;

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
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class LayerTest {

  @Test
  public void testLayer() {
    Long markup0 = 0L;
    Long markup1 = 1L;
    Long markup2 = 2L;
    Long markup3 = 3L;
    Long markup4 = 4L;

    Layer layer = new Layer("p", markup0);
    layer.addDescendantMarkup(markup0, markup1);
    layer.addDescendantMarkup(markup0, markup2);
    layer.addDescendantMarkup(markup1, markup3);
    layer.addDescendantMarkup(markup2, markup4);

    Long textNode0 = 5L;
    Long textNode1 = 6L;
//    Long textNode2 = 7L;
    layer.linkTextNodeToMarkup(textNode0, markup3);
    layer.linkTextNodeToMarkup(textNode1, markup4);
//    layer.linkTextNodeToMarkup(textNode2, markup0);

    List<Long> markupIds0 = layer.getMarkupIdsForTextId(textNode0);
    assertThat(markupIds0).containsExactly(markup3, markup1, markup0);

    List<Long> markupIds1 = layer.getMarkupIdsForTextId(textNode1);
    assertThat(markupIds1).containsExactly(markup4, markup2, markup0);

    List<Long> testIds0 = layer.getTextIdsForMarkupId(markup0);
    assertThat(testIds0).containsExactly(textNode0, textNode1);

    List<Long> testIds1 = layer.getTextIdsForMarkupId(markup1);
    assertThat(testIds1).containsExactly(textNode0);

    List<Long> testIds2 = layer.getTextIdsForMarkupId(markup2);
    assertThat(testIds2).containsExactly(textNode1);
  }

}
