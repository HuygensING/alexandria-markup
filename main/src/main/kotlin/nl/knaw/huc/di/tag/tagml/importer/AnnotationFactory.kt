package nl.knaw.huc.di.tag.tagml.importer

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

import nl.knaw.huc.di.tag.model.graph.TextGraph
import nl.knaw.huc.di.tag.model.graph.edges.AnnotationEdge
import nl.knaw.huc.di.tag.model.graph.edges.Edge
import nl.knaw.huc.di.tag.model.graph.edges.ListItemEdge
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.*
import nl.knaw.huc.di.tag.tagml.importer.AnnotationFactory
import nl.knaw.huygens.alexandria.ErrorListener
import nl.knaw.huygens.alexandria.storage.AnnotationType
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.slf4j.LoggerFactory
import java.util.*
import java.util.stream.Collectors

class AnnotationFactory @JvmOverloads constructor(private val store: TAGStore, private val textGraph: TextGraph, private val errorListener: ErrorListener? = null) {
    fun makeAnnotation(basicAnnotationContext: BasicAnnotationContext): AnnotationInfo {
        val aName = basicAnnotationContext.annotationName().text
        val annotationValueContext = basicAnnotationContext.annotationValue()
        val value = annotationValue(annotationValueContext)
        return makeAnnotation(aName, annotationValueContext, value)
                ?: throw RuntimeException("unhandled basic annotation " + basicAnnotationContext.text)
    }

    private fun makeAnnotation(
            aName: String, annotationValueContext: AnnotationValueContext, value: Any?): AnnotationInfo? {
        var annotationInfo: AnnotationInfo? = null
        annotationInfo = if (value is String) {
            makeStringAnnotation(aName, value)
        } else if (value is Boolean) {
            makeBooleanAnnotation(aName, value)
        } else if (value is Double) {
            makeDoubleAnnotation(aName, value)
        } else if (value is List<*>) {
            makeListAnnotation(aName, annotationValueContext, value as List<Any>)
        } else if (value is HashMap<*, *>) {
            makeMapAnnotation(aName, annotationValueContext, value as HashMap<String, Any>)
        } else {
            makeOtherAnnotation(aName, annotationValueContext)
        }
        return annotationInfo
    }

    private fun makeStringAnnotation(aName: String, value: String): AnnotationInfo {
        val id = store.createStringAnnotationValue(value)
        return AnnotationInfo(id, AnnotationType.String, aName)
    }

    private fun makeBooleanAnnotation(aName: String, value: Boolean): AnnotationInfo {
        val id = store.createBooleanAnnotationValue(value)
        return AnnotationInfo(id, AnnotationType.Boolean, aName)
    }

    private fun makeDoubleAnnotation(aName: String, value: Double): AnnotationInfo {
        val id = store.createNumberAnnotationValue(value)
        return AnnotationInfo(id, AnnotationType.Number, aName)
    }

    fun makeReferenceAnnotation(aName: String?, value: String?): AnnotationInfo {
        val id = store.createReferenceValue(value)
        return AnnotationInfo(id, AnnotationType.Reference, aName!!)
    }

    private fun makeListAnnotation(
            aName: String,
            annotationValueContext: AnnotationValueContext,
            value: List<Any>): AnnotationInfo {
        val annotationInfo: AnnotationInfo
        verifyListElementsAreSameType(aName, annotationValueContext, value)
        verifySeparatorsAreCommas(aName, annotationValueContext)
        val id = store.createListAnnotationValue()
        annotationInfo = AnnotationInfo(id, AnnotationType.List, aName)
        val valueTree = annotationValueContext.children[0]
        val childCount = valueTree.childCount
        var i = 1
        while (i < childCount) {
            val listElement = valueTree.getChild(i)
            val subValueParseTree = listElement.getChild(0)
            val subValue = value[(i - 1) / 2]
            val subValueContext = listElement as AnnotationValueContext
            val listElementInfo = makeAnnotation("", subValueContext, subValue)
            textGraph.addListItem(id, listElementInfo)
            i += 2
        }
        return annotationInfo
    }

