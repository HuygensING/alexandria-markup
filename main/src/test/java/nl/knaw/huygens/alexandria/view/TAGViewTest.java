package nl.knaw.huygens.alexandria.view;

/*
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

import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huc.di.tag.tagml.importer.TAGModelBuilder;
import nl.knaw.huc.di.tag.tagml.importer.TAGModelBuilderImpl;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import nl.knaw.huygens.alexandria.storage.TAGDocumentDAO;
import nl.knaw.huygens.alexandria.storage.TAGMarkupDAO;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.assertj.core.util.Sets;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class TAGViewTest extends AlexandriaBaseStoreTest {

  @Test
  public void testDefaultLayerIsAlwaysIncludedInInclusiveLayerView() {
    String tagml = "[tagml>[layerdef|+A,+B>[x|A>C'est [x|B>combien<x|A], cette [b|A>six<b|A]<x|B] <|saucissons|croissants|>-ci?<layerdef]<tagml]";
    String viewJson = "{'includeLayers':['A']}".replace("'", "\"");
    String expected = "[tagml>[layerdef|+A,+B>[x|A>C'est combien<x|A], cette [b|A>six<b|A] <|saucissons|croissants|>-ci?<layerdef|A,B]<tagml]";
    runInStoreTransaction(store -> {
      TAGViewFactory tagViewFactory = new TAGViewFactory(store);
      TAGView view = tagViewFactory.fromJsonString(viewJson);
      final TAGModelBuilder tagModelBuilder = new TAGModelBuilderImpl(store, new ErrorListener());
      TAGDocumentDAO document = store.runInTransaction(() ->
          new TAGMLImporter().importTAGML(tagModelBuilder,tagml)
      );
      String viewExport = store.runInTransaction(() -> new TAGMLExporter(store, view).asTAGML(document));
      assertThat(viewExport).isEqualTo(expected);
    });
  }

  @Test
  public void testDefaultLayerIsAlwaysIncludedInExclusiveLayerView() {
    String tagml = "[tagml>[layerdef|+A,+B>[x|A>C'est [x|B>combien<x|A], cette [b|A>six<b|A]<x|B] <|saucissons|croissants|>-ci?<layerdef]<tagml]";
    String viewJson = "{'excludeLayers':['B']}".replace("'", "\"");
    String expected = "[tagml>[x|A>C'est combien<x|A], cette [b|A>six<b|A] <|saucissons|croissants|>-ci?<tagml]";
    runInStoreTransaction(store -> {
      TAGViewFactory tagViewFactory = new TAGViewFactory(store);
      TAGView view = tagViewFactory.fromJsonString(viewJson);
      final TAGModelBuilder tagModelBuilder = new TAGModelBuilderImpl(store, new ErrorListener());
      TAGDocumentDAO document = store.runInTransaction(() -> new TAGMLImporter().importTAGML(tagModelBuilder,tagml));
      String viewExport = store.runInTransaction(() -> new TAGMLExporter(store, view).asTAGML(document));
      assertThat(viewExport).isEqualTo(expected);
    });
  }

  @Test // NLA-489
  public void testLayerMarkupCombinationInView() {
    String tagml = "[tagml>[layerdef|+A,+B>[x|A>C'est [x|B>combien<x|A], cette [b|A>six<b|A]<x|B] saucissons-ci?<layerdef]<tagml]";
    String viewJson = "{'includeLayers':['A'],'excludeMarkup':['b']}".replace("'", "\"");
    String expected = "[tagml>[layerdef|+A,+B>[x|A>C'est combien<x|A], cette six saucissons-ci?<layerdef|A,B]<tagml]";
    runInStoreTransaction(store -> {
      TAGViewFactory tagViewFactory = new TAGViewFactory(store);
      TAGView view = tagViewFactory.fromJsonString(viewJson);
      final TAGModelBuilder tagModelBuilder = new TAGModelBuilderImpl(store, new ErrorListener());
      TAGDocumentDAO document = store.runInTransaction(() -> new TAGMLImporter().importTAGML(tagModelBuilder,tagml));
      String viewExport = store.runInTransaction(() -> new TAGMLExporter(store, view).asTAGML(document));
      assertThat(viewExport).isEqualTo(expected);
    });
  }

  @Test
  public void testFilterRelevantMarkup0() {
    runInStoreTransaction(store -> {
      TAGDocumentDAO document = store.createDocument();

      String layer1 = "L1";
      String layer2 = "L2";

      String tag1 = "a";
      Long markupId1 = createNewMarkup(document, tag1, layer1, store);

      String tag2 = "b";
      Long markupId2 = createNewMarkup(document, tag2, layer2, store);

      String tag3 = "c";
      Long markupId3 = createNewMarkup(document, tag3, layer1, store);

      String tag4 = "d";
      Long markupId4 = createNewMarkup(document, tag4, layer2, store);

      Set<Long> allMarkupIds = new HashSet<>(asList(markupId1, markupId2, markupId3, markupId4));

      Set<String> l1 = new HashSet<>(Collections.singletonList(layer1));
      Set<String> l2 = new HashSet<>(Collections.singletonList(layer2));

      TAGView viewNoL1 = new TAGView(store).setLayersToExclude(l1);

      Set<Long> filteredMarkupIds = viewNoL1.filterRelevantMarkup(allMarkupIds);
      assertThat(filteredMarkupIds).containsExactlyInAnyOrder(markupId2, markupId4);

      TAGView viewL2 = new TAGView(store).setLayersToInclude(l2);

      Set<Long> filteredMarkupIds2 = viewL2.filterRelevantMarkup(allMarkupIds);
      assertThat(filteredMarkupIds2).containsExactlyInAnyOrder(markupId2, markupId4);

      TAGView viewL1 = new TAGView(store).setLayersToInclude(l1);

      Set<Long> filteredMarkupIds3 = viewL1.filterRelevantMarkup(allMarkupIds);
      assertThat(filteredMarkupIds3).containsExactlyInAnyOrder(markupId1, markupId3);

      final TAGModelBuilder tagModelBuilder = new TAGModelBuilderImpl(store, new ErrorListener());
      TAGMLImporter importer = new TAGMLImporter();
      TAGDocumentDAO document1 = importer.importTAGML(tagModelBuilder,"[tagml|+L1,+L2>[a|L1>a[b|L2>b[c|L1>c[d|L2>da<c]b<d]c<a]d<b]<tagml]");

      TAGMLExporter exporter1 = new TAGMLExporter(store, viewNoL1);
      String tagmlBD = exporter1.asTAGML(document1);
      assertThat(tagmlBD).isEqualTo("a[b|L2>bc[d|L2>dab<d|L2]cd<b|L2]");

      TAGMLExporter exporter2 = new TAGMLExporter(store, viewL1);
      String tagmlAC = exporter2.asTAGML(document1);
      assertThat(tagmlAC).isEqualTo("[tagml|+L1,+L2>[a|L1>ab[c|L1>cda<c|L1]bc<a|L1]d<tagml|L1,L2]");

      TAGView viewL1NoC = new TAGView(store)
          .setLayersToInclude(l1)
          .setMarkupToExclude(Sets.newLinkedHashSet(tag3));
      TAGMLExporter exporter3 = new TAGMLExporter(store, viewL1NoC);
      String tagmlA = exporter3.asTAGML(document1);
      assertThat(tagmlA).isEqualTo("[tagml|+L1,+L2>[a|L1>abcdabc<a|L1]d<tagml|L1,L2]");

      TAGView viewNoL1B = new TAGView(store)
          .setLayersToExclude(l1)
          .setMarkupToInclude(Sets.newLinkedHashSet(tag2));
      TAGMLExporter exporter4 = new TAGMLExporter(store, viewNoL1B);
      String tagmlB = exporter4.asTAGML(document1);
      assertThat(tagmlB).isEqualTo("a[b|L2>bcdabcd<b|L2]");

    });
  }

  @Ignore
  @Test
  public void testFilterRelevantMarkup() {
    runInStoreTransaction(store -> {
      TAGDocumentDAO document = store.createDocument();

      String tag1 = "a";
      Long markupId1 = createNewMarkup(document, tag1, store);

      String tag2 = "b";
      Long markupId2 = createNewMarkup(document, tag2, store);

      String tag3 = "c";
      Long markupId3 = createNewMarkup(document, tag3, store);

      String tag4 = "d";
      Long markupId4 = createNewMarkup(document, tag4, store);

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
      TAGDocumentDAO document1 = importer.importLMNL("[a}a[b}b[c}c[d}da{a]b{b]c{c]d{d]");

      LMNLExporter exporter1 = new LMNLExporter(store, viewNoAC);
      String lmnlBD = exporter1.toLMNL(document1);
      assertThat(lmnlBD).isEqualTo("a[b}bc[d}dab{b]cd{d]");

      LMNLExporter exporter2 = new LMNLExporter(store, viewAC);
      String lmnlAC = exporter2.toLMNL(document1);
      assertThat(lmnlAC).isEqualTo("[a}ab[c}cda{a]bc{c]d");
    });
  }

  private Long createNewMarkup(TAGDocumentDAO document, String tag1, final TAGStore store) {
    return store.createMarkup(document, tag1).getDbId();
  }

  private Long createNewMarkup(TAGDocumentDAO document, String tag1, String layer, final TAGStore store) {
    TAGMarkupDAO markup = store.createMarkup(document, tag1);
    markup.getLayers().add(layer);
    store.update(markup);
    return markup.getDbId();
  }

}
