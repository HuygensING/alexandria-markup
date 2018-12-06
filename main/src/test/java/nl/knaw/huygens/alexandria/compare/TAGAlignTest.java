package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * main
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
import prioritised_xml_collation.TAGToken;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TAGAlignTest extends AlexandriaBaseStoreTest {

  @Test
  public void testMarkupAlignment() {
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
    store.runInTransaction(() -> {
      TAGMLImporter importer = new TAGMLImporter(store);
      TAGDocument original = importer.importTAGML(originText.replace("\n", ""));
      TAGDocument edited = importer.importTAGML(editedText.replace("\n", ""));
      Set<String> none = Collections.EMPTY_SET;
      TAGView allTags = new TAGView(store).setMarkupToExclude(none);

      List<TAGToken> originalTokens = new Tokenizer(original, allTags)
          .getTAGTokens();
      List<TAGToken> editedTokens = new Tokenizer(edited, allTags)
          .getTAGTokens();

      original.getMarkupStream();

      // align eerst de text tokens,
      // bepaal bij de original en edited markup voor iedere markup welk deel van de superwitness ze omspannen
      // markup m1: firstTextSegmentRank, lastTextSegmentRank


    });
  }
}
