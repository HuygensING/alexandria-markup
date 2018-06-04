package nl.knaw.huygens.alexandria.view;

/*
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

import static java.util.Arrays.asList;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class TAGViewTest extends AlexandriaBaseStoreTest {

  @Test
  public void testFilterRelevantMarkup() {
    store.runInTransaction(() -> {
      TAGDocument document = store.createDocument();

      String tag1 = "a";
      Long markupId1 = createNewMarkup(document, tag1);

      String tag2 = "b";
      Long markupId2 = createNewMarkup(document, tag2);

      String tag3 = "c";
      Long markupId3 = createNewMarkup(document, tag3);

      String tag4 = "d";
      Long markupId4 = createNewMarkup(document, tag4);

      Set<Long> allMarkupIds = new HashSet<>(asList(markupId1, markupId2, markupId3, markupId4));

      Set<String> odds = new HashSet<>(asList(tag1, tag3));
      Set<String> evens = new HashSet<>(asList(tag2, tag4));

      TAGView viewNoAC = new TAGView(store).setMarkupToExclude(odds);

      Set<Long> filteredMarkupIds = viewNoAC.filterRelevantMarkup(allMarkupIds);
      assertThat(filteredMarkupIds).containsExactlyInAnyOrder(markupId2, markupId4);

      TAGView viewBD = new TAGView(store).setMarkupToInclude(evens);

      Set<Long> filteredMarkupIds2 = viewBD.filterRelevantMarkup(allMarkupIds);
      assertThat(filteredMarkupIds2).containsExactlyInAnyOrder(markupId2, markupId4);

      TAGView viewAC = new TAGView(store).setMarkupToInclude(odds);

      Set<Long> filteredMarkupIds3 = viewAC.filterRelevantMarkup(allMarkupIds);
      assertThat(filteredMarkupIds3).containsExactlyInAnyOrder(markupId1, markupId3);

      LMNLImporter importer = new LMNLImporter(store);
      TAGDocument document1 = importer.importLMNL("[a}a[b}b[c}c[d}da{a]b{b]c{c]d{d]");

      LMNLExporter exporter1 = new LMNLExporter(store, viewNoAC);
      String lmnlBD = exporter1.toLMNL(document1);
      assertThat(lmnlBD).isEqualTo("a[b}bc[d}dab{b]cd{d]");

      LMNLExporter exporter2 = new LMNLExporter(store, viewAC);
      String lmnlAC = exporter2.toLMNL(document1);
      assertThat(lmnlAC).isEqualTo("[a}ab[c}cda{a]bc{c]d");
    });
  }

  private Long createNewMarkup(TAGDocument document, String tag1) {
    TAGMarkup markup1 = store.createMarkup(document, tag1);
    return markup1.getDbId();
  }

}
