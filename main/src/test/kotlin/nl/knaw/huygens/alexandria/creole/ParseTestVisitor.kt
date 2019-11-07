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
import java.util.*

class ParseTestVisitor : DelegatingVisitor<XmlContext>(XmlContext()) {

    val testCode: String
        get() = testCodeBuilder.toString().replace(",//\n)", "//\n)")

    init {
        setTextHandler(XmlTextHandler())
        setDefaultElementHandler(DefaultElementHandler())
        addElementHandler(TestHandler(), "test:test")
        addElementHandler(TestTitleHandler(), "test:title")
        addElementHandler(TestParamHandler(), "test:param")
        addElementHandler(TestExpectHandler(), "test:expect")
        addElementHandler(ExpectedEventHandler("atomOpen"), "ev:atom-open")
        addElementHandler(ExpectedEventHandler("atomClose"), "ev:atom-close")
        addElementHandler(ExpectedEventHandler("startAnnotationOpen"), "ev:start-annotation-open")
        addElementHandler(ExpectedEventHandler("startAnnotationClose"), "ev:start-annotation-close")
        addElementHandler(ExpectedEventHandler("endAnnotationOpen"), "ev:end-annotation-open")
        addElementHandler(ExpectedEventHandler("endAnnotationClose"), "ev:end-annotation-close")
        addElementHandler(ExpectedEventHandler("startTagOpen"), "ev:start-tag-open")
        addElementHandler(ExpectedEventHandler("startTagClose"), "ev:start-tag-close")
        addElementHandler(ExpectedEventHandler("endTagOpen"), "ev:end-tag-open")
        addElementHandler(ExpectedEventHandler("endTagClose"), "ev:end-tag-close")
        addElementHandler(ExpectedTextHandler(), "ev:text")
    }

    fun getTests(): List<LMNLTest> {
        return tests
    }

    internal open class DefaultElementHandler : ElementHandler<XmlContext> {
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

    internal class TestHandler : DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            currentTest = LMNLTest()
            testCodeBuilder.append("@Test public void parseLMNL").append(testCount++).append("() {\n")
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            tests.add(currentTest)
            currentTest = null
            return Traversal.NEXT
        }
    }

    internal class TestParamHandler : DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val content = context.closeLayer().trim { it <= ' ' }//
                    .replace("\"".toRegex(), "\\\\\"")//
                    .replace("\n".toRegex(), "\\\\n")
            val name = element.getAttribute("name")
            when (name) {
                "lmnl" -> {
                    currentTest!!.lmnl = content
                    testCodeBuilder.append("String lmnl=\"").append(content).append("\";\n")
                }
                else -> throw RuntimeException("unexpected value for name: $name")
            }
            return Traversal.NEXT
        }
    }

    internal class TestTitleHandler : DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val content = context.closeLayer().trim { it <= ' ' }//
                    .replace("^ {6}".toRegex(), "")//
                    .replace("\n {6}".toRegex(), "\n")//
                    .replace("\n\n".toRegex(), "\n")
            if (currentTest != null) {
                currentTest!!.title = content
                testCodeBuilder.append("    // ").append(content).append("\n")
            }
            return Traversal.NEXT
        }
    }

    internal class TestExpectHandler : DefinitionVisitor.DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            expectedEvents.clear()
            testCodeBuilder.append("List<Event> expectedEvents = asList(//\n")
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            testCodeBuilder.append(");\n")//
                    .append("assertEventsAreExpected(lmnl, expectedEvents);\n}\n")
            return Traversal.NEXT
        }
    }

    private inner class ExpectedTextHandler : DefaultElementHandler() {
        override fun enterElement(element: Element, context: XmlContext): Traversal {
            context.openLayer()
            return Traversal.NEXT
        }

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val chars = element.getAttribute("chars")
            testCodeBuilder.append("Events.textEvent(\"").append(chars).append("\"),//\n")
            return Traversal.NEXT
        }
    }

    private inner class ExpectedEventHandler internal constructor(private val event: String) : DefaultElementHandler() {

        override fun leaveElement(element: Element, context: XmlContext): Traversal {
            val name = element.getAttribute("name")
            testCodeBuilder.append(event + "Event(\"").append(name).append("\"),//\n")
            return Traversal.NEXT
        }
    }

    companion object {
        private val tests = ArrayList<LMNLTest>()

        protected var currentTest: LMNLTest? = null
        protected var testCodeBuilder = StringBuilder()
        internal var testCount = 0

        private val expectedEvents = ArrayList<String>()
    }

}

