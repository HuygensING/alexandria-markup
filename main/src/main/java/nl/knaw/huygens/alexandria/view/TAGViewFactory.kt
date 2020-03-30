package nl.knaw.huygens.alexandria.view

import nl.knaw.huygens.alexandria.storage.TAGStore
import java.io.StringReader
import javax.json.Json
import javax.json.JsonArray
import javax.json.JsonObject
import javax.json.JsonString

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

class TAGViewFactory(private val store: TAGStore) {

    fun fromJsonString(json: String): TAGView {
        val reader = Json.createReader(StringReader(json))
        val jsonObject = reader.readObject()
        reader.close()
        return toTagView(jsonObject)
    }

    fun fromDefinition(definition: TAGViewDefinition): TAGView {
        return createTAGView(
                definition.includeLayers,
                definition.excludeLayers,
                definition.includeMarkup,
                definition.excludeMarkup,
                definition.markupWithLayerExclusiveText)
    }

    private fun toTagView(jsonObject: JsonObject): TAGView {
        val includeLayerArray = jsonObject.getJsonArray(INCLUDE_LAYERS)
        val excludeLayerArray = jsonObject.getJsonArray(EXCLUDE_LAYERS)
        val includeMarkupArray = jsonObject.getJsonArray(INCLUDE_MARKUP)
        val excludeMarkupArray = jsonObject.getJsonArray(EXCLUDE_MARKUP)
        val markupWithLayerExclusiveTextArray = jsonObject.getJsonArray(MARKUP_WITH_LAYER_EXCLUSIVE_TEXT)
        var includeLayers: Set<String> = setOf()
        var excludeLayers: Set<String> = setOf()
        var includeMarkup: Set<String> = setOf()
        var excludeMarkup: Set<String> = setOf()
        var markupWithLayerExclusiveText: Set<String> = setOf()
        if (includeLayerArray != null) {
            includeLayers = getElements(includeLayerArray)
        }
        if (excludeLayerArray != null) {
            excludeLayers = getElements(excludeLayerArray)
        }
        if (includeMarkupArray != null) {
            includeMarkup = getElements(includeMarkupArray)
        }
        if (excludeMarkupArray != null) {
            excludeMarkup = getElements(excludeMarkupArray)
        }
        if (markupWithLayerExclusiveTextArray != null) {
            markupWithLayerExclusiveText = getElements(markupWithLayerExclusiveTextArray)
        }
        return createTAGView(
                includeLayers,
                excludeLayers,
                includeMarkup,
                excludeMarkup,
                markupWithLayerExclusiveText
        )
    }

    private fun notEmpty(stringSet: Set<String>?): Boolean {
        return stringSet != null && stringSet.isNotEmpty()
    }

    private fun createTAGView(
            includeLayers: Set<String>,
            excludeLayers: Set<String>,
            includeMarkup: Set<String>,
            excludeMarkup: Set<String>,
            markupWithLayerExclusiveText: Set<String>): TAGView {
        val tagView = TAGView(store)
        if (notEmpty(includeLayers)) {
            tagView.layersToInclude = includeLayers
        }
        if (notEmpty(excludeLayers)) {
            tagView.layersToExclude = excludeLayers
        }
        if (notEmpty(includeMarkup)) {
            tagView.markupToInclude = includeMarkup
        }
        if (notEmpty(excludeMarkup)) {
            tagView.markupToExclude = excludeMarkup
        }
        if (notEmpty(markupWithLayerExclusiveText)) {
            tagView.markupWithLayerExclusiveText = markupWithLayerExclusiveText
        }
        return tagView
    }

    val defaultView: TAGView
        get() = TAGView(store)

    companion object {
        const val INCLUDE_LAYERS = "includeLayers"
        const val EXCLUDE_LAYERS = "excludeLayers"
        const val INCLUDE_MARKUP = "includeMarkup"
        const val EXCLUDE_MARKUP = "excludeMarkup"
        const val MARKUP_WITH_LAYER_EXCLUSIVE_TEXT = "markupWithLayerExclusiveText"

        private fun getElements(jsonArray: JsonArray): Set<String> =
                jsonArray.getValuesAs(JsonString::class.java)
                        .map { obj: JsonString -> obj.string }
                        .toSet()
    }

}
