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

import nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat
import nl.knaw.huygens.alexandria.creole.Basics.qName
import nl.knaw.huygens.alexandria.creole.Constructors.choice
import nl.knaw.huygens.alexandria.creole.Constructors.concur
import nl.knaw.huygens.alexandria.creole.Constructors.concurOneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.element
import nl.knaw.huygens.alexandria.creole.Constructors.empty
import nl.knaw.huygens.alexandria.creole.Constructors.group
import nl.knaw.huygens.alexandria.creole.Constructors.interleave
import nl.knaw.huygens.alexandria.creole.Constructors.mixed
import nl.knaw.huygens.alexandria.creole.Constructors.oneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.range
import nl.knaw.huygens.alexandria.creole.Constructors.text
import nl.knaw.huygens.alexandria.creole.Constructors.zeroOrMore
import nl.knaw.huygens.alexandria.creole.NameClasses.name
import nl.knaw.huygens.alexandria.creole.events.Events
import nl.knaw.huygens.tei.Document
import org.apache.commons.io.FileUtils
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.util.*
import java.util.Arrays.asList

class ValidatorTest {
    private val LOG = LoggerFactory.getLogger(javaClass)

    @Test
    fun testValidator() {
        // [text}tekst{text]
        val qName = qName("text")
        val startE = Events.startTagEvent(qName)
        val context = Basics.context()
        val textE = Events.textEvent("tekst", context)
        val endE = Events.endTagEvent(qName)
        val validEvents = ArrayList(asList(startE, textE, endE))

        val schemaPattern = element(
                "text",
                text()
        )

        val validator = Validator.ofPattern(schemaPattern)
        val validationResult1 = validator.validate(validEvents)
        assertThat(validationResult1).isSuccess.hasNoUnexpectedEvent()

        val invalidEvents = ArrayList(asList(textE, startE, startE, endE))
        val validationResult = validator.validate(invalidEvents)
        assertThat(validationResult).isFailure.hasUnexpectedEvent(textE)
    }

    @Test
    fun testValidator2() {
        val schemaPattern = element(
                "text",
                interleave(
                        text(),
                        range(name("bold"), text())
                )
        )

        val validator = Validator.ofPattern(schemaPattern)

        // [text}Text[bold}Bold{bold]Text{text]
        val qName = qName("text")
        val startE = Events.startTagEvent(qName)
        val context = Basics.context()
        val textE = Events.textEvent("Text", context)
        val startBoldE = Events.startTagEvent(qName("bold"))
        val endBoldE = Events.endTagEvent(qName("bold"))
        val boldTextE = Events.textEvent("Bold", context)
        val endE = Events.endTagEvent(qName)
        val validEvents = ArrayList(asList(startE, textE, startBoldE, boldTextE, endBoldE, textE, endE))

        assertThat(validator.validate(validEvents)).isSuccess.hasNoUnexpectedEvent()
    }

    // test cases from Jeni's validate-lmnl.xsl
    @Test
    fun validatingPlainText() {
        // <text />
        val schema = text()

        val someText = Events.textEvent("...")

        // ...
        val events = ArrayList<Event>(listOf<Event>(someText))

        assertValidationSucceeds(schema, events)
    }

    @Test
    fun validatingPlainTextAgainstElementPatternIIsInvalid() {
        // <element name="foo">
        //   <text />
        // </element>
        val schema = element("foo",
                text()
        )

        val someText = Events.textEvent("...")

        // ...
        val events = ArrayList<Event>(listOf<Event>(someText))

        assertValidationFailsWithUnexpectedEvent(schema, events, someText)
    }

