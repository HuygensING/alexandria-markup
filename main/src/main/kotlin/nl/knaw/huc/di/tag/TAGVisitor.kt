package nl.knaw.huc.di.tag

/*-
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

import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.view.TAGView

interface TAGVisitor {
    fun setView(tagView: TAGView)
    fun enterDocument(document: TAGDocument)
    fun exitDocument(document: TAGDocument)
    fun enterOpenTag(markup: TAGMarkup)
    fun addAnnotation(serializedAnnotation: String)
    fun serializeAnnotationAssigner(name: String): String
    fun serializeStringAnnotationValue(stringValue: String): String
    fun serializeNumberAnnotationValue(numberValue: Double): String
    fun serializeBooleanAnnotationValue(booleanValue: Boolean): String
    fun serializeListAnnotationValue(serializedItems: List<String>): String
    fun serializeMapAnnotationValue(serializedMapItems: List<String>): String
    fun exitOpenTag(markup: TAGMarkup)
    fun exitCloseTag(markup: TAGMarkup)
    fun exitText(escapedText: String, inVariation: Boolean)
    fun enterTextVariation()
    fun exitTextVariation()
    fun setRelevantLayers(relevantLayers: Set<String>)
}
