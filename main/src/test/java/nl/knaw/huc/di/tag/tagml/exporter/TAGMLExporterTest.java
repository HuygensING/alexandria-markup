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
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGMLExporterTest extends TAGStoreTest {

  public static final TAGView SHOW_ALL_MARKUP_VIEW = TAGViews.getShowAllMarkupView(store);


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
  public void testOpenMarkupInNonLinearAnnotatedText() {
    String tagmlIn = "[l>I'm <|done.<l][l>|ready.|finished.|> Let's go!.<l]";
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

  private void assertTAGMLOutIsIn(final String tagmlIn) {
    store.runInTransaction(() -> {
      DocumentWrapper documentWrapper = new TAGMLImporter(store).importTAGML(tagmlIn);
      String tagmlOut = new TAGMLExporter(SHOW_ALL_MARKUP_VIEW).asTAGML(documentWrapper);
      System.out.println(tagmlOut);
      assertThat(tagmlOut).isEqualTo(tagmlIn);
    });
  }

}
