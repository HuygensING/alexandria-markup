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
        .linkMarkupToTextNodeForLayer(markupTagml, textJonn, layerDefault)
        .linkMarkupToTextNodeForLayer(markupA, textJonn, layerA)
        .linkMarkupToTextNodeForLayer(markupName1, textJonn, layerNER);

    // <name|ner]

    // _
    Long textSpace1 = nodeIds.getAndIncrement();
    tg.appendTextNode(textSpace1) // (J'onn) -> ( )
        .linkMarkupToTextNodeForLayer(markupTagml, textSpace1, layerDefault)
        .linkMarkupToTextNodeForLayer(markupA, textSpace1, layerA);

    // [b|+b>
    Long markupB = nodeIds.getAndIncrement();
    String layerB = "b";
    tg.setLayerRootMarkup(layerB, markupTagml);
    tg.addChildMarkup(markupTagml, layerB, markupB); // (tagml) -[b]-> (b)

    // craves
    Long textCraves = nodeIds.getAndIncrement();
    tg.appendTextNode(textCraves) // ( ) -> (craves)
        .linkMarkupToTextNodeForLayer(markupTagml, textCraves, layerDefault)
        .linkMarkupToTextNodeForLayer(markupA, textCraves, layerA)
        .linkMarkupToTextNodeForLayer(markupB, textCraves, layerB);

    // <a|a]

    // _
    Long textSpace2 = nodeIds.getAndIncrement();
    tg.appendTextNode(textSpace2) // (craves) -> ( )
        .linkMarkupToTextNodeForLayer(markupTagml, textSpace2, layerDefault)
        .linkMarkupToTextNodeForLayer(markupB, textSpace2, layerB);

    // [name|ner>
    Long markupName2 = nodeIds.getAndIncrement();
    tg.addChildMarkup(markupTagml, layerNER, markupName2);

    // Oreos
    Long textOreos = nodeIds.getAndIncrement();
    tg.appendTextNode(textOreos) // ( ) -> (Oreos)
        .linkMarkupToTextNodeForLayer(markupTagml, textOreos, layerDefault)
        .linkMarkupToTextNodeForLayer(markupB, textOreos, layerB)
        .linkMarkupToTextNodeForLayer(markupName2, textOreos, layerNER);

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

    List<Long> textForDefault = tg.getTextNodeIdStreamForMarkupIdInLayer(markupTagml, "").collect(toList());
    assertThat(textForDefault).containsExactly(textJonn, textSpace1, textCraves, textSpace2, textOreos);

    List<Long> textForA = tg.getTextNodeIdStreamForMarkupIdInLayer(markupTagml, layerA).collect(toList());
    assertThat(textForA).containsExactly(textJonn, textSpace1, textCraves);

    List<Long> textForB = tg.getTextNodeIdStreamForMarkupIdInLayer(markupTagml, layerB).collect(toList());
    assertThat(textForB).containsExactly(textCraves, textSpace2, textOreos);

    List<Long> textForNER = tg.getTextNodeIdStreamForMarkupIdInLayer(markupTagml, layerNER).collect(toList());
    assertThat(textForNER).containsExactly(textJonn, textOreos);
  }
}
