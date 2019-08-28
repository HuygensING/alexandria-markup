package nl.knaw.huc.di.tag.tagml;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class MarkupPathTest extends AlexandriaBaseStoreTest {
  @Test
  public void test1() {
    String tagml = "[a>Uno [b>Dos [c>Tres<c]<b] Quatro<a]";
    runInStoreTransaction(store -> {
      TAGDocument tagDocument = parse(tagml, store);

      final List<TAGMarkup> markups = getTagMarkups(tagDocument);
      assertThat(markups).hasSize(3);

      final TAGMarkup a = markups.get(0);
      assertTagAndPath(a, "a", "a", store, tagDocument);

      final TAGMarkup b = markups.get(1);
      assertTagAndPath(b, "b", "a/b[1]", store, tagDocument);

      final TAGMarkup c = markups.get(2);
      assertTagAndPath(c, "c", "a/b[1]/c[1]", store, tagDocument);
    });
  }

  @Test
  public void test2() {
    String tagml = "[a>[l>line 1<l] [l>line 2<l]<a]";
    runInStoreTransaction(store -> {
      TAGDocument tagDocument = parse(tagml, store);

      final List<TAGMarkup> markups = getTagMarkups(tagDocument);
      assertThat(markups).hasSize(3);

      final TAGMarkup a = markups.get(0);
      assertTagAndPath(a, "a", "a", store, tagDocument);

      final TAGMarkup l1 = markups.get(1);
      assertTagAndPath(l1, "l", "a/l[1]", store, tagDocument);

      final TAGMarkup l2 = markups.get(2);
      assertTagAndPath(l2, "l", "a/l[2]", store, tagDocument);
    });
  }

  @Test
  public void test3() {
    String tagml = "[a|+L>[l|L>line 1<l] [l|L>line 2<l]<a]";
    runInStoreTransaction(store -> {
      TAGDocument tagDocument = parse(tagml, store);

      final List<TAGMarkup> markups = getTagMarkups(tagDocument);
      assertThat(markups).hasSize(3);

      final TAGMarkup a = markups.get(0);
      assertTagAndPath(a, "a", "a|L", store, tagDocument);

      final TAGMarkup l1 = markups.get(1);
      assertTagAndPath(l1, "l", "a/l[1]|L", store, tagDocument);

      final TAGMarkup l2 = markups.get(2);
      assertTagAndPath(l2, "l", "a/l[2]|L", store, tagDocument);
    });
  }

  @Test
  public void test4() {
    String tagml = "[a|+B,+C>[b|B>bbbb<b] [c|C>ccccc<c]<a]";
    runInStoreTransaction(store -> {
      TAGDocument tagDocument = parse(tagml, store);

      final List<TAGMarkup> markups = getTagMarkups(tagDocument);
      assertThat(markups).hasSize(3);

      final TAGMarkup a = markups.get(0);
      assertTagAndPath(a, "a", "a|B", store, tagDocument);

      final TAGMarkup b = markups.get(1);
      assertTagAndPath(b, "b", "a/b|B", store, tagDocument);

      final TAGMarkup c = markups.get(2);
      assertTagAndPath(c, "c", "a/c|C", store, tagDocument);
    });
  }

  private void assertTagAndPath(final TAGMarkup tagMarkup, final String expectedTag, final String expectedPath,
      final TAGStore store, final TAGDocument tagDocument) {
    assertThat(tagMarkup.getTag()).isEqualTo(expectedTag);
    MarkupPath pathA = new MarkupPath(tagMarkup, tagDocument, store);
    assertThat(pathA.getPath()).isEqualTo(expectedPath);
  }

  private List<TAGMarkup> getTagMarkups(final TAGDocument tagDocument) {
    return tagDocument.getMarkupStream().collect(Collectors.toList());
  }

  private TAGDocument parse(final String tagml, final TAGStore store) {
    TAGMLImporter importer = new TAGMLImporter(store);
    return importer.importTAGML(tagml);
  }

}
