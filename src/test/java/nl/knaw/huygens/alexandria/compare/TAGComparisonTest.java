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

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.joining;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;

public class TAGComparisonTest extends AlexandriaBaseStoreTest {

  private static final Logger LOG = LoggerFactory.getLogger(TAGComparisonTest.class);

  @Test
  public void testNoChanges() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";

    TAGComparison comparison = compare(originText, editedText);

    assertThat(comparison).hasFoundNoDifference();
  }

  @Test
  public void testOmission() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced technology is magic.{quote]";

    TAGComparison comparison = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        " [quote}Any sufficiently advanced technology is ",//
        "-indistinguishable from ",//
        " magic.{quote]"//
    ));
    assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testAddition() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced technology is virtually indistinguishable from magic.{quote]";

    TAGComparison comparison = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        " [quote}Any sufficiently advanced technology is ",//
        "+virtually ",//
        " indistinguishable from magic.{quote]"//
    ));
    assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testReplacement() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced code is indistinguishable from magic.{quote]";

    TAGComparison comparison = compare(originText, editedText);

    List<String> expected = new ArrayList<>(asList(//
        " [quote}Any sufficiently advanced ",//
        "-technology ",//
        "+code ",//
        " is indistinguishable from magic.{quote]"//
    ));
    assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testReplacement2() {
    String originText = "[quote}Any sufficiently advanced technology is indistinguishable from magic.{quote]";
    String editedText = "[s}Any sufficiently advanced code is indistinguishable from magic.{s]";

    TAGComparison comparison = compare(originText, editedText);
    assertThat(comparison.hasDifferences()).isTrue();

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
    assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Ignore
  @Test
  public void testNewlinesInText() {
    String originText = "[l}line 1{l]\n[l}line 2{l]\n[l}line 3{l]";
    String editedText = "[l}line 1{l]\n[l}line 1a{l]\n[l}line 2{l]\n[l}line 3{l]";

    TAGComparison comparison = compare(originText, editedText);
    assertThat(comparison.hasDifferences()).isTrue();

    List<String> expected = new ArrayList<>(asList(//
        " [l}line 1{l]\n",//
        "+[l}line 1a{l]\n",//
        " [l}line 2{l]\n",//
        " [l}line 3{l]\n"//
    ));
    assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected);
  }

  @Test
  public void testMergeTextReplacement() {
    String originText = "[quote}Any [emp}sufficiently advanced technology{emp] is indistinguishable from magic.{quote]";
    String editedText = "[quote}Any sufficiently advanced code is indistinguishable from magic.{quote]";
    String expected = "[quote}Any [emp}sufficiently advanced code{emp] is indistinguishable from magic.{quote]";
    Set<String> quote = singleton("quote");
    assertMerge(originText, quote, editedText, expected);
  }

  @Test
  public void testMergeMarkupReplacement() {
    String originText = "[quote}Any [emp}sufficiently advanced technology{emp] is indistinguishable from magic.{quote]";
    String editedText = "[s}Any sufficiently advanced technology is indistinguishable from magic.{s]";
    String expected = "[s}Any [emp}sufficiently advanced technology{emp] is indistinguishable from magic.{s]";
    Set<String> quote = singleton("quote");
    assertMerge(originText, quote, editedText, expected);
  }

  @Test
  public void testMergeTextPrepend() {
    String originText = "[l}text{l]";
    String editedText = "prequel [l}text{l]";
    String expected = "prequel [l}text{l]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextAppend() {
    String originText = "[l}text{l]";
    String editedText = "[l}text{l] postscript";
    String expected = "[l}text{l] postscript";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextAdditionStart() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    String editedText = "[l}A line 1{l]" +
        "[l}line 2{l]";
    String expected = "[excerpt}" +
        "[l}A line 1{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextAdditionMiddle() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    String editedText = "[l}line = 1{l]" +
        "[l}line 2{l]";
    String expected = "[excerpt}" +
        "[l}line = 1{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextAdditionEnd() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    String editedText = "[l}line 1 A{l]" +
        "[l}line 2{l]";
    String expected = "[excerpt}" +
        "[l}line 1 A{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextAndMarkupAddition() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    String editedText = "[l}line 1{l]" +
        "[l}line 1a{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]";
    String expected = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 1a{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeMarkupAddition() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    String editedText = "[l}line 1{l]" +
        "[l}line{l] [l}2{l]" +
        "[l}line 3{l]";
    String expected = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line{l] [l}2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextOmission() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    String editedText = "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line{l]";
    String expected = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeTextAndMarkupOmission() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    String editedText = "[l}line 1{l]" +
        "[l}line 2{l]";
    String expected = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  @Test
  public void testMergeMarkupOmission() {
    String originText = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2{l]" +
        "[l}line 3{l]" +
        "{excerpt]";
    String editedText = "[l}line 1{l]" +
        "[l}line 2 line 3{l]";
    String expected = "[excerpt}" +
        "[l}line 1{l]" +
        "[l}line 2 line 3{l]" +
        "{excerpt]";
    Set<String> lines = singleton("l");
    assertMerge(originText, lines, editedText, expected);
  }

  private void assertMerge(final String originText, final Set<String> includedTags, final String editedText, final String expectedLmnl) {
    store.runInTransaction(() -> {
      LMNLImporter importer = new LMNLImporter(store);
      DocumentWrapper original = importer.importLMNL(originText);
      DocumentWrapper edited = importer.importLMNL(editedText);
      TAGView tagView = new TAGView(store).setMarkupToInclude(includedTags);
      TAGComparison comparison = new TAGComparison(original, tagView, edited);
      comparison.mergeChanges();
      String editedOriginText = new LMNLExporter(store).toLMNL(original);
      assertThat(editedOriginText).isEqualTo(expectedLmnl);
    });
  }

  private TAGComparison compare(String originText, String editedText) {
    return store.runInTransaction(() -> {
      LMNLImporter importer = new LMNLImporter(store);
      DocumentWrapper original = importer.importLMNL(originText);
      DocumentWrapper edited = importer.importLMNL(editedText);
      Set<String> none = Collections.EMPTY_SET;
      TAGView allTags = new TAGView(store).setMarkupToExclude(none);

      TAGComparison comparison = new TAGComparison(original, allTags, edited);
      LOG.info("diffLines = \n{}", comparison.getDiffLines()//
          .stream()//
          .map(l -> "'" + l + "'")//
          .collect(joining("\n")));
      return comparison;
    });
  }
}
