package nl.knaw.huygens.alexandria.compare;

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
  public void test() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>\n" +
        "Une [del|M>[add|M>jolie<add]<del][add|M>belle<add] main de femme, élégante et fine<l] [l|M>malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]";
    String editedText = "[TAGML|+M,+N>\n" +
        "[text|M,N>\n" +
        "[l|M>\n" +
        "[s|N>Une [del|M>[add|M>jolie<add]<del][add|M>belle<add] main de femme, élégante et fine.<l]<s] [l|M>[s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<l]\n" +
        "<text]<TAGML]";

    store.runInTransaction(() -> {
      TAGMLImporter importer = new TAGMLImporter(store);
      TAGDocument original = importer.importTAGML(originText);
      TAGDocument edited = importer.importTAGML(editedText);
      Set<String> none = Collections.EMPTY_SET;
      TAGView allTags = new TAGView(store).setMarkupToExclude(none);

      VariantGraphVisualizer vgv = new VariantGraphVisualizer();
      String dot = vgv.visualizeVariation(original, edited, allTags);
      LOG.info("dot=\n{}\n", dot);
    });

  }

}
