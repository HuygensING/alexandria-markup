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

import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.view.TAGView.RelevanceStyle.*
import java.util.*

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
            field = layers + DEFAULT_LAYER
            this.layerRelevance = INCLUDE
        }

    var layersToExclude: Set<String> = HashSet()
        set(layers) {
            if (layerStyleIsInclude()) {
                throw RuntimeException("This TAGView already has set layersToInclude")
            }
            field = layers
            this.layerRelevance = EXCLUDE

        }

    var markupToInclude: Set<String> = HashSet()
        set(markup) {
            if (markupStyleIsExclude()) {
                throw RuntimeException("This TAGView already has set markupToExclude")
            }
            field = markup
            this.markupRelevance = INCLUDE
        }

    var markupToExclude: Set<String> = HashSet()
        set(markup) {
            if (markupStyleIsInclude()) {
                throw RuntimeException("This TAGView already has set markupToInclude")
            }
            field = markup
            this.markupRelevance = EXCLUDE
        }

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
        relevantLayers.add(DEFAULT_LAYER)
        return relevantLayers.toSet()
    }

    fun filterRelevantMarkup(markupIds: Set<Long>): Set<Long> {
        val relevantMarkupIds: MutableSet<Long> = LinkedHashSet(markupIds)
        when (layerRelevance) {
            INCLUDE -> {
                val retain = markupIds
                        .filter { m: Long -> layersToInclude.overlapsWith(getLayersForMarkup(m)) }
                relevantMarkupIds.retainAll(retain)
            }
            EXCLUDE -> {
                // remove all markup whose layers are all in the layersToExclude
                val remove = markupIds
                        .filter { m: Long -> layersToExclude.containsAll(getLayersForMarkup(m)) }
                relevantMarkupIds.removeAll(remove)
            }
            UNDEFINED -> {
                // no action needed
            }
        }

        when (markupRelevance) {
            INCLUDE -> {
                val retain = markupIds.filter { m: Long -> loadTag(m) in markupToInclude }
                relevantMarkupIds.retainAll(retain)
            }
            EXCLUDE -> {
                val remove = markupIds.filter { m: Long -> loadTag(m) in markupToExclude }
                relevantMarkupIds -= remove
            }
            UNDEFINED -> {
                // no action needed
            }
        }

        return relevantMarkupIds
    }

    private fun isInDefaultLayerOnly(markupId: Long): Boolean {
        val layers = getLayersForMarkup(markupId)
        return layers.size == 1 && layers.iterator().next() == DEFAULT_LAYER
    }

    private fun Set<String>.overlapsWith(other: Set<String>): Boolean {
        val overlap: MutableSet<String> = other.toMutableSet()
        overlap.retainAll(this)
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

    private fun markupStyleIsInclude() = markupRelevance == INCLUDE

    private fun markupStyleIsExclude() = markupRelevance == EXCLUDE

    private fun layerStyleIsInclude() = layerRelevance == INCLUDE

    private fun layerStyleIsExclude() = layerRelevance == EXCLUDE

    fun isIncluded(tagMarkup: TAGMarkup): Boolean {
        val tag = tagMarkup.tag
        return if (markupStyleIsInclude()) {
            tag in markupToInclude
        } else EXCLUDE == markupRelevance && tag !in markupToExclude
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

    fun withLayersToExclude(layers: Set<String>): TAGView {
        layersToExclude = layers
        return this
    }

}
