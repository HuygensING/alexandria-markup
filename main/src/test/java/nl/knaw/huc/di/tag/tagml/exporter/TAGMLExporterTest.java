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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGMLExporterTest extends TAGStoreTest {

  @Test
  public void testLinearAnnotatedText() {
    store.runInTransaction(() -> {
      String tagmlIn = "[a>I've got a [b>bad<b] feeling about [c>this<c].<a]";
      TAGMLImporter importer = new TAGMLImporter(store);
      DocumentWrapper documentWrapper = importer.importTAGML(tagmlIn);
      TAGMLExporter exporter = new TAGMLExporter();
      String tagmlOut = exporter.asTAGML(documentWrapper);
      System.out.println(tagmlOut);
      assertThat(tagmlOut).isEqualTo(tagmlIn);
    });
  }

  @Test
  public void testNonLinearAnnotatedText() {
    store.runInTransaction(() -> {
      String tagmlIn = "[a>I've got a <|very [b>bad<b]|exceptionally good|> feeling about [c>this<c].<a]";
      TAGMLImporter importer = new TAGMLImporter(store);
      DocumentWrapper documentWrapper = importer.importTAGML(tagmlIn);
      TAGMLExporter exporter = new TAGMLExporter();
      String tagmlOut = exporter.asTAGML(documentWrapper);
      System.out.println(tagmlOut);
      assertThat(tagmlOut).isEqualTo(tagmlIn);
    });
  }
}
