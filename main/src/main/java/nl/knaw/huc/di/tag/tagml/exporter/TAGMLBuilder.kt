package nl.knaw.huc.di.tag.tagml.exporter

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

import nl.knaw.huc.di.tag.TAGVisitor
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES_END
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCHES_START
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH_END
import nl.knaw.huc.di.tag.tagml.TAGML.BRANCH_START
import nl.knaw.huc.di.tag.tagml.TAGML.CONVERGENCE
import nl.knaw.huc.di.tag.tagml.TAGML.DIVERGENCE
import nl.knaw.huc.di.tag.tagml.TAGML.DIVIDER
import nl.knaw.huc.di.tag.tagml.TAGML.escapeRegularText
import nl.knaw.huc.di.tag.tagml.TAGML.escapeVariantText
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.view.TAGView

class TAGMLBuilder : TAGVisitor {
    var result = ""
    var tagmlBuilder = StringBuilder()

    override fun setView(tagView: TAGView) {}

    override fun enterDocument(document: TAGDocument) {}

    override fun exitDocument(document: TAGDocument) {
        result = tagmlBuilder
                .toString()
                .replace(BRANCHES_START + BRANCH_START, DIVERGENCE)
                .replace(BRANCH_END + BRANCH_START, DIVIDER)
                .replace(BRANCH_END + BRANCHES_END, CONVERGENCE)
    }

    override fun enterOpenTag(markup: TAGMarkup) {}

    override fun addAnnotation(serializedAnnotation: String) {}

    override fun serializeAnnotationAssigner(name: String): String {
        TODO()
    }

    override fun exitOpenTag(markup: TAGMarkup) {}

    override fun exitCloseTag(markup: TAGMarkup) {}

    override fun exitText(text: String, inVariation: Boolean) {
        val escapedText = if (inVariation)
            escapeVariantText(text)
        else
            escapeRegularText(text)
        tagmlBuilder.append(escapedText)
    }

    override fun enterTextVariation() {}

    override fun exitTextVariation() {}

    override fun setRelevantLayers(relevantLayers: Set<String>) {}

    override fun serializeStringAnnotationValue(stringValue: String): String = ""

    override fun serializeNumberAnnotationValue(numberValue: Double): String = ""

    override fun serializeBooleanAnnotationValue(booleanValue: Boolean): String = ""

    override fun serializeListAnnotationValue(serializedItems: List<String>): String = ""

    override fun serializeMapAnnotationValue(serializedMapItems: List<String>): String = ""
}