    private fun makeMapAnnotation(
            aName: String,
            annotationValueContext: AnnotationValueContext,
            value: HashMap<String, Any>): AnnotationInfo {
        val annotationInfo: AnnotationInfo
        val id = store.createMapAnnotationValue()
        annotationInfo = AnnotationInfo(id, AnnotationType.Map, aName)
        val valueTree = annotationValueContext.children[0]
        val childCount = valueTree.childCount // children: '{' annotation+ '}'
        for (i in 1 until childCount - 1) {
            val hashElement = valueTree.getChild(i)
            val subName = hashElement.getChild(0).text
            val subValueParseTree = hashElement.getChild(2)
            if (subValueParseTree is AnnotationValueContext) {
                val subValue = value[subName]
                val aInfo = makeAnnotation(subName, subValueParseTree, subValue)
                textGraph.addAnnotationEdge(id, aInfo)
            } else if (subValueParseTree is IdValueContext) {
                val idValue = subValueParseTree.text
                annotationInfo.setId(idValue)
            } else if (subValueParseTree is RefValueContext) {
                val refValue = subValueParseTree.text
                val aInfo = makeReferenceAnnotation(subName, refValue)
                textGraph.addAnnotationEdge(id, aInfo)
            } else {
                throw RuntimeException("TODO: handle " + subValueParseTree.javaClass)
            }
        }
        return annotationInfo
    }

    private fun makeOtherAnnotation(
            aName: String, annotationValueContext: AnnotationValueContext): AnnotationInfo {
        val annotationInfo: AnnotationInfo
        val placeholder = annotationValueContext.text
        val id = store.createStringAnnotationValue(placeholder)
        annotationInfo = AnnotationInfo(id, AnnotationType.String, aName)
        return annotationInfo
    }

    private fun verifyListElementsAreSameType(
            aName: String,
            annotationValueContext: AnnotationValueContext,
            list: List<Any>) {
        val valueTypes = list.stream().map { v: Any -> v.javaClass.name }.collect(Collectors.toSet())
        if (valueTypes.size > 1) {
            addError(
                    annotationValueContext,
                    "All elements of ListAnnotation %s should be of the same type.",
                    aName)
        }
    }

    private fun verifySeparatorsAreCommas(
            aName: String, annotationValueContext: AnnotationValueContext) {
        val valueTree = annotationValueContext.children[0]
        val childCount = valueTree.childCount // children: '[' value (separator value)* ']'
        val separators: MutableSet<String> = HashSet()
        var i = 2
        while (i < childCount - 1) {
            separators.add(valueTree.getChild(i).text.trim { it <= ' ' })
            i += 2
        }
        val allSeparatorsAreCommas = separators.isEmpty() || separators.size == 1 && separators.contains(",")
        if (!allSeparatorsAreCommas) {
            addError(
                    annotationValueContext,
                    "The elements of ListAnnotation %s should be separated by commas.",
                    aName)
        }
    }

    private fun annotationValue(annotationValueContext: AnnotationValueContext): Any? {
        if (annotationValueContext.AV_StringValue() != null) {
            return annotationValueContext
                    .AV_StringValue()
                    .text
                    .replaceFirst("^.".toRegex(), "")
                    .replaceFirst(".$".toRegex(), "")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
        } else if (annotationValueContext.booleanValue() != null) {
            return java.lang.Boolean.valueOf(annotationValueContext.booleanValue().text)
        } else if (annotationValueContext.AV_NumberValue() != null) {
            return java.lang.Double.valueOf(annotationValueContext.AV_NumberValue().text)
        } else if (annotationValueContext.listValue() != null) {
            return annotationValueContext.listValue().annotationValue().stream()
                    .map { annotationValueContext: AnnotationValueContext -> annotationValue(annotationValueContext) }
                    .collect(Collectors.toList())
        } else if (annotationValueContext.objectValue() != null) {
            return readObject(annotationValueContext.objectValue())
        } else if (annotationValueContext.richTextValue() != null) {
            return annotationValueContext.richTextValue().text
        }
        errorListener!!.addError(
                Position.Companion.startOf(annotationValueContext.getParent()),
                Position.Companion.endOf(annotationValueContext.getParent()),
                "Cannot determine the type of this annotation: %s",
                annotationValueContext.text)
        return null
    }

    private fun readObject(objectValueContext: ObjectValueContext): Map<String, Any?> {
        val map: MutableMap<String, Any?> = LinkedHashMap()
        objectValueContext.children.stream()
                .filter { c: ParseTree? -> c !is TerminalNode }
                .map { parseTree: ParseTree -> parseAttribute(parseTree) } //        .peek(System.out::println)
                .forEach { kv: KeyValue -> map[kv.key] = kv.value }
        return map
    }

