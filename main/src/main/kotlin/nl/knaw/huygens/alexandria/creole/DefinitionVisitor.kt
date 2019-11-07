package nl.knaw.huygens.alexandria.creole

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import nl.knaw.huygens.tei.*
import nl.knaw.huygens.tei.handlers.XmlTextHandler
import org.slf4j.LoggerFactory
import java.util.*

class DefinitionVisitor : DelegatingVisitor<XmlContext>(XmlContext()) {

    val expandedSchemaTree: String
        get() {
            var schemaTree = start
            while (schemaTree!!.contains("<ref name=")) {
                for (name in definitionMap.keys) {
                    schemaTree = schemaTree!!.replace("<ref name=\"$name\"/>", definitionMap[name])
                }
            }
            return schemaTree
        }

    init {
        setTextHandler(XmlTextHandler())
        setDefaultElementHandler(DefaultElementHandler())
        addElementHandler(DefineHandler(), "define")
        addElementHandler(GrammarHandler(), "grammar")
        addElementHandler(RefHandler(), "ref")
        addElementHandler(StartHandler(), "start")
    }

    open class DefaultElementHandler : ElementHandler<XmlContext> {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            if (element.hasChildren()) {
                context.addOpenTag(element)
            } else {
                context.addEmptyElementTag(element)
            }
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            if (element.hasChildren()) {
                context.addCloseTag(element)
            }
            return Traversal.NEXT
        }
    }

    class GrammarHandler : DefaultElementHandler()

    class StartHandler : DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            currentDefinition = "_start"
            (requirements as java.util.Map<String, Set<String>>).putIfAbsent(currentDefinition, HashSet())
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            start = context.closeLayer().trim { it <= ' ' }
            return Traversal.NEXT
        }
    }

    class RefHandler : DefaultElementHandler() {
        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val ref = element.getAttribute("name")
            requirements.get(currentDefinition).add(ref)
            (dependants as java.util.Map<String, Set<String>>).putIfAbsent(ref, HashSet())
            dependants[ref].add(currentDefinition)
            return super.leaveElement(element, context)
        }
    }

    class DefineHandler : DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            currentDefinition = element.getAttribute("name")
            (requirements as java.util.Map<String, Set<String>>).putIfAbsent(currentDefinition, HashSet())
            (dependants as java.util.Map<String, Set<String>>).putIfAbsent(currentDefinition, HashSet())
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val value = context.closeLayer().replace(" *\n *".toRegex(), "")
            definitionMap[currentDefinition] = value
            return Traversal.NEXT
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(DefinitionVisitor::class.java!!)

        private var start: String? = null
        private val definitionMap = HashMap<String, String>()
        private val requirements = HashMap<String, Set<String>>() // the definitions referred to in this definition
        private val dependants = HashMap<String, Set<String>>() // the definitions referring to this definition

        private var currentDefinition: String? = null
    }


}
