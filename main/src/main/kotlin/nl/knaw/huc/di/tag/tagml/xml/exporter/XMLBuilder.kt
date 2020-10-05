package nl.knaw.huc.di.tag.tagml.xml.exporter

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
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.view.TAGView
import org.apache.commons.text.StringEscapeUtils
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors

class XMLBuilder(val leadingLayer: String = DEFAULT_LAYER) : TAGVisitor {
    private val xmlBuilder = StringBuilder()
    val thIds: MutableMap<Any, String> = HashMap()
    val thIdCounter = AtomicInteger(0)
    val namespaceDefinitions: MutableList<String> = ArrayList()
    private var useTagNamespace = false
    var useTrojanHorse = false
    private var relevantLayers: Set<String>? = null

    var result: String = ""
        private set

    private val discontinuityCounter = AtomicInteger(1)
    private val discontinuityNumber: MutableMap<String, Int> = HashMap()

    override fun setView(tagView: TAGView) {}

    override fun setRelevantLayers(relevantLayers: Set<String>) {
        useTrojanHorse = relevantLayers.size > 1
        this.relevantLayers = relevantLayers
    }

    override fun enterDocument(document: TAGDocument) {
        xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        if (relevantLayers!!.size > 1) {
            namespaceDefinitions.add(TH_NAMESPACE)
        }
        document.namespaces
                .forEach { (ns: String, url: String) -> namespaceDefinitions.add("xmlns:$ns=\"$url\"") }
        xmlBuilder.append("<xml")
        if (namespaceDefinitions.isNotEmpty()) {
            xmlBuilder.append(" ").append(java.lang.String.join(" ", namespaceDefinitions))
        }
        if (useTrojanHorse) {
            val thDoc = getThDoc(relevantLayers!!)
            xmlBuilder.append(" th:doc=\"").append(thDoc).append("\"")
        }
        xmlBuilder.append(">\n")
    }

    override fun exitDocument(document: TAGDocument) {
        xmlBuilder.append("\n</xml>")
        result = xmlBuilder.toString()
        if (useTagNamespace) {
            result = result.replaceFirst("<xml".toRegex(), "<xml $TAG_NAMESPACE")
        }
    }

    override fun enterOpenTag(markup: TAGMarkup) {
        //    boolean showMarkup = shouldBeShown(markup);
        //    if (showMarkup) {
        val markupName = getMarkupName(markup)
        xmlBuilder.append("<").append(markupName)
        if (markup.isOptional) {
            useTagNamespace = true
            xmlBuilder.append(" tag:optional=\"true\"")
        }
        val discontinuityKey = discontinuityKey(markup, markupName)
        if (markup.isSuspended) {
            useTagNamespace = true
            val n = discontinuityCounter.getAndIncrement()
            discontinuityNumber[discontinuityKey] = n
            xmlBuilder.append(" tag:n=\"").append(n).append("\"")
        } else if (markup.isResumed) {
            val n = discontinuityNumber[discontinuityKey]
            xmlBuilder.append(" tag:n=\"").append(n).append("\"")
        }
        //    }
    }

    private fun discontinuityKey(markup: TAGMarkup, markupName: String): String =
            markup.layers.stream().sorted().collect(Collectors.joining(",", "$markupName|", ""))

    private fun getMarkupName(markup: TAGMarkup): String {
        var markupName = markup.tag
        if (markupName.startsWith(":")) {
            markupName = "tag$markupName"
            useTagNamespace = true
        }
        return markupName
    }

    override fun addAnnotation(serializedAnnotation: String) {
        xmlBuilder.append(" ").append(serializedAnnotation)
    }

    override fun exitOpenTag(markup: TAGMarkup) {
        val layers = markup.layers intersect relevantLayers!!
        //    boolean showMarkup = shouldBeShown(markup);
        //    if (showMarkup) {
        if (useTrojanHorse && leadingLayer !in layers) {
            val thId = markup.tag + thIdCounter.getAndIncrement()
            thIds[markup] = thId
            val thDoc = getThDoc(layers)
            val id = if (markup.isAnonymous) "soleId" else "sId"
            xmlBuilder
                    .append(" th:doc=\"")
                    .append(thDoc)
                    .append("\"")
                    .append(" th:")
                    .append(id)
                    .append("=\"")
                    .append(thId)
                    .append("\"/")
        } else if (markup.isAnonymous) {
            xmlBuilder.append("/")
        }
        xmlBuilder.append(">")
        //    }
    }

    override fun exitCloseTag(markup: TAGMarkup) {
        val markupName = getMarkupName(markup)
        val layers = markup.layers intersect relevantLayers!!
        if (markup.isAnonymous) {
            return
        }
        //    boolean showMarkup = shouldBeShown(markup);
        //    if (showMarkup) {
        xmlBuilder.append("<")
        if (!useTrojanHorse || leadingLayer in layers) {
            xmlBuilder.append("/")
        }
        xmlBuilder.append(markupName)
        if (useTrojanHorse && leadingLayer !in layers) {
            val thDoc = getThDoc(layers)
            val thId = thIds.remove(markup)
            xmlBuilder
                    .append(" th:doc=\"")
                    .append(thDoc)
                    .append("\"")
                    .append(" th:eId=\"")
                    .append(thId)
                    .append("\"/")
        }
        xmlBuilder.append(">")
        //    }
    }

    //  private boolean shouldBeShown(final TAGMarkup markup) {
    //    return markup.getLayers().stream().anyMatch(relevantLayers::contains);
    //  }
    override fun exitText(escapedText: String, inVariation: Boolean) {
        val xmlEscapedText = StringEscapeUtils.escapeXml11(escapedText)
        xmlBuilder.append(xmlEscapedText)
    }

    override fun enterTextVariation() {
        useTagNamespace = true
    }

    override fun exitTextVariation() {}

    override fun serializeStringAnnotationValue(stringValue: String): String =
            "\"" + StringEscapeUtils.escapeXml11(stringValue) + "\""

    override fun serializeNumberAnnotationValue(numberValue: Double): String =
            serializeStringAnnotationValue(numberValue.toString().replaceFirst(".0$".toRegex(), ""))

    override fun serializeBooleanAnnotationValue(booleanValue: Boolean): String =
            serializeStringAnnotationValue(if (booleanValue) "true" else "false")

    override fun serializeListAnnotationValue(serializedItems: List<String>): String =
            serializeStringAnnotationValue(serializedItems.stream().collect(Collectors.joining(",", "[", "]")))

    override fun serializeMapAnnotationValue(serializedMapItems: List<String>): String =
            serializeStringAnnotationValue(
                    serializedMapItems.stream().collect(Collectors.joining(",", "{", "}")))

    override fun serializeAnnotationAssigner(name: String): String = "$name="

    private fun getThDoc(layerNames: Set<String>): String =
            layerNames
                    .filter { it != leadingLayer }
                    .map { l: String -> if (DEFAULT_LAYER == l) DEFAULT_DOC else l }
                    .sorted()
                    .joinToString(" ")

    companion object {
        const val TH_NAMESPACE = "xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\""
        const val TAG_NAMESPACE = "xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\""
        const val DEFAULT_DOC = "_default"
    }
}
