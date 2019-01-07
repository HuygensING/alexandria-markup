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
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Set;

public class VariantGraphVisualizerTest extends AlexandriaBaseStoreTest {
  private static final Logger LOG = LoggerFactory.getLogger(VariantGraphVisualizerTest.class);

  @Test
  public void test1() {
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

    visualizeDiff("A-1", originText, "B-1", editedText);
  }

  @Test
  public void test2() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>\n" +
        "Une belle<l|M][l|M>main de femme, élégante et fine,<l][l|M> malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]\n";
    String editedText = "[TAGML|+N>\n" +
        "[text|N>\n" +
        "[s|N>Une belle main de femme, élégante et fine.<s][s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<text]<TAGML]\n";

    visualizeDiff("A-2", originText, "B-2", editedText);
  }

  @Test
  public void test3() {
    String originText = "[TAGML|+M>\n" +
        "[body|M>\n" +
        "[s|M>Une belle main de femme, élégante et fine, malgré [del|M>l'agrandissement du<del] close-up.\n" +
        "<s]\n" +
        "<body]\n" +
        "<TAGML]\n";
    String editedText = "[TAGML|+M>\n" +
        "[body|M>\n" +
        "[s|M>Une [add|M>belle<add] main de femme, élégante et fine.<s]\n" +
        "[s|M>Malgré l'agrandissement du close-up.<s]\n" +
        "<body]\n" +
        "<TAGML]\n";

    visualizeDiff("A-3", originText, "B-3", editedText);
  }

  @Test
  public void test4() {
    String originText = "[line>The rain in Spain falls mainly on the plain.<line]";
    String editedText = "[markup annotation_1='string value' annotation_2=2.718>Some text<markup]";
    visualizeDiff("A-4", originText, "B-4", editedText);
  }

  @Test
  public void test5() {
    String originText = "[TAGML|+M>Leentje leerde Lotje lopen<TAGML]\n";
    String editedText = "[TAGML|+M>leerde Lotje lopen langs de lange Lindelaan.<TAGML]\n";

    visualizeDiff("A-4", originText, "B-4", editedText);
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