    @Test
    fun validatingSingleRangeAgainstElementPattern() {
        // <element name="foo">
        //   <text />
        // </element>
        val schema = element("foo",
                text()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationSucceeds(schema, events)
    }

    @Test
    fun validatingSingleRangeAgainstElementPatternWithInvalidExtraCloseFoo() {
        // <element name="foo">
        //   <text />
        // </element>
        val schema = element("foo",
                text()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, closeFoo)
    }

    @Test
    fun validatingSingleRangeAgainstElementPatternInvalidName() {
        // <element name="bar">
        //   <text />
        // </element>
        val schema = element("bar",
                text()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, openFoo)
    }

    @Test
    fun validatingSingleRangeAgainstElementPatternInvalidContent() {
        // <element name="foo">
        //   <empty />
        // </element>
        val schema = element("foo",
                empty()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, someText)
    }

    @Test
    fun validatingSingleRangeAgainstRangePattern() {
        // <range name="foo">
        //   <text />
        //  </range>
        val schema = range(name("foo"),
                text()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationSucceeds(schema, events)
    }

    @Test
    fun validatingSingleRangeAgainstRangePatternInvalidExtraCloseEvent() {
        // <range name="foo">
        //   <text />
        //  </range>
        val schema = range(name("foo"),
                text()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, closeFoo)
    }

    @Test
    fun validatingSingleRangeAgainstRangePatternInvalidName() {
        // <range name="bar">
        //   <text />
        //  </range>
        val schema = range(name("bar"),
                text()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, openFoo)
    }

    @Test
    fun validatingSingleRangeAgainstRangePatternInvalidContent() {
        // <range name="foo">
        //   <empty />
        //  </range>
        val schema = range(name("foo"),
                empty()
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, someText)
    }

    @Test
    fun validatingSingleRangeAgainstChoicePattern() {
        //<choice>
        //  <element name="foo">
        //    <text />
        //  </element>
        //  <range name="foo">
        //    <text />
        //  </range>
        //</choice>
        val schema = choice(
                element("foo",
                        text()
                ),
                range(name("foo"),
                        text()
                )
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationSucceeds(schema, events)
    }

    @Test
    fun validatingSingleRangeAgainstChoicePatternInvalidChoices() {
        //<choice>
        //  <element name="bar">
        //    <text />
        //  </element>
        //  <range name="bar">
        //    <text />
        //  </range>
        //</choice>
        val schema = choice(
                element("bar",
                        text()
                ),
                range(name("bar"),
                        text()
                )
        )

        val foo = qName("foo")
        val openFoo = Events.startTagEvent(foo)
        val closeFoo = Events.endTagEvent(foo)
        val someText = Events.textEvent("...")

        // [foo}...{foo]
        val events = ArrayList(asList(openFoo, someText, closeFoo))

        assertValidationFailsWithUnexpectedEvent(schema, events, openFoo)
    }

    @Test
    fun validatingRangeWithRangeContentAgainstRangePattern() {
        //<range name="foo">
        //  <range name="bar">
        //    <text />
        //  </range>
        //</range>
        val schema = range(name("foo"),
                range(name("bar"),
                        text()
                )
        )

        val foo = qName("foo")
        val bar = qName("bar")
        val openFoo = Events.startTagEvent(foo)
        val openBar = Events.startTagEvent(bar)
        val someText = Events.textEvent("...")
        val closeBar = Events.endTagEvent(bar)
        val closeFoo = Events.endTagEvent(foo)

        // [foo}[bar}...{bar]{foo]
        val events = ArrayList(asList(openFoo, openBar, someText, closeBar, closeFoo))

        assertValidationSucceeds(schema, events)
    }

    @Test
    fun validatingElementWithElementContentAgainstRangePattern() {
        //<element name="foo">
        //  <element name="bar">
        //    <text />
        //  </element>
        //</element>
        val schema = element("foo",
                element("bar",
                        text()
                )
        )

        val foo = qName("foo")
        val bar = qName("bar")
        val openFoo = Events.startTagEvent(foo)
        val openBar = Events.startTagEvent(bar)
        val someText = Events.textEvent("...")
        val closeBar = Events.endTagEvent(bar)
        val closeFoo = Events.endTagEvent(foo)

        // [foo}[bar}...{bar]{foo]
        val events = ArrayList(asList(openFoo, openBar, someText, closeBar, closeFoo))

        assertValidationSucceeds(schema, events)
    }

    @Test
    fun validatingElementsAppearingInConcur() {
        val verse = range(name("v"), text())
        val chapter = range(name("chapter"), oneOrMore(verse))

        val heading = element("heading", text())
        val para = range(name("para"), text())
        val section = range(name("section"),
                group(
                        heading,
                        oneOrMore(para)
                )
        )
        val schema = concur(
                oneOrMore(chapter),
                oneOrMore(section)
        )

        val openHeading = Events.startTagEvent(qName("heading"))
        val closeHeading = Events.endTagEvent(qName("heading"))

        val openSection = Events.startTagEvent(qName("section"))
        val closeSection = Events.endTagEvent(qName("section"))

        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val openPara = Events.startTagEvent(qName("para"))
        val closePara = Events.endTagEvent(qName("para"))

        val openV = Events.startTagEvent(qName("v"))
        val closeV = Events.endTagEvent(qName("v"))

        val someText = Events.textEvent(". . .")

        // [chapter}
        // [section}[heading}The creation of the world{heading]
        // [para}[v}...{v][v}...{para]
        // [para}...{v][v}...{v]{para]
        // {section]
        // {chapter]
        val events = ArrayList(asList(
                openChapter,
                openSection, openHeading, someText, closeHeading,
                openPara, openV, someText, closeV, openV, someText, closePara,
                openPara, someText, closeV, openV, someText, closeV, closePara,
                closeSection,
                closeChapter
        ))
        assertValidationSucceeds(schema, events)
    }

    @Ignore
    @Test
    fun validateBibleText() {
        val verse = range(name("verse"), text())
        //    Pattern chapter = range(name("chapter"), text());
        val chapter = range(name("chapter"), oneOrMore(verse))

        val index = range(name("index"), text())
        val indexedText = concurOneOrMore(mixed(zeroOrMore(index)))
        val s = range(name("s"), indexedText)
        val page = range(name("page"), text()) // TODO: How to indicate when a range can't self-overlap?
        val title = element("title", text())
        val heading = element("heading", indexedText)
        val para = range(name("para"),
                concur(
                        //            text(),
                        oneOrMore(verse),
                        oneOrMore(s)
                )
        )
        val section = range(name("section"),
                group(
                        heading,
                        oneOrMore(para)
                )
        )
        val book = element("book",
                concur(
                        oneOrMore(page),
                        group(
                                title,
                                concur(
                                        oneOrMore(chapter),
                                        oneOrMore(section)
                                )
                        )
                )
        )

        val openBook = Events.startTagEvent(qName("book"))
        val closeBook = Events.endTagEvent(qName("book"))

        val openPage = Events.startTagEvent(qName("page"))
        val closePage = Events.endTagEvent(qName("page"))

        val openTitle = Events.startTagEvent(qName("title"))
        val titleText = Events.textEvent("Genesis")
        val closeTitle = Events.endTagEvent(qName("title"))

        val openHeading = Events.startTagEvent(qName("heading"))
        val headingText = Events.textEvent("The flood and the tower of Babel")
        val closeHeading = Events.endTagEvent(qName("heading"))

        val openSection = Events.startTagEvent(qName("section"))
        val closeSection = Events.endTagEvent(qName("section"))

        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val openPara = Events.startTagEvent(qName("para"))
        val closePara = Events.endTagEvent(qName("para"))

        val openS = Events.startTagEvent(qName("s"))
        val closeS = Events.endTagEvent(qName("s"))

        val openVerse = Events.startTagEvent(qName("verse"))
        val closeVerse = Events.endTagEvent(qName("verse"))

        val openIndex1 = Events.startTagEvent(qName("index"), "1")
        val closeIndex1 = Events.endTagEvent(qName("index"), "1")
        val openIndex2 = Events.startTagEvent(qName("index"), "2")
        val closeIndex2 = Events.endTagEvent(qName("index"), "2")

        val someText = Events.textEvent("some text")

        val events = ArrayList(asList(
                openBook, openPage,
                openTitle, titleText, closeTitle,
                openSection,
                openHeading, headingText, closeHeading,
                openChapter,
                openPara, openS,
                openVerse,
                someText,
                openIndex1, someText,
                openIndex2, someText, closeIndex1, someText, closePage,
                openPage, someText, closeIndex2, someText,
                closeS, closeVerse, closePara,
                openPara, openVerse, openS, someText,
                closeVerse, closeChapter,
                openChapter, openVerse, someText,
                closeVerse,
                closeS, closePara,
                closeChapter,
                closeSection,
                closePage, closeBook
                //        , closeBook // <- huh?
        ))
        assertValidationSucceeds(book, events)
    }

    @Test
    fun validateSimplifiedBibleTextWithRootRange() {
        val s = range(name("s"), text())
        val verse = range(name("verse"), text())
        val chapter = range(name("chapter"), oneOrMore(verse))
        val para = range(name("para"),
                concur(
                        oneOrMore(verse),
                        oneOrMore(s)
                )
        )
        val book = range(name("book"),
                concur(
                        oneOrMore(chapter),
                        oneOrMore(para)
                )
        )

        val openBook = Events.startTagEvent(qName("book"))
        val closeBook = Events.endTagEvent(qName("book"))

        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val openPara = Events.startTagEvent(qName("para"))
        val closePara = Events.endTagEvent(qName("para"))

        val openS = Events.startTagEvent(qName("s"))
        val closeS = Events.endTagEvent(qName("s"))

        val openVerse = Events.startTagEvent(qName("verse"))
        val closeVerse = Events.endTagEvent(qName("verse"))


        val someText = Events.textEvent("some text")

        // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
        val events = ArrayList(asList(
                openBook,
                openChapter,
                openPara,
                openVerse,
                openS,
                someText,
                closeVerse,
                closeS,
                closeChapter,
                closePara,
                closeBook
        ))
        assertValidationSucceeds(book, events)
    }

    @Ignore
    @Test
    fun validateSimplifiedBibleTextWithRootRangeAndExtraCloseBookShouldFail() {
        val s = range(name("s"), text())
        val verse = range(name("verse"), text())
        val chapter = range(name("chapter"), oneOrMore(verse))
        val para = range(name("para"),
                concur(
                        oneOrMore(verse),
                        oneOrMore(s)
                )
        )
        val book = range(name("book"),
                concur(
                        oneOrMore(chapter),
                        oneOrMore(para)
                )
        )

        val openBook = Events.startTagEvent(qName("book"))
        val closeBook = Events.endTagEvent(qName("book"))

        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val openPara = Events.startTagEvent(qName("para"))
        val closePara = Events.endTagEvent(qName("para"))

        val openS = Events.startTagEvent(qName("s"))
        val closeS = Events.endTagEvent(qName("s"))

        val openVerse = Events.startTagEvent(qName("verse"))
        val closeVerse = Events.endTagEvent(qName("verse"))


        val someText = Events.textEvent("some text")

        // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
        val events = ArrayList(asList(
                openBook,
                openChapter,
                openPara,
                openVerse,
                openS,
                someText,
                closeVerse,
                closeS,
                closeChapter,
                closePara,
                closeBook,
                closeBook
        ))
        assertValidationFailsWithUnexpectedEvent(book, events, closeBook)
    }

    @Ignore
    @Test
    fun validateSimplifiedBibleTextWithRootElement() {
        val s = range(name("s"), text())
        val verse = range(name("verse"), text())
        val chapter = range(name("chapter"), oneOrMore(verse))
        val para = range(name("para"),
                concur(
                        oneOrMore(verse),
                        oneOrMore(s)
                )
        )
        val book = element("book",
                concur(
                        oneOrMore(chapter),
                        oneOrMore(para)
                )
        )

        val openBook = Events.startTagEvent(qName("book"))
        val closeBook = Events.endTagEvent(qName("book"))

        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val openPara = Events.startTagEvent(qName("para"))
        val closePara = Events.endTagEvent(qName("para"))

        val openS = Events.startTagEvent(qName("s"))
        val closeS = Events.endTagEvent(qName("s"))

        val openVerse = Events.startTagEvent(qName("verse"))
        val closeVerse = Events.endTagEvent(qName("verse"))

        val someText = Events.textEvent("some text")

        // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
        val events = ArrayList(asList(
                openBook,
                openChapter,
                openPara,
                openVerse,
                openS,
                someText,
                closeVerse,
                closeS,
                closeChapter,
                closePara,
                closeBook
        ))
        assertValidationSucceeds(book, events)
    }

    @Ignore
    @Test
    fun validateSimplifiedBibleTextWithRootElementAndExtraCloseBookShouldFail() {
        val s = range(name("s"), text())
        val verse = range(name("verse"), text())
        val chapter = range(name("chapter"), oneOrMore(verse))
        val para = range(name("para"),
                concur(
                        oneOrMore(verse),
                        oneOrMore(s)
                )
        )
        val book = element("book",
                concur(
                        oneOrMore(chapter),
                        oneOrMore(para)
                )
        )

        val openBook = Events.startTagEvent(qName("book"))
        val closeBook = Events.endTagEvent(qName("book"))

        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val openPara = Events.startTagEvent(qName("para"))
        val closePara = Events.endTagEvent(qName("para"))

        val openS = Events.startTagEvent(qName("s"))
        val closeS = Events.endTagEvent(qName("s"))

        val openVerse = Events.startTagEvent(qName("verse"))
        val closeVerse = Events.endTagEvent(qName("verse"))


        val someText = Events.textEvent("some text")

        // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
        val events = ArrayList(asList(
                openBook,
                openChapter,
                openPara,
                openVerse,
                openS,
                someText,
                closeVerse,
                closeS,
                closeChapter,
                closePara,
                closeBook
                , closeBook // <- huh?
        ))
        assertValidationFailsWithUnexpectedEvent(book, events, closeBook)
    }

    @Ignore
    @Test
    @Throws(IOException::class)
    fun extractTests() {
        val xml = FileUtils.readFileToString(File("data/creole/validate-lmnl.xsl"), "UTF-8")
        val doc = Document.createFromXml(xml, true)
        val testVisitor = TestVisitor()
        doc.accept(testVisitor)
        val tests = testVisitor.getTests()
        for (i in tests.indices) {
            val t = tests[i]
            val baseDir = "src/test/resources/"
            val dir = if (t.isValid) "valid" else "invalid"
            //      String lmnlFile = baseDir + dir + "/test_" + i + ".lmnl";
            //      FileUtils.writeStringToFile(new File(lmnlFile), t.getLMNL(), "UTF-8");
            val creoleFile = "$baseDir/test_$i.creole"
            FileUtils.writeStringToFile(File(creoleFile), t.creole, "UTF-8")
        }
    }

    @Ignore
    @Test
    @Throws(IOException::class)
    fun extractParseTests() {
        val xml = FileUtils.readFileToString(File("data/creole/parse-lmnl.xsl"), "UTF-8")
        val doc = Document.createFromXml(xml, true)
        val testVisitor = ParseTestVisitor()
        doc.accept(testVisitor)
        println(testVisitor.testCode)
    }

    /* private methods*/

    private fun validate(schema: Pattern, events: MutableList<Event>): ValidationResult {
        LOG.info("schema=\n{}", Utilities.patternTreeToDepth(schema, 2))
        val validator = Validator.ofPattern(schema)
        return validator.validate(events)
    }

    private fun assertValidationSucceeds(schema: Pattern, events: MutableList<Event>) {
        val validationResult = validate(schema, events)
        assertThat(validationResult)
                .isSuccess
                .hasNoUnexpectedEvent()
    }

    private fun assertValidationFailsWithUnexpectedEvent(schema: Pattern, events: MutableList<Event>, unexpectedEvent: Event) {
        val validationResult = validate(schema, events)
        assertThat(validationResult)
                .isFailure
                .hasUnexpectedEvent(unexpectedEvent)
    }

}
