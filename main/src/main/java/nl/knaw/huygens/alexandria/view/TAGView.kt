package nl.knaw.huygens.alexandria.view

import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
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
    val isValid: Boolean
        get() = !(layerRelevance == RelevanceStyle.undefined && markupRelevance == RelevanceStyle.undefined)

    internal enum class RelevanceStyle {
        include, exclude, undefined
    }

    private var layerRelevance = RelevanceStyle.undefined
    private var markupRelevance = RelevanceStyle.undefined
    var layersToInclude: Set<String?>? = HashSet()
        internal set
    var layersToExclude: Set<String?>? = HashSet()
        internal set
    var markupToInclude: Set<String?>? = HashSet()
        internal set
    var markupToExclude: Set<String?>? = HashSet()
        internal set

    fun filterRelevantLayers(layerNames: Set<String?>?): Set<String?> {
        val relevantLayers: MutableSet<String?> = HashSet(layerNames)
        if (layerRelevance == RelevanceStyle.include) {
            relevantLayers.clear()
            relevantLayers.addAll(layersToInclude!!)
            //      boolean hasDefault = layerNames.contains(TAGML.DEFAULT_LAYER);
            //      if (hasDefault) {
            //        relevantLayers.add(TAGML.DEFAULT_LAYER);
            //      }
        } else if (layerRelevance == RelevanceStyle.exclude) {
            relevantLayers.removeAll(layersToExclude!!)
        }
        return relevantLayers
    }

    fun filterRelevantMarkup(markupIds: Set<Long>): Set<Long> {
        val relevantMarkupIds: MutableSet<Long> = LinkedHashSet(markupIds)
        if (RelevanceStyle.include == layerRelevance) {
            val retain = markupIds.stream()
                    .filter { m: Long -> hasOverlap(layersToInclude, getLayers(m)) }
                    .collect(Collectors.toList())
            relevantMarkupIds.retainAll(retain)
        } else if (RelevanceStyle.exclude == layerRelevance) {
            val remove = markupIds.stream()
                    .filter { m: Long -> hasOverlap(layersToExclude, getLayers(m)) }
                    .collect(Collectors.toList())
            relevantMarkupIds.removeAll(remove)
        }
        if (RelevanceStyle.include == markupRelevance) {
            val retain = markupIds.stream()
                    .filter { m: Long -> markupToInclude!!.contains(getTag(m)) }
                    .collect(Collectors.toList())
            relevantMarkupIds.retainAll(retain)
        } else if (RelevanceStyle.exclude == markupRelevance) {
            val remove = markupIds.stream()
                    .filter { m: Long -> markupToExclude!!.contains(getTag(m)) }
                    .collect(Collectors.toList())
            relevantMarkupIds.removeAll(remove)
        }
        return relevantMarkupIds
    }

    //  private boolean isInDefaultLayerOnly(final Long markupId) {
    //    return getLayers(markupId).stream().anyMatch(TAGML.DEFAULT_LAYER::equals);
    //  }
    private fun isInDefaultLayerOnly(markupId: Long): Boolean {
        val layers = getLayers(markupId)
        return layers.size == 1 && layers.iterator().next() == DEFAULT_LAYER
    }

    private fun hasOverlap(layersToInclude: Set<String?>?, layers: Set<String?>): Boolean {
        val overlap: MutableSet<String?> = HashSet(layers)
        overlap.retainAll(layersToInclude!!)
        return overlap.isNotEmpty()
    }

    private fun getLayers(markupId: Long): Set<String?> = store.getMarkup(markupId).layers

    fun withLayersToInclude(layersToInclude: Set<String?>?): TAGView {
        if (RelevanceStyle.exclude == layerRelevance) {
            throw RuntimeException("This TAGView already has set layersToExclude")
        }
        this.layersToInclude = layersToInclude
        layerRelevance = RelevanceStyle.include
        return this
    }

    fun withLayersToExclude(layersToExclude: Set<String?>?): TAGView {
        if (RelevanceStyle.include == layerRelevance) {
            throw RuntimeException("This TAGView already has set layersToInclude")
        }
        this.layersToExclude = layersToExclude
        layerRelevance = RelevanceStyle.exclude
        return this
    }

    fun withMarkupToInclude(markupToInclude: Set<String?>?): TAGView {
        if (RelevanceStyle.exclude == markupRelevance) {
            throw RuntimeException("This TAGView already has set markupToExclude")
        }
        this.markupToInclude = markupToInclude
        markupRelevance = RelevanceStyle.include
        return this
    }

    fun withMarkupToExclude(markupToExclude: Set<String?>?): TAGView {
        if (RelevanceStyle.include == markupRelevance) {
            throw RuntimeException("This TAGView already has set markupToInclude")
        }
        this.markupToExclude = markupToExclude
        markupRelevance = RelevanceStyle.exclude
        return this
    }

    val definition: TAGViewDefinition?
        get() = TAGViewDefinition()
                .setIncludeLayers(layersToInclude)
                .setExcludeLayers(layersToExclude)
                .setIncludeMarkup(markupToInclude)
                .setExcludeMarkup(markupToExclude)

    fun markupStyleIsInclude(): Boolean = RelevanceStyle.include == markupRelevance

    fun markupStyleIsExclude(): Boolean = RelevanceStyle.exclude == markupRelevance

    fun layerStyleIsInclude(): Boolean = RelevanceStyle.include == layerRelevance

    fun layerStyleIsExclude(): Boolean = RelevanceStyle.exclude == layerRelevance

    fun isIncluded(tagMarkup: TAGMarkup): Boolean {
        val tag = tagMarkup.tag
        return if (RelevanceStyle.include == markupRelevance) {
            markupToInclude!!.contains(tag)
        } else RelevanceStyle.exclude == markupRelevance && !markupToExclude!!.contains(tag)
    }

    private fun getTag(markupId: Long): String = store.getMarkup(markupId).tag

}