    private fun parseAttribute(parseTree: ParseTree): KeyValue {
        return if (parseTree is BasicAnnotationContext) {
            val basicAnnotationContext = parseTree
            val aName = basicAnnotationContext.annotationName().text
            val annotationValueContext = basicAnnotationContext.annotationValue()
            val value = annotationValue(annotationValueContext)
            KeyValue(aName, value)
        } else if (parseTree is IdentifyingAnnotationContext) {
            // TODO: deal with this identifier
            val value = parseTree.idValue().text
            KeyValue(":id", value)
        } else if (parseTree is RefAnnotationContext) {
            val refAnnotationContext = parseTree
            val aName = refAnnotationContext.annotationName().text
            val value = refAnnotationContext.refValue().text
            KeyValue("!$aName", value)
        } else {
            throw RuntimeException("unhandled type " + parseTree.javaClass.name)
            //      errorListener.addBreakingError("%s Cannot determine the type of this annotation: %s",
            //          errorPrefix(parseTree.), parseTree.getText());
        }
    }

    fun getStringValue(annotationInfo: AnnotationInfo): String {
        val stringAnnotationValue = store.getStringAnnotationValue(annotationInfo.nodeId)
        return stringAnnotationValue.value
    }

    fun getNumberValue(annotationInfo: AnnotationInfo): Double {
        val numberAnnotationValue = store.getNumberAnnotationValue(annotationInfo.nodeId)
        return numberAnnotationValue.value
    }

    fun getBooleanValue(annotationInfo: AnnotationInfo): Boolean {
        val booleanAnnotationValue = store.getBooleanAnnotationValue(annotationInfo.nodeId)
        return booleanAnnotationValue.value
    }

    fun getListValue(annotationInfo: AnnotationInfo): List<AnnotationInfo> {
        val nodeId = annotationInfo.nodeId
        return textGraph.getOutgoingEdges(nodeId).stream()
                .filter { obj: Edge? -> ListItemEdge::class.java.isInstance(obj) }
                .map { obj: Edge? -> ListItemEdge::class.java.cast(obj) }
                .map { listItemEdge: ListItemEdge -> this.toAnnotationInfo(listItemEdge) }
                .collect(Collectors.toList())
    }

    fun getMapValue(annotationInfo: AnnotationInfo): List<AnnotationInfo> {
        val nodeId = annotationInfo.nodeId
        return textGraph.getOutgoingEdges(nodeId).stream()
                .filter { obj: Edge? -> AnnotationEdge::class.java.isInstance(obj) }
                .map { obj: Edge? -> AnnotationEdge::class.java.cast(obj) }
                .map { annotationEdge: AnnotationEdge -> this.toAnnotationInfo(annotationEdge) }
                .collect(Collectors.toList())
    }

    fun getReferenceValue(annotationInfo: AnnotationInfo): String {
        val nodeId = annotationInfo.nodeId
        return store.getReferenceValue(nodeId).value
    }

    private fun toAnnotationInfo(listItemEdge: ListItemEdge): AnnotationInfo {
        val nodeId = textGraph.getTargets(listItemEdge).iterator().next()
        val type = listItemEdge.annotationType
        val annotationInfo = AnnotationInfo(nodeId, type, "")
        annotationInfo.setId(listItemEdge.id)
        return annotationInfo
    }

    private fun toAnnotationInfo(annotationEdge: AnnotationEdge): AnnotationInfo {
        val nodeId = textGraph.getTargets(annotationEdge).iterator().next()
        val type = annotationEdge.annotationType
        val name = annotationEdge.field
        val annotationInfo = AnnotationInfo(nodeId, type, name)
        annotationInfo.setId(annotationEdge.id)
        return annotationInfo
    }

    private fun addError(
            annotationValueContext: AnnotationValueContext,
            messageTemplate: String,
            aName: String) {
        errorListener!!.addError(
                Position.Companion.startOf(annotationValueContext),
                Position.Companion.endOf(annotationValueContext),
                messageTemplate,
                aName)
    }

    private class KeyValue(var key: String, var value: Any?)

    companion object {
        private val LOG = LoggerFactory.getLogger(AnnotationFactory::class.java)
    }

}
