package nl.knaw.huygens.alexandria;

/*
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

import static java.util.Arrays.asList;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class TAGViewTest {

  @Test
  public void testFilterRelevantMarkup() {
    Limen limen = new Limen();
    String tag1 = "a";
    Markup markup1 = new Markup(limen, tag1);
    String tag2 = "b";
    Markup markup2 = new Markup(limen, tag2);
    String tag3 = "c";
    Markup markup3 = new Markup(limen, tag3);
    String tag4 = "d";
    Markup markup4 = new Markup(limen, tag4);
    Set<Markup> allMarkup = new HashSet<>(asList(markup1, markup2, markup3, markup4));

    Set<String> odds = new HashSet<>(asList(tag1,tag3));
    Set<String> evens = new HashSet<>(asList(tag2,tag4));

    TAGView viewNoAC = new TAGView().setMarkupToExclude(odds);

    Set<Markup> filteredMarkups = viewNoAC.filterRelevantMarkup(allMarkup);
    assertThat(filteredMarkups).containsExactlyInAnyOrder(markup2,markup4);

    TAGView viewBD = new TAGView().setMarkupToInclude(evens);

    Set<Markup> filteredMarkups2 = viewBD.filterRelevantMarkup(allMarkup);
    assertThat(filteredMarkups2).containsExactlyInAnyOrder(markup2,markup4);

    TAGView viewAC = new TAGView().setMarkupToInclude(odds);

    Set<Markup> filteredMarkups3 = viewAC.filterRelevantMarkup(allMarkup);
    assertThat(filteredMarkups3).containsExactlyInAnyOrder(markup1,markup3);

    LMNLImporter importer = new LMNLImporter();
    Document document = importer.importLMNL("[a}a[b}b[c}c[d}da{a]b{b]c{c]d{d]");

    LMNLExporter exporter1 = new LMNLExporter(viewNoAC);
    String lmnlBD = exporter1.toLMNL(document);
    assertThat(lmnlBD).isEqualTo("a[b}bc[d}dab{b]cd{d]");

    LMNLExporter exporter2 = new LMNLExporter(viewAC);
    String lmnlAC = exporter2.toLMNL(document);
    assertThat(lmnlAC).isEqualTo("[a}ab[c}cda{a]bc{c]d");
  }

}