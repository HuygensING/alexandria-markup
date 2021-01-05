package nl.knaw.huc.di.tag.model.graph;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class TextGraphTest {

  AtomicLong nodeIds = new AtomicLong();

  @Test
  public void testConstruction() {
    // [tagml>[a|+a>[name|+ner>J'onn<name|ner] [b|+b>craves<a|a] [name|ner>Oreos<name|ner]<b|b]<tagml]
    Long root = newNode();

    TextGraph tg = new TextGraph();
    tg.documentNode = root;

    // [tagml>
    Long markupTagml = newNode();
    String layerDefault = "";
    tg.setLayerRootMarkup(layerDefault, markupTagml);

    // [a|+a>
    Long markupA = newNode();
    String layerA = "a";
    tg.setLayerRootMarkup(layerA, markupTagml)
        .addChildMarkup(markupTagml, layerA, markupA); // (tagml) -[a]-> (a)

    // [name|+ner>
    Long markupName1 = newNode();
    String layerNER = "ner";
    tg.setLayerRootMarkup(layerNER, markupTagml)
        .addChildMarkup(markupTagml, layerNER, markupName1); // (tagml) -[ner]-> (name)

    // J'onn
    Long textJonn = newNode();
    tg.setFirstTextNodeId(textJonn) // () -> (J'onn)
        .linkMarkupToTextNodeForLayer(markupTagml, textJonn, layerDefault)
        .linkMarkupToTextNodeForLayer(markupA, textJonn, layerA)
        .linkMarkupToTextNodeForLayer(markupName1, textJonn, layerNER);

    // <name|ner]

    // _
    Long textSpace1 = newNode();
    tg//.linkTextNodes(textJonn, textSpace1) // (J'onn) -> ( )
        .linkMarkupToTextNodeForLayer(markupTagml, textSpace1, layerDefault)
        .linkMarkupToTextNodeForLayer(markupA, textSpace1, layerA);

    // [b|+b>
    Long markupB = newNode();
    String layerB = "b";
    tg.setLayerRootMarkup(layerB, markupTagml);
    tg.addChildMarkup(markupTagml, layerB, markupB); // (tagml) -[b]-> (b)

    // craves
    Long textCraves = newNode();
    tg//.linkTextNodes(textSpace1, textCraves) // ( ) -> (craves)
        .linkMarkupToTextNodeForLayer(markupTagml, textCraves, layerDefault)
        .linkMarkupToTextNodeForLayer(markupA, textCraves, layerA)
        .linkMarkupToTextNodeForLayer(markupB, textCraves, layerB);

    // <a|a]

    // _
    Long textSpace2 = newNode();
    tg//.linkTextNodes(textCraves, textSpace2) // (craves) -> ( )
        .linkMarkupToTextNodeForLayer(markupTagml, textSpace2, layerDefault)
        .linkMarkupToTextNodeForLayer(markupB, textSpace2, layerB);

    // [name|ner>
    Long markupName2 = newNode();
    tg.addChildMarkup(markupTagml, layerNER, markupName2);

    // Oreos
    Long textOreos = newNode();
    tg//.linkTextNodes(textSpace2, textOreos) // ( ) -> (Oreos)
        .linkMarkupToTextNodeForLayer(markupTagml, textOreos, layerDefault)
        .linkMarkupToTextNodeForLayer(markupB, textOreos, layerB)
        .linkMarkupToTextNodeForLayer(markupName2, textOreos, layerNER);

    tg.linkParentlessLayerRootsToDocument();

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

  @Test
  public void testNesting() {
    // [l>He said: [phr>That's what she said: [phr>Too much!<phr]<phr]<line]
    final Long documentNode = newNode();
    TextGraph tg = new TextGraph();
    tg.documentNode = documentNode;

    // [l>
    Long markupL = newNode();
    String layerDefault = "";
    tg.setLayerRootMarkup(layerDefault, markupL);

    // He said:
    Long textHeSaid = newNode();
    tg.setFirstTextNodeId(textHeSaid)
        .linkMarkupToTextNodeForLayer(markupL, textHeSaid, layerDefault);

    // [phr>
    Long markupPhr1 = newNode();
    tg.addChildMarkup(markupL, layerDefault, markupPhr1);

    // That's what she said:
    Long textSheSaid = newNode();
    tg//.linkTextNodes(textHeSaid, textSheSaid)
        .linkMarkupToTextNodeForLayer(markupPhr1, textSheSaid, layerDefault);

    // [phr>
    Long markupPhr2 = newNode();
    tg.addChildMarkup(markupPhr1, layerDefault, markupPhr2);

    // Too much!
    Long textTooMuch = newNode();
    tg//.linkTextNodes(textSheSaid, textTooMuch)
        .linkMarkupToTextNodeForLayer(markupPhr2, textTooMuch, layerDefault);
    // <phr]
    // <phr]
    // <line]
    tg.linkParentlessLayerRootsToDocument();

    List<Long> textIds = tg.getTextNodeIdStream().collect(toList());
    assertThat(textIds).containsExactly(textHeSaid, textSheSaid, textTooMuch);

    Set<String> layerNames = tg.getLayerNames();
    assertThat(layerNames).containsOnly(layerDefault);

    List<Long> defaultTextIds = tg.getTextNodeIdStreamForLayer(layerDefault).collect(toList());
    assertThat(defaultTextIds).containsExactly(textHeSaid, textSheSaid, textTooMuch);

    List<Long> markupForTooMuch = tg.getMarkupIdStreamForTextNodeId(textTooMuch).collect(toList());
    assertThat(markupForTooMuch).containsExactlyInAnyOrder(markupPhr2, markupPhr1, markupL);

    List<Long> textForPhr1 = tg.getTextNodeIdStreamForMarkupIdInLayer(markupPhr1, layerDefault).collect(toList());
    assertThat(textForPhr1).containsExactly(textSheSaid, textTooMuch);
  }

  private Long newNode() {
    return nodeIds.getAndIncrement();
  }
}
