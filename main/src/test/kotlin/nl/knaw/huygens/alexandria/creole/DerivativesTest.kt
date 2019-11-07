package nl.knaw.huygens.alexandria.creole

/*
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
import nl.knaw.huygens.alexandria.creole.Constructors.concur
import nl.knaw.huygens.alexandria.creole.Constructors.concurOneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.element
import nl.knaw.huygens.alexandria.creole.Constructors.empty
import nl.knaw.huygens.alexandria.creole.Constructors.group
import nl.knaw.huygens.alexandria.creole.Constructors.mixed
import nl.knaw.huygens.alexandria.creole.Constructors.notAllowed
import nl.knaw.huygens.alexandria.creole.Constructors.oneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.range
import nl.knaw.huygens.alexandria.creole.Constructors.text
import nl.knaw.huygens.alexandria.creole.Constructors.zeroOrMore
import nl.knaw.huygens.alexandria.creole.NameClasses.name
import nl.knaw.huygens.alexandria.creole.events.Events
import org.junit.Test
import org.slf4j.LoggerFactory
import java.util.*
import java.util.Arrays.asList

class DerivativesTest : CreoleTest() {
    private val LOG = LoggerFactory.getLogger(javaClass)

    @Test
    fun testEventsDerivation() {
        val page = range(name("page"), text())
        val chapter = range(name("chapter"), text())
        val schemaPattern = element(
                "text",
                concur(
                        page,
                        chapter
                )
        )

        // [text}[page}tekst{page]{text]
        val qName = qName("text")
        val startE = Events.startTagEvent(qName)
        val context = Basics.context()
        val textE = Events.textEvent("tekst", context)
        val endE = Events.endTagEvent(qName)
        val openPage = Events.startTagEvent(qName("page"))
        val closePage = Events.endTagEvent(qName("page"))
        val openChapter = Events.startTagEvent(qName("chapter"))
        val closeChapter = Events.endTagEvent(qName("chapter"))

        val events = mutableListOf(startE, openPage, openChapter, textE, closePage, closeChapter, endE)
        assertEventsAreValidForSchema(schemaPattern, events)

        val events2 = mutableListOf(startE, openChapter, openPage, textE, closePage, closeChapter, endE)
        assertEventsAreValidForSchema(schemaPattern, events2)

    }

    @Test
    fun testEventsDerivation2() {
        // [text}tekst{text]
        val qName = qName("text")
        val startE = Events.startTagEvent(qName)
        val context = Basics.context()
        val textE = Events.textEvent("tekst", context)
        val endE = Events.endTagEvent(qName)
        val events = mutableListOf(startE, textE, endE)

        val schemaPattern = element(
                "text",
                text()
        )

        val pattern = Validator(schemaPattern).eventsDeriv(schemaPattern, events)
        LOG.info("derived pattern={}", pattern)
        assertThat(pattern).isEqualTo(empty())

        val pattern1 = endE.eventDeriv(schemaPattern)
        LOG.info("derived pattern={}", pattern1)
        assertThat(pattern1).isEqualTo(notAllowed())
    }

    @Test
    fun testBiblicalExample() {
        val book = createSchema()
        val events = createEvents()

        assertEventsAreValidForSchema(book, events)
    }

    private fun createEvents(): MutableList<Event> {
        //<book|<page no="1"|
        //  <title|Genesis|title>
        //  ...
        //  <section|
        //    <heading|The flood and the tower of Babel|heading>
        //    ...
        //    <chapter no="7"|
        //      ...
        //      <para|...<s|<verse no="23"|God wiped out every living thing
        //    that existed on earth, <index~1 ref="i0037"|man and
        //        <index~2 ref="i0038"|beast|index~1>, reptile|page>
        //      <page no="74"|and bird|index~2>; they were all wiped out
        //    over the whole earth, and only Noah and his company in the
        //    ark survived.|s>|verse>|para>
        //      <para|<verse no="24"|<s|When the waters had increased over
        //    the earth for a hundred and fifty days, |verse>|chapter>
        //      <chapter no="8"|<verse no="1"|God thought of Noah and all
        //    the wild animals and the cattle with him in the ark, and
        //    he made a wind pass over the earth, and the waters began to
        //    subside.|verse>|s>...|para>
        //      ...
        //    |chapter>
        //    ...
        //  |section>
        //  ...
        //|page>|book>
        val openBook = Events.startTagEvent(qName("book"))
        val closeBook = Events.endTagEvent(qName("book"))
        val openPage = Events.startTagEvent(qName("page"))
        val closePage = Events.endTagEvent(qName("page"))
        val openTitle = Events.startTagEvent(qName("title"))
        val closeTitle = Events.endTagEvent(qName("title"))
        val titleText = Events.textEvent("Genesis")
        val openHeading = Events.startTagEvent(qName("heading"))
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
        val headingText = Events.textEvent("The flood and the tower of Babel")
        val someText = Events.textEvent("some text")

        return ArrayList(asList(
                openBook, openPage,
                openTitle, titleText, closeTitle,
                openSection,
                openHeading, headingText, closeHeading,
                openChapter,
                openPara, openS, openVerse, someText,
                openIndex1, someText,
                openIndex2, someText, closeIndex1, someText, closePage,
                openPage, someText, closeIndex2, someText,
                closeS, closeVerse, closePara,
                openPara, openVerse, openS, someText,
                closeVerse, closeChapter,
                openChapter, openVerse, someText,
                closeVerse, closeS, closePara,
                closeChapter,
                closeSection,
                closePage, closeBook
                //        , closeBook // <- huh?
        ))
    }

    private fun createSchema(): Pattern {
        //    start = book
        //    book = element book { page ~
        //        ( title, ( chapter+ ~ section+ ) ) }
        //    page = range page { attribute no { text }, text }
        //    title = element title { text }
        //    chapter = range chapter { attribute no { text }, verse+ }
        //    verse = range verse { attribute no { text }, text }
        //    section = range section { heading, para+ }
        //    heading = element heading { indexedText }
        //    para = range para { verse+ ~ s+ }
        //    s = range s { indexedText }
        //    indexedText = concurOneOrMore { mixed { index* } }
        //    index = range index { attribute ref { text }, text }
        val page = range(name("page"), text()) // TODO: How to indicate when a range can't self-overlap?
        val title = element("title", text())
        val verse = range(name("verse"), text())
        val chapter = range(name("chapter"), oneOrMore(verse))
        val index = range(name("index"), text())
        val indexedText = concurOneOrMore(mixed(zeroOrMore(index)))
        val heading = element("heading", indexedText)
        val s = range(name("s"), indexedText)
        val para = range(name("para"),
                concur(
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
        return element("book",
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
    }

    private fun assertEventsAreValidForSchema(schemaPattern: Pattern, events: MutableList<Event>) {
        val pattern1 = Validator(schemaPattern).eventsDeriv(schemaPattern, events)
        //    LOG.info("expected events: {}", expectedEvents(pattern1).stream().map(Event::toString).sorted().distinct().collect(toList()));
        assertThat(pattern1).isNullable
    }

    private fun assertEventsAreInvalidForSchema(schemaPattern: Pattern, events: MutableList<Event>) {
        val pattern1 = Validator(schemaPattern).eventsDeriv(schemaPattern, events)
        assertThat(pattern1)
                .isNotNullable
    }
}
