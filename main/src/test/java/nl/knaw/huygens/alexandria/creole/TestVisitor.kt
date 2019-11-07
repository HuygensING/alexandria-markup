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

import nl.knaw.huygens.tei.*
import nl.knaw.huygens.tei.handlers.XmlTextHandler
import java.util.*

internal class TestVisitor : DelegatingVisitor<XmlContext>(XmlContext()) {
    init {
        setTextHandler(XmlTextHandler())
        setDefaultElementHandler(DefaultElementHandler())
        addElementHandler(TestHandler(), "test:test")
        addElementHandler(TestTitleHandler(), "test:title")
        addElementHandler(TestParamHandler(), "test:param")
        addElementHandler(TestExpectHandler(), "test:expect")
    }

    fun getTests(): List<LMNLTest> {
        return tests
    }

    internal class DefaultElementHandler : ElementHandler<XmlContext> {
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

    internal class TestHandler : DefinitionVisitor.DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            currentTest = LMNLTest()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            tests.add(currentTest)
            currentTest = null
            return Traversal.NEXT
        }
    }

    internal class TestParamHandler : DefinitionVisitor.DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val content = context.closeLayer().trim { it <= ' ' }//
                    .replace("^ {6}".toRegex(), "")//
                    .replace("\n {6}".toRegex(), "\n")//
                    .replace("\n\n".toRegex(), "\n")
            val name = element.getAttribute("name")
            when (name) {
                "lmnl" -> currentTest!!.lmnl = content
                "schema" -> currentTest!!.creole = "<!-- " + currentTest!!.title + " -->\n<start>\n" + content + "\n</start>"
                else -> throw RuntimeException("unexpected value for name: $name")
            }
            return Traversal.NEXT
        }
    }

    internal class TestTitleHandler : DefinitionVisitor.DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val content = context.closeLayer().trim { it <= ' ' }
            if (currentTest != null) {
                currentTest!!.title = content
            }
            return Traversal.NEXT
        }
    }

    internal class TestExpectHandler : DefinitionVisitor.DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            val select = element.getAttribute("select")
            when (select) {
                "true()" -> currentTest!!.isValid = true
                "false()" -> currentTest!!.isValid = false
                else -> throw RuntimeException("unexpected value for select: $select")
            }
            return Traversal.NEXT
        }
    }

    companion object {
        private val tests = ArrayList<LMNLTest>()

        private var currentTest: LMNLTest? = null
    }

}
