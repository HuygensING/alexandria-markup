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

  @Ignore
  @Test
  public void testMarkupDiff0() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>\n" +
        "Une [del|M>jolie<del][add|M>belle<add] main de femme, élégante et fine,<l][l|M> malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]\n";
    String editedText = "[TAGML|+N>\n" +
        "[text|N>\n" +
        "[s|N>Une belle main de femme, élégante et fine.<s][s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<text]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[l|M] replaced by [s|N]", "[l|M] replaced by [s|N]");
  }

  @Test
  public void testMarkupDeletion() {
    String originText = "[TAGML>A simple [del>short<del] text<TAGML]\n";
    String editedText = "[TAGML>A simple text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[del](2-2) deleted");
  }

  @Test
  public void testMarkupAddition() {
    String originText = "[TAGML>A simple text<TAGML]\n";
    String editedText = "[TAGML>A simple [add>short<add] text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[add](2-2) added");
  }

  @Test
  public void testMarkupReplacement() {
    String originText = "[TAGML>A [a>simple<a] text<TAGML]\n";
    String editedText = "[TAGML>A [b>simple<b] text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[a](2-2) replaced by [b](2-2)");
//    assertThat(markupInfoDiffs).containsExactly("[a>simple<a] replaced by [b>simple<b]");
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
      List<String> diffMarkupInfo = differ.diffMarkupInfo(markupInfoLists);
      LOG.info("{}", diffMarkupInfo);
      return diffMarkupInfo;
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

