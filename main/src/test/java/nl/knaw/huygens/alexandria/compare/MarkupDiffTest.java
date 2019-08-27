package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkupDiffTest extends AlexandriaBaseStoreTest {
  Logger LOG = LoggerFactory.getLogger(MarkupDiffTest.class);

  @Test
  public void testTAGMLDiffCase1a() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>" +
        "Une [del|M>jolie<del][add|M>belle<add] main de femme, élégante et fine, <l][l|M>malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]";
    String editedText = "[TAGML|+N>\n" +
        "[text|N>\n" +
        "[s|N>Une belle main de femme, élégante et fine.<s][s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<text]<TAGML]";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly(
        "layeridentifier change [TAGML|M](0-7) -> [TAGML|N](0-7)",
        "layeridentifier change [text|M](0-7) -> [text|N](0-7)",
        "replace [l|M](0-5) -> [s|N](0-5)",
        "replace [l|M](6-7) -> [s|N](6-7)",
        "del [del|M](1-1)",
        "del [add|M](2-2)"
    );
  }

  @Test
  public void testTAGMLDiffCase1b() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>" +
        "Une [del|M>jolie<del][add|M>belle<add] main de femme, élégante et fine, <l][l|M>malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]";
    String editedText = "[TAGML|+N>\n" +
        "[text|N>\n" +
        "[s|N>Une belle main de femme, élégante et fine.<s] [s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<text]<TAGML]";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly(
        "layeridentifier change [TAGML|M](0-8) -> [TAGML|N](0-8)",
        "layeridentifier change [text|M](0-8) -> [text|N](0-8)",
        "del [l|M](0-6)",
        "replace [l|M](7-8) -> [s|N](7-8)",
        "del [del|M](1-1)",
        "del [add|M](2-2)",
        "add [s|N](0-5)"
    );

//    Edit operations on markup:
//
//    { "layeridentifier_change" : [TAGML|M][TAGML|N] }
//    { "layeridentifier_change" : [TAGML/text|M][TAGML/text|N] }
//    the Markup nodes are a match, but their layer suffix is not.
//
//    { "replace" : [TAGML/text/l[1]|M][TAGML/text/s[1]|N] }
//    { "replace" : [TAGML/text/l[2]|M][TAGML/text/s[2]|N] }
//    We need to specify that the first child of the text node is replaced.
//
//    { "del" : [TAGML/text/l[1]/del|M][] }
//    { "del" : [TAGML/text/l[1]/add|M][] }

  }


  //  @Ignore
  @Test
  public void testMarkupDiff0() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>\n" +
        "Une [del|M>jolie<del][add|M>belle<add] main de femme, élégante et fine, <l][l|M>malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]\n";
    String editedText = "[TAGML|+N>\n" +
        "[text|N>\n" +
        "[s|N>Une belle main de femme, élégante et fine.<s][s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<text]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly(
        "layeridentifier change [TAGML|M](0-7) -> [TAGML|N](0-7)",
        "layeridentifier change [text|M](0-7) -> [text|N](0-7)",
        "replace [l|M](0-5) -> [s|N](0-5)",
        "replace [l|M](6-7) -> [s|N](6-7)",
        "del [del|M](1-1)",
        "del [add|M](2-2)");
  }

  @Test
  public void testMarkupDeletion() {
    String originText = "[TAGML>A simple [del>short<del] text<TAGML]\n";
    String editedText = "[TAGML>A simple text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("del [del](1-1)");
  }

  @Test
  public void testMarkupAddition() {
    String originText = "[TAGML>A simple text<TAGML]\n";
    String editedText = "[TAGML>A simple [add>short<add] text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("add [add](1-1)");
  }

  @Test
  public void testMarkupReplacement() {
    String originText = "[TAGML>A [a>simple<a] text<TAGML]\n";
    String editedText = "[TAGML>A [b>simple<b] text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("replace [a](1-1) -> [b](1-1)");
  }

  @Ignore
  @Test
  public void testMarkupSplit() {
    String originText = "[TAGML>[l>Sentence one. Sentence two.<l]<TAGML]\n";
    String editedText = "[TAGML>[l>Sentence one.<l][l>Sentence two.<l]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[l](1,2) split in {[l](1,1),[l](2,1)}");
  }

  @Ignore
  @Test
  public void testMarkupJoin() {
    String originText = "[TAGML>[l>Sentence one.<l][l>Sentence two.<l]<TAGML]\n";
    String editedText = "[TAGML>[l>Sentence one. Sentence two.<l]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("{[l](1,1),[l](2,1)} joined to [l](1,2)");
  }

  private List<String> getMarkupDiffs(final String originText, final String editedText) {
    visualizeDiff("A", originText, "B", editedText);
    return runInStoreTransaction(store -> {
      TAGMLImporter importer = new TAGMLImporter(store);
      TAGDocument original = importer.importTAGML(originText.replace("\n", ""));
      TAGDocument edited = importer.importTAGML(editedText.replace("\n", ""));
      Set<String> none = Collections.EMPTY_SET;
      TAGView tagView = new TAGView(store).setMarkupToExclude(none);
      TAGComparison2 differ = new TAGComparison2(original, tagView, edited, store);
      List<TAGComparison2.MarkupInfo>[] markupInfoLists = differ.getMarkupInfoLists();
      assertThat(markupInfoLists).hasSize(2);
      for (int i = 0; i < 2; i++) {
        for (TAGComparison2.MarkupInfo mi : markupInfoLists[i]) {
          LOG.info("{}: {}", i, mi);
        }
      }
//      List<String> diffMarkupInfo = differ.diffMarkupInfo(markupInfoLists, TAGComparison2.HR_DIFFPRINTER);
//      LOG.info("{}", diffMarkupInfo);
//      List<String> mrDiffMarkupInfo = differ.diffMarkupInfo(markupInfoLists, TAGComparison2.MR_DIFFPRINTER);
//      LOG.info("{}", mrDiffMarkupInfo);
      return differ.getDiffLines();
    });
  }

  private void visualizeDiff(final String witness1, final String tagml1, final String witness2, final String tagml2) {
    LOG.info("{}:\n{}", witness1, tagml1);
    LOG.info("{}:\n{}", witness2, tagml2);
    runInStoreTransaction(store -> {
      TAGMLImporter importer = new TAGMLImporter(store);
      TAGDocument original = importer.importTAGML(tagml1.replace("\n", ""));
      TAGDocument edited = importer.importTAGML(tagml2.replace("\n", ""));
      Set<String> none = Collections.EMPTY_SET;
      TAGView allTags = new TAGView(store).setMarkupToExclude(none);

      DiffVisualizer visualizer = new AsHTMLDiffVisualizer();
//      DiffVisualizer visualizer = new AsDOTDiffVisualizer();
      new VariantGraphVisualizer(visualizer)
          .visualizeVariation(witness1, original, witness2, edited, allTags);
      String result = visualizer.getResult();
      LOG.info("result=\n" +
          "------8<---------------------------------------\n" +
          "{}\n" +
          "------8<---------------------------------------\n", result);
    });
  }

}

