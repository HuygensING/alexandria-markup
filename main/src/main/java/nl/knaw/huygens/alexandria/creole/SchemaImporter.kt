package nl.knaw.huygens.alexandria.creole

/*-
     * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
     */

import com.google.common.base.Preconditions
import nl.knaw.huygens.alexandria.creole.patterns.Patterns
import nl.knaw.huygens.tei.Document
import nl.knaw.huygens.tei.Element
import nl.knaw.huygens.tei.Node
import nl.knaw.huygens.tei.Text
import org.slf4j.LoggerFactory
import java.util.*
import java.util.function.BiFunction
import java.util.function.Function
import java.util.stream.Collectors

object SchemaImporter {
    private val LOG = LoggerFactory.getLogger(SchemaImporter::class.java!!)

    private val elementToPattern = HashMap<String, Function<Element, Pattern>>()

    private val elementToNameClass = HashMap<String, Function<Element, NameClass>>()

    fun fromXML(xml: String): Pattern {
        val doc = Document.createFromXml(xml, true)
        val definitionVisitor = DefinitionVisitor()
        doc.accept(definitionVisitor)
        val schemaTree = definitionVisitor.expandedSchemaTree

        val doc2 = Document.createFromXml(schemaTree, true)
        val root = doc2.root
        return toPattern(root)
    }

    fun fromCompactGrammar(xml: String): Pattern {
        return Patterns.EMPTY
    }

    init {
        elementToPattern["atom"] = Function { handleAtom(it) }
        elementToPattern["annotation"] = Function { handleAnnotation(it) }
        elementToPattern["attribute"] = Function { handleAttribute(it) }
        elementToPattern["choice"] = Function { handleChoice(it) }
        elementToPattern["concur"] = Function { handleConcur(it) }
        elementToPattern["concurOneOrMore"] = Function { handleConcurOneOrMore(it) }
        elementToPattern["concurZeroOrMore"] = Function { handleConcurZeroOrMore(it) }
        elementToPattern["element"] = Function { handleElement(it) }
        elementToPattern["empty"] = Function { handleEmpty(it) }
        elementToPattern["group"] = Function { handleGroup(it) }
        elementToPattern["interleave"] = Function { handleInterleave(it) }
        elementToPattern["mixed"] = Function { handleMixed(it) }
        elementToPattern["oneOrMore"] = Function { handleOneOrMore(it) }
        elementToPattern["optional"] = Function { handleOptional(it) }
        elementToPattern["partition"] = Function { handlePartition(it) }
        elementToPattern["range"] = Function { handleRange(it) }
        elementToPattern["text"] = Function { handleText(it) }
        elementToPattern["zeroOrMore"] = Function { handleZeroOrMore(it) }
    }

    private fun toPattern(element: Element): Pattern {
        val elementName = element.name
        val handler = elementToPattern[elementName]
                ?: throw RuntimeException("no elementHandler defined for Element $elementName")
        return handler.apply(element)
    }

