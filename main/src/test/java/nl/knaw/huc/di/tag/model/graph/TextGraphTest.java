package nl.knaw.huc.di.tag.model.graph;

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
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class TextGraphTest {

  @Test
  public void testConstruction() {
    // [tagml>[a|+a>[name|+ner>J'onn<name|ner] [b|+b>craves<a|a] [name|ner>Oreos<name|ner]<b|b]<tagml]

    AtomicLong nodeIds = new AtomicLong();
    TextGraph tg = new TextGraph();

    // [tagml>
    Long markupTagml = nodeIds.getAndIncrement();
    String layerDefault = "";
    tg.setLayerRootMarkup(layerDefault, markupTagml);

    // [a|+a>
    Long markupA = nodeIds.getAndIncrement();
    String layerA = "a";
    tg.setLayerRootMarkup(layerA, markupTagml)
        .addChildMarkup(markupTagml, layerA, markupA); // (tagml) -[a]-> (a)

    // [name|+ner>
    Long markupName1 = nodeIds.getAndIncrement();
    String layerNER = "ner";
    tg.setLayerRootMarkup(layerNER, markupTagml)
        .addChildMarkup(markupTagml, layerNER, markupName1); // (tagml) -[ner]-> (name)

    // J'onn
    Long textJonn = nodeIds.getAndIncrement();
    tg.appendTextNode(textJonn) // () -> (J'onn)
        .linkMarkupToTextNode(markupTagml, layerDefault, textJonn)
        .linkMarkupToTextNode(markupA, layerA, textJonn)
        .linkMarkupToTextNode(markupName1, layerNER, textJonn);

    // <name|ner]

    // _
    Long textSpace1 = nodeIds.getAndIncrement();
    tg.appendTextNode(textSpace1) // (J'onn) -> ( )
        .linkMarkupToTextNode(markupTagml, layerDefault, textSpace1)
        .linkMarkupToTextNode(markupA, layerA, textSpace1);

    // [b|+b>
    Long markupB = nodeIds.getAndIncrement();
    String layerB = "b";
    tg.setLayerRootMarkup(layerB, markupTagml);
    tg.addChildMarkup(markupTagml, layerB, markupB); // (tagml) -[b]-> (b)

    // craves
    Long textCraves = nodeIds.getAndIncrement();
    tg.appendTextNode(textCraves) // ( ) -> (craves)
        .linkMarkupToTextNode(markupTagml, layerDefault, textCraves)
        .linkMarkupToTextNode(markupA, layerA, textCraves)
        .linkMarkupToTextNode(markupB, layerB, textCraves);

    // <a|a]

    // _
    Long textSpace2 = nodeIds.getAndIncrement();
    tg.appendTextNode(textSpace2) // (craves) -> ( )
        .linkMarkupToTextNode(markupTagml, layerDefault, textSpace2)
        .linkMarkupToTextNode(markupB, layerB, textSpace2);

    // [name|ner>
    Long markupName2 = nodeIds.getAndIncrement();
    tg.addChildMarkup(markupTagml, layerNER, markupName2);

    // Oreos
    Long textOreos = nodeIds.getAndIncrement();
    tg.appendTextNode(textOreos) // ( ) -> (Oreos)
        .linkMarkupToTextNode(markupTagml, layerDefault, textOreos)
        .linkMarkupToTextNode(markupB, layerB, textOreos)
        .linkMarkupToTextNode(markupName2, layerNER, textOreos);

    // <name|ner]
    // <b|b]
    // <tagml]

    List<Long> textIds = tg.getTextNodeIdStream().collect(toList());
    assertThat(textIds).containsExactly(textJonn, textSpace1, textCraves, textSpace2, textOreos);

    Set<String> layerNames = tg.getLayerNames();
    assertThat(layerNames).containsOnly(layerDefault, layerA, layerB, layerNER);

    List<Long> defaultTextIds = tg.getTextNodeIdStreamForLayer(layerDefault).collect(toList());
    assertThat(defaultTextIds).containsExactly(textJonn, textSpace1, textCraves, textSpace2, textOreos);

    List<Long> layerATextIds = tg.getTextNodeIdStreamForLayer(layerA).collect(toList());
    assertThat(layerATextIds).containsExactly(textJonn, textSpace1, textCraves);

    List<Long> layerBTextIds = tg.getTextNodeIdStreamForLayer(layerB).collect(toList());
    assertThat(layerBTextIds).containsExactly(textCraves, textSpace2, textOreos);

    List<Long> layerNERTextIds = tg.getTextNodeIdStreamForLayer(layerNER).collect(toList());
    assertThat(layerNERTextIds).containsExactly(textJonn, textOreos);

    List<Long> markupForJonnNER = tg.getMarkupIdStreamForTextNodeId(textJonn, layerNER).collect(toList());
    assertThat(markupForJonnNER).containsExactly(markupName1, markupTagml);

    List<Long> markupForJonn = tg.getMarkupIdStreamForTextNodeId(textJonn).collect(toList());
    assertThat(markupForJonn).containsExactlyInAnyOrder(markupA, markupName1, markupTagml);

    List<Long> textForName = tg.getTextNodeIdStreamForMarkupIdInLayer(markupTagml, layerNames).collect(toList());
    assertThat(textForName).containsExactly(textJonn, textOreos);
  }
}
