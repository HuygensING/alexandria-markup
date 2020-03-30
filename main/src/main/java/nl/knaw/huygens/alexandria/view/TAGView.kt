package nl.knaw.huygens.alexandria.view

import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.view.TAGView.RelevanceStyle.*
import java.util.*
import java.util.stream.Collectors

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

class TAGView(private val store: TAGStore) {

    internal enum class RelevanceStyle {
        INCLUDE, EXCLUDE, UNDEFINED
    }

    val isValid: Boolean
        get() = !(layerRelevance == UNDEFINED && markupRelevance == UNDEFINED)

    private var layerRelevance = UNDEFINED
    private var markupRelevance = UNDEFINED

    var layersToInclude: Set<String> = HashSet()
        set(layers) {
            if (layerStyleIsExclude()) {
                throw RuntimeException("This TAGView already has set layersToExclude")
            }
            field = layers
            this.layerRelevance = RelevanceStyle.INCLUDE
        }

    var layersToExclude: Set<String> = HashSet()
        set(layers) {
            if (layerStyleIsInclude()) {
                throw RuntimeException("This TAGView already has set layersToInclude")
            }
            field = layers
            this.layerRelevance = RelevanceStyle.EXCLUDE

        }

    var markupToInclude: Set<String> = HashSet()
        set(markup) {
            if (markupStyleIsExclude()) {
                throw RuntimeException("This TAGView already has set markupToExclude")
            }
            field = markup
            this.markupRelevance = RelevanceStyle.INCLUDE
        }

    var markupToExclude: Set<String> = HashSet()
        set(markup) {
            if (markupStyleIsInclude()) {
                throw RuntimeException("This TAGView already has set markupToInclude")
            }
            field = markup
            this.markupRelevance = RelevanceStyle.EXCLUDE
        }

    var markupWithLayerExclusiveText: Set<String> = HashSet()

    fun filterRelevantLayers(layerNames: Set<String>): Set<String> {
        val relevantLayers: MutableSet<String> = HashSet(layerNames)
        val nonRelevantLayers = when (layerRelevance) {
            INCLUDE -> {
                val layers = layerNames.toMutableSet()
                layers.removeAll(layersToInclude)
                layers
            }
            EXCLUDE -> {
                layersToExclude.toMutableSet()
            }
            else -> {
                mutableSetOf()
            }
        }
        relevantLayers.removeAll(nonRelevantLayers)
        return relevantLayers
    }

    fun filterRelevantMarkup(markupIds: Set<Long>): Set<Long> {
        val relevantMarkupIds: MutableSet<Long> = LinkedHashSet(markupIds)
        if (layerStyleIsInclude()) {
            val retain = markupIds.stream()
                    .filter { m: Long -> hasOverlap(layersToInclude, getLayersForMarkup(m)) }
                    .collect(Collectors.toList())
            relevantMarkupIds.retainAll(retain)
        } else if (layerStyleIsExclude()) {
            val remove = markupIds.stream()
                    .filter { m: Long -> hasOverlap(layersToExclude, getLayersForMarkup(m)) } //
                    .collect(Collectors.toList())
            relevantMarkupIds.removeAll(remove)
        }
        if (markupStyleIsInclude()) {
            val retain = markupIds.stream().filter { m: Long -> markupToInclude.contains(loadTag(m)) }.collect(Collectors.toList())
            relevantMarkupIds.retainAll(retain)
        } else if (markupStyleIsExclude()) {
            val remove = markupIds.stream().filter { m: Long -> markupToExclude.contains(loadTag(m)) }.collect(Collectors.toList())
            relevantMarkupIds.removeAll(remove)
        }
        return relevantMarkupIds
    }

    fun textIsRelevant(markupIds: Set<Long>): Boolean =
            markupWithLayerExclusiveText.isEmpty() ||
                    markupIds.stream()
                            .map { store.getMarkup(it) }
                            .noneMatch { markupWithIgnoredTextForThisView(it) }

    private fun markupWithIgnoredTextForThisView(tagMarkup: TAGMarkup): Boolean =
            // Ignore contained text if the tag name is one to watch out for,
            markupWithLayerExclusiveText.contains(tagMarkup.tag)
                    // and none of the layers is relevant for this view
                    && filterRelevantLayers(tagMarkup.layers).isEmpty()

//    private boolean isInDefaultLayerOnly(final Long markupId) {
//        return getLayers(markupId).stream().anyMatch(TAGML.DEFAULT_LAYER::equals);
//    }

    private fun isInDefaultLayerOnly(markupId: Long): Boolean {
        val layers = getLayersForMarkup(markupId)
        return layers.size == 1 && layers.iterator().next() == DEFAULT_LAYER
    }

    private fun hasOverlap(layersToInclude: Set<String>, layers: Set<String>): Boolean {
        val overlap: MutableSet<String> = HashSet(layers)
        overlap.retainAll(layersToInclude)
        return overlap.isNotEmpty()
    }

    private fun getLayersForMarkup(markupId: Long): Set<String> =
            store.getMarkup(markupId).layers

    val definition: TAGViewDefinition
        get() = TAGViewDefinition()
                .withIncludeLayers(layersToInclude)
                .withExcludeLayers(layersToExclude)
                .withIncludeMarkup(markupToInclude)
                .withExcludeMarkup(markupToExclude)
                .withMarkupWithLayerExclusiveText(markupWithLayerExclusiveText)

    fun markupStyleIsInclude(): Boolean = markupRelevance == RelevanceStyle.INCLUDE

    fun markupStyleIsExclude(): Boolean = markupRelevance == RelevanceStyle.EXCLUDE

    fun layerStyleIsInclude(): Boolean = layerRelevance == RelevanceStyle.INCLUDE

    fun layerStyleIsExclude(): Boolean = layerRelevance == RelevanceStyle.EXCLUDE

    fun isIncluded(tagMarkup: TAGMarkup): Boolean {
        val tag = tagMarkup.tag
        return if (markupStyleIsInclude()) {
            markupToInclude.contains(tag)
        } else RelevanceStyle.EXCLUDE == markupRelevance && !markupToExclude.contains(tag)
    }

    private fun loadTag(markupId: Long): String {
        return store.getMarkup(markupId).tag
    }

    fun withMarkupToInclude(markup: Set<String>): TAGView {
        markupToInclude = markup
        return this
    }

    fun withMarkupToExclude(markup: Set<String>): TAGView {
        markupToExclude = markup
        return this
    }

    fun withLayersToInclude(layers: Set<String>): TAGView {
        layersToInclude = layers
        return this
    }

}
