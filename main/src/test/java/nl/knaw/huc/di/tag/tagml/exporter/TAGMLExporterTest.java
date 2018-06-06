package nl.knaw.huc.di.tag.tagml.exporter;

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
import nl.knaw.huygens.alexandria.storage.TAGStoreTest;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import org.junit.Ignore;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGMLExporterTest extends TAGStoreTest {

  @Test
  public void testEscapedCharacters() {
    String tagmlIn = "[line>\\[, \\< and \\\\ need to be escaped.<line]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testMarkedUpText() {
    String tagmlIn = "[line>The rain in Spain falls mainly on the plain.<line]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testLinearAnnotatedText() {
    String tagmlIn = "[a>I've got a [b>bad<b] feeling about [c>this<c].<a]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testNonLinearAnnotatedText() {
      String tagmlIn = "[a>I've got a <|very [b>bad<b]|exceptionally good|> feeling about [c>this<c].<a]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testNestedNonLinearity() {
    String tagmlIn = "[l>This is <|" +
        "[del>great stuff!<del]" +
        "|" +
        "[add>questionable <|[del>text<del]|[add>code<add]|><add]" +
        "|><l]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testDoublyNestedNonLinearity() {
    String tagmlIn = "[l>This is <|" +
        "[del>great stuff!<del]" +
        "|" +
        "[add>questionable <|" +
        "[del>text<del]" +
        "|" +
        "[add>but readable <|" +
        "[del>cdoe<del]|[add>code<add]" +
        "|><add]" +
        "|><add]" +
        "|><l]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testOptionalTags() {
    String tagmlIn = "[l>This [?w>word<?w] is optional.<l]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testDiscontinuity() {
    String tagmlIn = "[x>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<x]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Ignore
  @Test
  public void testCombiningOverlapAndNonLinearity1() {
    String tagmlIn = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<b]<add]" +
        "|[b>[del>rich<del]|>" +
        " man <b] is in need of a maid.<text]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testCombiningOverlapAndNonLinearity2() {
    String tagmlIn = "[text>It is a truth universally acknowledged that every " +
        "<|[add>young [b>woman<b]<add]" +
        "|[b>[del>rich<del]<b]|>" +
        " [b>man<b] is in need of a maid.<text]";
    assertTAGMLOutIsIn(tagmlIn);
  }

  @Test
  public void testCombiningDiscontinuityAndNonLinearity1() {
    String tagML = "[x>[q>and what is the use of a " +
        "<|[del>book,<del]" +
        "|<-q][add>thought Alice<add][+q>|>" +
        "without pictures or conversation?<q]<x]";
    assertTAGMLOutIsIn(tagML);
  }
  // --- private methods ---

  private void assertTAGMLOutIsIn(final String tagmlIn) {
    store.runInTransaction(() -> {
      TAGDocument document = new TAGMLImporter(store).importTAGML(tagmlIn);
      String tagmlOut = new TAGMLExporter(store).asTAGML(document);
      System.out.println(tagmlOut);
      assertThat(tagmlOut).isEqualTo(tagmlIn);
    });
  }

}
