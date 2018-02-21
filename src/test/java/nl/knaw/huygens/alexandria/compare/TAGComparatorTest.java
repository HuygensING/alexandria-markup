package nl.knaw.huygens.alexandria.compare;

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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TAGComparatorTest extends AlexandriaBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGComparatorTest.class);

  @Test
  public void testNoChanges() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";

    TAGComparator comparator = compare(originText, editedText);

    assertThat(comparator).hasFoundNoDifference();
  }

  @Test
  public void testOmission() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced technology is magic.{quote]";

    TAGComparator comparator = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        " [quote}Any sufficiently advanced technology ",//
        "-is indistinguishable from ",//
        " magic.{quote]"//
    ));
    assertThat(comparator.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testAddition() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced technology is virtually indistinguishable from magic.{quote]";

    TAGComparator comparator = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        " [quote}Any sufficiently advanced technology is ",//
        "+virtually ",//
        " indistinguishable from magic.{quote]"//
    ));
    assertThat(comparator.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testReplacement() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced code is virtually indistinguishable from magic.{quote]";

    TAGComparator comparator = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        " [quote}Any sufficiently advanced ",//
        "-technology ",//
        "+code ",//
        " is indistinguishable from magic.{quote]"//
    ));
    assertThat(comparator.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testReplacement2() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[s}Any sufficiently advanced code is virtually indistinguishable from magic.{s]";

    TAGComparator comparator = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        "-[quote}",//
        "+[s}",//
        " Any sufficiently advanced ",//
        "-technology ",//
        "+code ",//
        " is indistinguishable from magic.",//
        "-{quote]",//
        "+{s]"//
    ));
    assertThat(comparator.getDiffLines()).containsExactlyElementsOf(expected);
  }

  private TAGComparator compare(String originText, String editedText) {
    return store.runInTransaction(() -> {
      LMNLImporter importer = new LMNLImporter(store);
      DocumentWrapper original = importer.importLMNL(originText);
      DocumentWrapper edited = importer.importLMNL(editedText);
      Set<String> quote = new HashSet<>(singletonList("quote"));
      TAGView onlyQuote = new TAGView(store).setMarkupToInclude(quote);

      TAGComparator comparator = new TAGComparator(original, onlyQuote, edited);
      LOG.info("diffLines = \n{}", comparator.getDiffLines()//
          .stream()//
          .map(l -> "'" + l + "'")//
          .collect(joining("\n")));
      return comparator;
    });
  }
}
