package nl.knaw.huygens.alexandria.view

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.assertj.core.util.Sets
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class TAGViewTest : AlexandriaBaseStoreTest() {

    //  @Disabled("Should the default layer always be included?")
    @Test
    fun testDefaultLayerIsAlwaysIncludedInInclusiveLayerView() {
        val tagml = "[tagml>[layerdef|+A,+B>[x|A>C'est [x|B>combien<x|A], cette [b|A>six<b|A]<x|B] <|saucissons|croissants|>-ci?<layerdef]<tagml]"
        val viewJson = """{
          |"includeLayers" : ["A"]
          |}""".trimMargin()
        val expected = "[tagml>[layerdef|+A,+B>[x|A>C'est combien<x|A], cette [b|A>six<b|A] <|saucissons|croissants|>-ci?<layerdef|A,B]<tagml]"
        runInStore { store: TAGStore ->
            val tagViewFactory = TAGViewFactory(store)
            val view = tagViewFactory.fromJsonString(viewJson)
            val softly = SoftAssertions()
            with(softly) {
                assertThat(view.markupStyleIsInclude()).isFalse()
                assertThat(view.markupStyleIsExclude()).isFalse()
                assertThat(view.layerStyleIsInclude()).isTrue()
                assertThat(view.layerStyleIsExclude()).isFalse()
                assertAll()
            }
            val document = store.runInTransaction<TAGDocument> { TAGMLImporter(store).importTAGML(tagml) }
            val viewExport = store.runInTransaction<String> { TAGMLExporter(store, view).asTAGML(document) }
            assertThat(viewExport).isEqualTo(expected)
        }
    }

    //  @Disabled("Should the default layer always be included?")
    @Test
    fun testDefaultLayerIsAlwaysIncludedInExclusiveLayerView() {
        val tagml = "[tagml>[layerdef|+A,+B>[x|A>C'est [x|B>combien<x|A], cette [b|A>six<b|A]<x|B] <|saucissons|croissants|>-ci?<layerdef]<tagml]"
        val viewJson = """{
          |"excludeLayers" : ["B"]
          |}""".trimMargin()
        val expected = "[tagml>[layerdef|+A,+B>[x|A>C'est combien<x|A], cette [b|A>six<b|A] <|saucissons|croissants|>-ci?<layerdef|A,B]<tagml]"
        runInStore { store: TAGStore ->
            val tagViewFactory = TAGViewFactory(store)
            val view = tagViewFactory.fromJsonString(viewJson)
            val softly = SoftAssertions()
            with(softly) {
                assertThat(view.markupStyleIsInclude()).isFalse()
                assertThat(view.markupStyleIsExclude()).isFalse()
                assertThat(view.layerStyleIsInclude()).isFalse()
                assertThat(view.layerStyleIsExclude()).isTrue()
                assertAll()
            }
            val document = store.runInTransaction<TAGDocument> { TAGMLImporter(store).importTAGML(tagml) }
            val viewExport = store.runInTransaction<String> { TAGMLExporter(store, view).asTAGML(document) }
            assertThat(viewExport).isEqualTo(expected)
        }
    }

    @Disabled("Should the default layer always be included?")
    @Test // NLA-489
    fun testLayerMarkupCombinationInView() {
        val tagml = "[tagml>[layerdef|+A,+B>[x|A>C'est [x|B>combien<x|A], cette [b|A>six<b|A]<x|B] saucissons-ci?<layerdef]<tagml]"
        val viewJson = """{
          |"includeLayers" : ["A"],
          |"excludeMarkup" : ["b"]
          |}""".trimMargin()
        val expected = "[tagml>[layerdef|+A,+B>[x|A>C'est combien<x|A], cette six saucissons-ci?<layerdef|A,B]<tagml]"
        runInStore { store: TAGStore ->
            val tagViewFactory = TAGViewFactory(store)
            val view = tagViewFactory.fromJsonString(viewJson)
            val document = store.runInTransaction<TAGDocument> { TAGMLImporter(store).importTAGML(tagml) }
            val viewExport = store.runInTransaction<String> { TAGMLExporter(store, view).asTAGML(document) }
            assertThat(viewExport).isEqualTo(expected)
        }
    }

    //  @Disabled("Should the default layer always be included?")
    @Test
    fun testFilterRelevantMarkup0() {
        runInStoreTransaction { store: TAGStore ->
            val document = store.createDocument()
            val layer1 = "L1"
            val layer2 = "L2"
            val tag1 = "a"
            val markupId1 = Companion.createNewMarkup(document, tag1, layer1, store)
            val tag2 = "b"
            val markupId2 = Companion.createNewMarkup(document, tag2, layer2, store)
            val tag3 = "c"
            val markupId3 = Companion.createNewMarkup(document, tag3, layer1, store)
            val tag4 = "d"
            val markupId4 = Companion.createNewMarkup(document, tag4, layer2, store)
            val allMarkupIds: Set<Long> = setOf(markupId1, markupId2, markupId3, markupId4)
            val l1: Set<String> = setOf(layer1)
            val l2: Set<String> = setOf(layer2)
            val viewNoL1 = TAGView(store).withLayersToExclude(l1)
            val filteredMarkupIds = viewNoL1.filterRelevantMarkup(allMarkupIds)
            assertThat(filteredMarkupIds).containsExactlyInAnyOrder(markupId2, markupId4)

            val viewL2 = TAGView(store).withLayersToInclude(l2)
            val filteredMarkupIds2 = viewL2.filterRelevantMarkup(allMarkupIds)
            assertThat(filteredMarkupIds2).containsExactlyInAnyOrder(markupId2, markupId4)

            val viewL1 = TAGView(store).withLayersToInclude(l1)
            val filteredMarkupIds3 = viewL1.filterRelevantMarkup(allMarkupIds)
            assertThat(filteredMarkupIds3).containsExactlyInAnyOrder(markupId1, markupId3)

            val importer = TAGMLImporter(store)
            val document1 = importer.importTAGML("[tagml|+L1,+L2>[a|L1>a[b|L2>b[c|L1>c[d|L2>da<c]b<d]c<a]d<b]<tagml]")
            val exporter1 = TAGMLExporter(store, viewNoL1)
            val tagmlBD = exporter1.asTAGML(document1)
            assertThat(tagmlBD).isEqualTo("[tagml|+L1,+L2>a[b|L2>bc[d|L2>dab<d|L2]cd<b|L2]<tagml|L1,L2]")

            val exporter2 = TAGMLExporter(store, viewL1)
            val tagmlAC = exporter2.asTAGML(document1)
            assertThat(tagmlAC).isEqualTo("[tagml|+L1,+L2>[a|L1>ab[c|L1>cda<c|L1]bc<a|L1]d<tagml|L1,L2]")

            val viewL1NoC = TAGView(store)
                    .withLayersToInclude(l1)
                    .withMarkupToExclude(Sets.newLinkedHashSet(tag3))
            val exporter3 = TAGMLExporter(store, viewL1NoC)
            val tagmlA = exporter3.asTAGML(document1)
            assertThat(tagmlA).isEqualTo("[tagml|+L1,+L2>[a|L1>abcdabc<a|L1]d<tagml|L1,L2]")

            val viewNoL1B = TAGView(store)
                    .withLayersToExclude(l1)
                    .withMarkupToInclude(Sets.newLinkedHashSet(tag2))
            val exporter4 = TAGMLExporter(store, viewNoL1B)
            val tagmlB = exporter4.asTAGML(document1)
            assertThat(tagmlB).isEqualTo("a[b|L2>bcdabcd<b|L2]")
        }
    }

    @Disabled
    @Test
    fun testFilterRelevantMarkup() {
        runInStoreTransaction { store: TAGStore ->
            val document = store.createDocument()
            val tag1 = "a"
            val markupId1 = Companion.createNewMarkup(document, tag1, store)
            val tag2 = "b"
            val markupId2 = Companion.createNewMarkup(document, tag2, store)
            val tag3 = "c"
            val markupId3 = Companion.createNewMarkup(document, tag3, store)
            val tag4 = "d"
            val markupId4 = Companion.createNewMarkup(document, tag4, store)
            val allMarkupIds: Set<Long> = setOf(markupId1, markupId2, markupId3, markupId4)
            val odds: Set<String> = setOf(tag1, tag3)
            val evens: Set<String> = setOf(tag2, tag4)
            val viewNoAC = TAGView(store).withMarkupToExclude(odds)
            val filteredMarkupIds = viewNoAC.filterRelevantMarkup(allMarkupIds)
            assertThat(filteredMarkupIds).containsExactlyInAnyOrder(markupId2, markupId4)

            val viewBD = TAGView(store).withMarkupToInclude(evens)
            val filteredMarkupIds2 = viewBD.filterRelevantMarkup(allMarkupIds)
            assertThat(filteredMarkupIds2).containsExactlyInAnyOrder(markupId2, markupId4)

            val viewAC = TAGView(store).withMarkupToInclude(odds)
            val filteredMarkupIds3 = viewAC.filterRelevantMarkup(allMarkupIds)
            assertThat(filteredMarkupIds3).containsExactlyInAnyOrder(markupId1, markupId3)

            val importer = LMNLImporter(store)
            val document1 = importer.importLMNL("[a}a[b}b[c}c[d}da{a]b{b]c{c]d{d]")
            val exporter1 = LMNLExporter(store, viewNoAC)
            val lmnlBD = exporter1.toLMNL(document1)
            assertThat(lmnlBD).isEqualTo("a[b}bc[d}dab{b]cd{d]")

            val exporter2 = LMNLExporter(store, viewAC)
            val lmnlAC = exporter2.toLMNL(document1)
            assertThat(lmnlAC).isEqualTo("[a}ab[c}cda{a]bc{c]d")
        }
    }

    companion object {
        private fun createNewMarkup(document: TAGDocument, tag1: String, layer: String, store: TAGStore): Long {
            val markup = store.createMarkup(document, tag1)
            markup.layers.add(layer)
            store.persist(markup.dto)
            return markup.dbId
        }

        private fun createNewMarkup(document: TAGDocument, tag1: String, store: TAGStore): Long {
            return store.createMarkup(document, tag1).dbId
        }
    }
}
