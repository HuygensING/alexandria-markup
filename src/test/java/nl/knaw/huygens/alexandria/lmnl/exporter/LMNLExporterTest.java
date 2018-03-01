package nl.knaw.huygens.alexandria.lmnl.exporter;

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
import nl.knaw.huygens.alexandria.AlexandriaSoftAssertions;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import org.junit.Rule;
import org.junit.Test;

public class LMNLExporterTest extends AlexandriaBaseStoreTest {
  @Rule
  public final AlexandriaSoftAssertions softly = new AlexandriaSoftAssertions();

  @Test
  public void testExportAfterImportLeavesNothingBehind() {
    String[] lmnl = {
        "[a}A{a]",
        "[x [owner}y{owner]}X{x]",
        "[a}[b}C{b]{a]",
        "[a}[b}a [c}b{b] c{c]{a]",
        "[thing [class [note}This is a [n}nested{n] comment{note]}animal{class]}louse{thing]"
    };
    store.runInTransaction(() -> {
      LMNLImporter importer = new LMNLImporter(store);
      LMNLExporter exporter = new LMNLExporter(store);
      for (String l : lmnl) {
        DocumentWrapper doc = importer.importLMNL(l);
        String exported = exporter.toLMNL(doc);
        softly.assertThat(exported).isEqualTo(l);
      }
    });
  }
}