    private fun handleAtom(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)
        val name = element.getAttribute("name")
        return atom(name)
    }

    private fun handleAnnotation(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)
        if (element.hasAttribute("name")) {
            val name = element.getAttribute("name")
            Preconditions.checkState(children.size == 1)
            val pattern = toPattern(children[0])
            return annotation(name, pattern)

        } else {
            Preconditions.checkState(children.size == 2)
            val nameClass = toNameClass(children[0])
            val pattern = toPattern(children[1])
            return annotation(nameClass, pattern)
        }
    }

    private fun handleAttribute(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)
        val name = element.getAttribute("name")
        return attribute(name)
    }

    private fun handleChoice(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)

        val pattern1 = toPattern(children.removeAt(0))
        val pattern2 = simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.choice(pattern1, pattern2) })
        return choice(pattern1, pattern2)
    }

    private fun handleConcur(element: Element): Pattern {
        val children = getChildElements(element)
        //    List<Element> attributes = removeAttributes(children);

        val pattern1 = toPattern(children.removeAt(0))
        val pattern2 = simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.concur(pattern1, pattern2) })
        return concur(pattern1, pattern2)
    }

    private fun handleConcurOneOrMore(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)
        Preconditions.checkState(children.size == 1)
        val pattern = toPattern(children[0])
        return concurOneOrMore(pattern)
    }

    private fun handleConcurZeroOrMore(element: Element): Pattern {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 1)
        val pattern = toPattern(children[0])
        return concurZeroOrMore(pattern)
    }

    private fun handleElement(element: Element): Pattern {
        val localName = element.getAttribute("name")
        val children = getChildElements(element)
        val pattern = if (children.size == 1)
            toPattern(children[0])
        else
            simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.group(pattern1, pattern2) })
        return element(localName, pattern)
    }

    private fun handleEmpty(element: Element): Pattern {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 0)
        return empty()
    }

    private fun handleGroup(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)

        val pattern1 = toPattern(children.removeAt(0))
        val pattern2 = simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.group(pattern1, pattern2) })
        return group(pattern1, pattern2)
    }

    private fun handleInterleave(element: Element): Pattern {
        val children = getChildElements(element)
        //    List<Element> attributes = removeAttributes(children);

        val pattern1 = toPattern(children.removeAt(0))
        val pattern2 = simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.interleave(pattern1, pattern2) })
        return interleave(pattern1, pattern2)
    }

    private fun handleMixed(element: Element): Pattern {
        val children = getChildElements(element)
        val pattern = if (children.size == 1//
        )
            toPattern(children[0])//
        else
            simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.group(pattern1, pattern2) })
        return mixed(pattern)
    }

    private fun handleOneOrMore(element: Element): Pattern {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 1)
        val pattern = toPattern(children[0])
        return oneOrMore(pattern)
    }

    private fun handleOptional(element: Element): Pattern {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 1)
        val pattern = toPattern(children[0])
        return optional(pattern)
    }

    private fun handlePartition(element: Element): Pattern {
        val children = getChildElements(element)
        val pattern = if (children.size == 1)
            toPattern(children[0])
        else
            simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.group(pattern1, pattern2) })
        return partition(pattern)
    }

    private fun handleRange(element: Element): Pattern {
        val children = getChildElements(element)
        val attributes = removeAttributes(children)
        val nameClass = if (element.hasAttribute("name")//
        )
            name(element.getAttribute("name"))//
        else
            toNameClass(children.removeAt(0))
        val childPattern = if (children.size == 1//
        )
            toPattern(children[0])//
        else
            simplifyAsNeeded(children, BiFunction { pattern1, pattern2 -> Constructors.group(pattern1, pattern2) })
        return range(nameClass, childPattern)
    }

    private fun handleZeroOrMore(element: Element): Pattern {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 1)
        val pattern = toPattern(children[0])
        return zeroOrMore(pattern)
    }

    private fun handleText(element: Element): Pattern {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 0)
        return text()
    }

    private fun getChildElements(element: Element): MutableList<Element> {
        return element.nodes//
                .stream()//
                .filter(Predicate<Node> { Element::class.java!!.isInstance(it) })//
                .map(Function<Node, Element> { Element::class.java!!.cast(it) })//
                .collect<List<Element>, Any>(Collectors.toList())
    }

    private fun removeAttributes(children: MutableList<Element>): List<Element> {
        val attributes = children.stream().filter { e -> "attribute" == e.name }.collect<List<Element>, Any>(Collectors.toList())
        children.removeAll(attributes)
        return attributes
    }

    private fun simplifyAsNeeded(children: MutableList<Element>, patternConstructor: BiFunction<Pattern, Pattern, Pattern>): Pattern {
        if (children.size == 1) {
            return toPattern(children.removeAt(0))

        } else {
            val pattern1 = toPattern(children.removeAt(0))
            val pattern2 = simplifyAsNeeded(children, patternConstructor)
            return patternConstructor.apply(pattern1, pattern2)
        }
    }

    init {
        elementToNameClass["anyName"] = Function { handleAnyName(it) }
        elementToNameClass["name"] = Function { handleName(it) }
        elementToNameClass["nsName"] = Function { handleNsName(it) }
        elementToNameClass["choice"] = Function { handleNameClassChoice(it) }
    }

    private fun toNameClass(element: Element): NameClass {
        val elementName = element.name
        val handler = elementToNameClass[elementName]
                ?: throw RuntimeException("no elementHandler defined for Element $elementName")
        return handler.apply(element)
    }

    private fun handleAnyName(element: Element): NameClass {
        val children = getChildElements(element)
        if (hasExcept(children)) {
            val except = children[0]
            val exceptChildren = getChildElements(except)
            Preconditions.checkState(exceptChildren.size == 1)
            val nc = toNameClass(exceptChildren[0])
            return anyNameExcept(nc)
        } else {
            return anyName()
        }
    }

    private fun hasExcept(children: List<Element>): Boolean {
        return children.size == 1 && children[0].name == "except"
    }

    private fun handleName(element: Element): NameClass {
        val nodes = element.nodes
        Preconditions.checkState(nodes.size == 1)
        val nameNode = nodes[0] as Text
        return name(nameNode.text)
    }

    private fun handleNsName(element: Element): NameClass {
        val uri = element.getAttribute("uri")
        val children = getChildElements(element)
        if (hasExcept(children)) {
            val except = children[0]
            val exceptChildren = getChildElements(except)
            Preconditions.checkState(exceptChildren.size == 1)
            val nc = toNameClass(exceptChildren[0])
            return nsNameExcept(uri, nc)
        } else {
            return nsName(uri)
        }
    }

    private fun handleNameClassChoice(element: Element): NameClass {
        val children = getChildElements(element)
        Preconditions.checkState(children.size == 2)
        val nc1 = toNameClass(children.removeAt(0))
        val nc2 = toNameClass(children.removeAt(0))
        return nameClassChoice(nc1, nc2)
    }


}
