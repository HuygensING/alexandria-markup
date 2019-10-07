package nl.knaw.huygens.alexandria.creole;

    /*-
     * #%L
 * alexandria-markup
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

import static java.util.Arrays.asList;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.NameClasses.name;
import nl.knaw.huygens.alexandria.creole.events.Events;
import nl.knaw.huygens.tei.Document;
import org.apache.commons.io.FileUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ValidatorTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testValidator() {
    // [text}tekst{text]
    Basics.QName qName = qName("text");
    Event startE = Events.startTagEvent(qName);
    Basics.Context context = Basics.context();
    Event textE = Events.textEvent("tekst", context);
    Event endE = Events.endTagEvent(qName);
    List<Event> validEvents = new ArrayList<>(asList(startE, textE, endE));

    Pattern schemaPattern = element(//
        "text",//
        text()//
    );

    Validator validator = Validator.ofPattern(schemaPattern);
    ValidationResult validationResult1 = validator.validate(validEvents);
    assertThat(validationResult1).isSuccess().hasNoUnexpectedEvent();

    List<Event> invalidEvents = new ArrayList<>(asList(textE, startE, startE, endE));
    ValidationResult validationResult = validator.validate(invalidEvents);
    assertThat(validationResult).isFailure().hasUnexpectedEvent(textE);
  }

  @Test
  public void testValidator2() {
    Pattern schemaPattern = element(//
        "text",//
        interleave(//
            text(),//
            range(name("bold"), text())//
        )//
    );

    Validator validator = Validator.ofPattern(schemaPattern);

    // [text}Text[bold}Bold{bold]Text{text]
    Basics.QName qName = qName("text");
    Event startE = Events.startTagEvent(qName);
    Basics.Context context = Basics.context();
    Event textE = Events.textEvent("Text", context);
    Event startBoldE = Events.startTagEvent(qName("bold"));
    Event endBoldE = Events.endTagEvent(qName("bold"));
    Event boldTextE = Events.textEvent("Bold", context);
    Event endE = Events.endTagEvent(qName);
    List<Event> validEvents = new ArrayList<>(asList(startE, textE, startBoldE, boldTextE, endBoldE, textE, endE));

    assertThat(validator.validate(validEvents)).isSuccess().hasNoUnexpectedEvent();
  }

  // test cases from Jeni's validate-lmnl.xsl
  @Test
  public void validatingPlainText() {
    // <text />
    Pattern schema = text();

    Event someText = Events.textEvent("...");

    // ...
    List<Event> events = new ArrayList<>(Collections.singletonList(someText));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void validatingPlainTextAgainstElementPatternIIsInvalid() {
    // <element name="foo">
    //   <text />
    // </element>
    Pattern schema = element("foo",//
        text()//
    );//

    Event someText = Events.textEvent("...");

    // ...
    List<Event> events = new ArrayList<>(Collections.singletonList(someText));

    assertValidationFailsWithUnexpectedEvent(schema, events, someText);
  }

  @Test
  public void validatingSingleRangeAgainstElementPattern() {
    // <element name="foo">
    //   <text />
    // </element>
    Pattern schema = element("foo",//
        text()//
    );//

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void validatingSingleRangeAgainstElementPatternWithInvalidExtraCloseFoo() {
    // <element name="foo">
    //   <text />
    // </element>
    Pattern schema = element("foo",//
        text()//
    );//

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, closeFoo);
  }

  @Test
  public void validatingSingleRangeAgainstElementPatternInvalidName() {
    // <element name="bar">
    //   <text />
    // </element>
    Pattern schema = element("bar",//
        text()//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, openFoo);
  }

  @Test
  public void validatingSingleRangeAgainstElementPatternInvalidContent() {
    // <element name="foo">
    //   <empty />
    // </element>
    Pattern schema = element("foo",//
        empty()//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, someText);
  }

  @Test
  public void validatingSingleRangeAgainstRangePattern() {
    // <range name="foo">
    //   <text />
    //  </range>
    Pattern schema = range(name("foo"),//
        text()//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void validatingSingleRangeAgainstRangePatternInvalidExtraCloseEvent() {
    // <range name="foo">
    //   <text />
    //  </range>
    Pattern schema = range(name("foo"),//
        text()//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, closeFoo);
  }

  @Test
  public void validatingSingleRangeAgainstRangePatternInvalidName() {
    // <range name="bar">
    //   <text />
    //  </range>
    Pattern schema = range(name("bar"),//
        text()//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, openFoo);
  }

  @Test
  public void validatingSingleRangeAgainstRangePatternInvalidContent() {
    // <range name="foo">
    //   <empty />
    //  </range>
    Pattern schema = range(name("foo"),//
        empty()//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, someText);
  }

  @Test
  public void validatingSingleRangeAgainstChoicePattern() {
    //<choice>
    //  <element name="foo">
    //    <text />
    //  </element>
    //  <range name="foo">
    //    <text />
    //  </range>
    //</choice>
    Pattern schema = choice(
        element("foo",//
            text()//
        ),//
        range(name("foo"),//
            text()//
        )//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void validatingSingleRangeAgainstChoicePatternInvalidChoices() {
    //<choice>
    //  <element name="bar">
    //    <text />
    //  </element>
    //  <range name="bar">
    //    <text />
    //  </range>
    //</choice>
    Pattern schema = choice(
        element("bar",//
            text()//
        ),//
        range(name("bar"),//
            text()//
        )//
    );

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    // [foo}...{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, someText, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, openFoo);
  }

  @Test
  public void validatingRangeWithRangeContentAgainstRangePattern() {
    //<range name="foo">
    //  <range name="bar">
    //    <text />
    //  </range>
    //</range>
    Pattern schema = range(name("foo"),
        range(name("bar"),
            text()
        )
    );

    Basics.QName foo = qName("foo");
    Basics.QName bar = qName("bar");
    Event openFoo = Events.startTagEvent(foo);
    Event openBar = Events.startTagEvent(bar);
    Event someText = Events.textEvent("...");
    Event closeBar = Events.endTagEvent(bar);
    Event closeFoo = Events.endTagEvent(foo);

    // [foo}[bar}...{bar]{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, openBar, someText, closeBar, closeFoo));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void validatingElementWithElementContentAgainstRangePattern() {
    //<element name="foo">
    //  <element name="bar">
    //    <text />
    //  </element>
    //</element>
    Pattern schema = element("foo",
        element("bar",
            text()
        )
    );

    Basics.QName foo = qName("foo");
    Basics.QName bar = qName("bar");
    Event openFoo = Events.startTagEvent(foo);
    Event openBar = Events.startTagEvent(bar);
    Event someText = Events.textEvent("...");
    Event closeBar = Events.endTagEvent(bar);
    Event closeFoo = Events.endTagEvent(foo);

    // [foo}[bar}...{bar]{foo]
    List<Event> events = new ArrayList<>(asList(openFoo, openBar, someText, closeBar, closeFoo));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void validatingElementsAppearingInConcur() {
    Pattern verse = range(name("v"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));

    Pattern heading = element("heading", text());
    Pattern para = range(name("para"), text());
    Pattern section = range(name("section"),//
        group(//
            heading,//
            oneOrMore(para)//
        )//
    );
    Pattern schema = concur(//
        oneOrMore(chapter),
        oneOrMore(section)
    );

    Event openHeading = Events.startTagEvent(qName("heading"));
    Event closeHeading = Events.endTagEvent(qName("heading"));

    Event openSection = Events.startTagEvent(qName("section"));
    Event closeSection = Events.endTagEvent(qName("section"));

    Event openChapter = Events.startTagEvent(qName("chapter"));
    Event closeChapter = Events.endTagEvent(qName("chapter"));

    Event openPara = Events.startTagEvent(qName("para"));
    Event closePara = Events.endTagEvent(qName("para"));

    Event openV = Events.startTagEvent(qName("v"));
    Event closeV = Events.endTagEvent(qName("v"));

    Event someText = Events.textEvent(". . .");

    // [chapter}
    // [section}[heading}The creation of the world{heading]
    // [para}[v}...{v][v}...{para]
    // [para}...{v][v}...{v]{para]
    // {section]
    // {chapter]
    List<Event> events = new ArrayList<>(asList(//
        openChapter,
        openSection, openHeading, someText, closeHeading,
        openPara, openV, someText, closeV, openV, someText, closePara,
        openPara, someText, closeV, openV, someText, closeV, closePara,
        closeSection,
        closeChapter
    ));
    assertValidationSucceeds(schema, events);
  }

  @Ignore
  @Test
  public void validateBibleText() {
    Pattern verse = range(name("verse"), text());
//    Pattern chapter = range(name("chapter"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));

    Pattern index = range(name("index"), text());
    Pattern indexedText = concurOneOrMore(mixed(zeroOrMore(index)));
    Pattern s = range(name("s"), indexedText);
    Pattern page = range(name("page"), text()); // TODO: How to indicate when a range can't self-overlap?
    Pattern title = element("title", text());
    Pattern heading = element("heading", indexedText);
    Pattern para = range(name("para"),//
        concur(//
//            text(),//
            oneOrMore(verse),//
            oneOrMore(s)
        )//
    );
    Pattern section = range(name("section"),//
        group(//
            heading,//
            oneOrMore(para)//
        )//
    );
    Pattern book = element("book",//
        concur(//
            oneOrMore(page),//
            group(//
                title,//
                concur(//
                    oneOrMore(chapter),//
                    oneOrMore(section)//
                )//
            )//
        )//
    );

    Event openBook = Events.startTagEvent(qName("book"));
    Event closeBook = Events.endTagEvent(qName("book"));

    Event openPage = Events.startTagEvent(qName("page"));
    Event closePage = Events.endTagEvent(qName("page"));

    Event openTitle = Events.startTagEvent(qName("title"));
    Event titleText = Events.textEvent("Genesis");
    Event closeTitle = Events.endTagEvent(qName("title"));

    Event openHeading = Events.startTagEvent(qName("heading"));
    Event headingText = Events.textEvent("The flood and the tower of Babel");
    Event closeHeading = Events.endTagEvent(qName("heading"));

    Event openSection = Events.startTagEvent(qName("section"));
    Event closeSection = Events.endTagEvent(qName("section"));

    Event openChapter = Events.startTagEvent(qName("chapter"));
    Event closeChapter = Events.endTagEvent(qName("chapter"));

    Event openPara = Events.startTagEvent(qName("para"));
    Event closePara = Events.endTagEvent(qName("para"));

    Event openS = Events.startTagEvent(qName("s"));
    Event closeS = Events.endTagEvent(qName("s"));

    Event openVerse = Events.startTagEvent(qName("verse"));
    Event closeVerse = Events.endTagEvent(qName("verse"));

    Event openIndex1 = Events.startTagEvent(qName("index"), "1");
    Event closeIndex1 = Events.endTagEvent(qName("index"), "1");
    Event openIndex2 = Events.startTagEvent(qName("index"), "2");
    Event closeIndex2 = Events.endTagEvent(qName("index"), "2");

    Event someText = Events.textEvent("some text");

    List<Event> events = new ArrayList<>(asList(//
        openBook, openPage,//
        openTitle, titleText, closeTitle,//
        openSection,//
        openHeading, headingText, closeHeading,//
        openChapter,//
        openPara, openS,
        openVerse,
        someText,//
        openIndex1, someText,//
        openIndex2, someText, closeIndex1, someText, closePage,//
        openPage, someText, closeIndex2, someText,//
        closeS, closeVerse, closePara,//
        openPara, openVerse, openS, someText,//
        closeVerse, closeChapter,//
        openChapter, openVerse, someText,//
        closeVerse,
        closeS, closePara,//
        closeChapter,//
        closeSection,//
        closePage, closeBook//
//        , closeBook // <- huh?
    ));
    assertValidationSucceeds(book, events);
  }

  @Test
  public void validateSimplifiedBibleTextWithRootRange() {
    Pattern s = range(name("s"), text());
    Pattern verse = range(name("verse"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));
    Pattern para = range(name("para"),//
        concur(//
            oneOrMore(verse),//
            oneOrMore(s)
        )//
    );
    Pattern book = range(name("book"),//
        concur(//
            oneOrMore(chapter),//
            oneOrMore(para)//
        )//
    );

    Event openBook = Events.startTagEvent(qName("book"));
    Event closeBook = Events.endTagEvent(qName("book"));

    Event openChapter = Events.startTagEvent(qName("chapter"));
    Event closeChapter = Events.endTagEvent(qName("chapter"));

    Event openPara = Events.startTagEvent(qName("para"));
    Event closePara = Events.endTagEvent(qName("para"));

    Event openS = Events.startTagEvent(qName("s"));
    Event closeS = Events.endTagEvent(qName("s"));

    Event openVerse = Events.startTagEvent(qName("verse"));
    Event closeVerse = Events.endTagEvent(qName("verse"));


    Event someText = Events.textEvent("some text");

    // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
    List<Event> events = new ArrayList<>(asList(//
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
    ));
    assertValidationSucceeds(book, events);
  }

  @Ignore
  @Test
  public void validateSimplifiedBibleTextWithRootRangeAndExtraCloseBookShouldFail() {
    Pattern s = range(name("s"), text());
    Pattern verse = range(name("verse"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));
    Pattern para = range(name("para"),//
        concur(//
            oneOrMore(verse),//
            oneOrMore(s)
        )//
    );
    Pattern book = range(name("book"),//
        concur(//
            oneOrMore(chapter),//
            oneOrMore(para)//
        )//
    );

    Event openBook = Events.startTagEvent(qName("book"));
    Event closeBook = Events.endTagEvent(qName("book"));

    Event openChapter = Events.startTagEvent(qName("chapter"));
    Event closeChapter = Events.endTagEvent(qName("chapter"));

    Event openPara = Events.startTagEvent(qName("para"));
    Event closePara = Events.endTagEvent(qName("para"));

    Event openS = Events.startTagEvent(qName("s"));
    Event closeS = Events.endTagEvent(qName("s"));

    Event openVerse = Events.startTagEvent(qName("verse"));
    Event closeVerse = Events.endTagEvent(qName("verse"));


    Event someText = Events.textEvent("some text");

    // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
    List<Event> events = new ArrayList<>(asList(//
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
    ));
    assertValidationFailsWithUnexpectedEvent(book, events, closeBook);
  }

  @Ignore
  @Test
  public void validateSimplifiedBibleTextWithRootElement() {
    Pattern s = range(name("s"), text());
    Pattern verse = range(name("verse"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));
    Pattern para = range(name("para"),//
        concur(//
            oneOrMore(verse),//
            oneOrMore(s)
        )//
    );
    Pattern book = element("book",//
        concur(//
            oneOrMore(chapter),//
            oneOrMore(para)//
        )//
    );

    Event openBook = Events.startTagEvent(qName("book"));
    Event closeBook = Events.endTagEvent(qName("book"));

    Event openChapter = Events.startTagEvent(qName("chapter"));
    Event closeChapter = Events.endTagEvent(qName("chapter"));

    Event openPara = Events.startTagEvent(qName("para"));
    Event closePara = Events.endTagEvent(qName("para"));

    Event openS = Events.startTagEvent(qName("s"));
    Event closeS = Events.endTagEvent(qName("s"));

    Event openVerse = Events.startTagEvent(qName("verse"));
    Event closeVerse = Events.endTagEvent(qName("verse"));

    Event someText = Events.textEvent("some text");

    // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
    List<Event> events = new ArrayList<>(asList(//
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
    ));
    assertValidationSucceeds(book, events);
  }

  @Ignore
  @Test
  public void validateSimplifiedBibleTextWithRootElementAndExtraCloseBookShouldFail() {
    Pattern s = range(name("s"), text());
    Pattern verse = range(name("verse"), text());
    Pattern chapter = range(name("chapter"), oneOrMore(verse));
    Pattern para = range(name("para"),//
        concur(//
            oneOrMore(verse),//
            oneOrMore(s)
        )//
    );
    Pattern book = element("book",//
        concur(//
            oneOrMore(chapter),//
            oneOrMore(para)//
        )//
    );

    Event openBook = Events.startTagEvent(qName("book"));
    Event closeBook = Events.endTagEvent(qName("book"));

    Event openChapter = Events.startTagEvent(qName("chapter"));
    Event closeChapter = Events.endTagEvent(qName("chapter"));

    Event openPara = Events.startTagEvent(qName("para"));
    Event closePara = Events.endTagEvent(qName("para"));

    Event openS = Events.startTagEvent(qName("s"));
    Event closeS = Events.endTagEvent(qName("s"));

    Event openVerse = Events.startTagEvent(qName("verse"));
    Event closeVerse = Events.endTagEvent(qName("verse"));


    Event someText = Events.textEvent("some text");

    // [book}[chapter}[para}[verse}[s}...{verse]{s]{chapter]{para]{book]
    List<Event> events = new ArrayList<>(asList(//
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
        closeBook//
        , closeBook // <- huh?
    ));
    assertValidationFailsWithUnexpectedEvent(book, events, closeBook);
  }

  @Ignore
  @Test
  public void extractTests() throws IOException {
    String xml = FileUtils.readFileToString(new File("data/creole/validate-lmnl.xsl"), "UTF-8");
    Document doc = Document.createFromXml(xml, true);
    TestVisitor testVisitor = new TestVisitor();
    doc.accept(testVisitor);
    List<LMNLTest> tests = testVisitor.getTests();
    for (int i = 0; i < tests.size(); i++) {
      LMNLTest t = tests.get(i);
      String baseDir = "src/test/resources/";
      String dir = t.isValid() ? "valid" : "invalid";
//      String lmnlFile = baseDir + dir + "/test_" + i + ".lmnl";
//      FileUtils.writeStringToFile(new File(lmnlFile), t.getLMNL(), "UTF-8");
      String creoleFile = baseDir + "/test_" + i + ".creole";
      FileUtils.writeStringToFile(new File(creoleFile), t.getCreole(), "UTF-8");
    }
  }

  @Ignore
  @Test
  public void extractParseTests() throws IOException {
    String xml = FileUtils.readFileToString(new File("data/creole/parse-lmnl.xsl"), "UTF-8");
    Document doc = Document.createFromXml(xml, true);
    ParseTestVisitor testVisitor = new ParseTestVisitor();
    doc.accept(testVisitor);
    System.out.println(testVisitor.getTestCode());
  }

  /* private methods*/

  private ValidationResult validate(Pattern schema, List<Event> events) {
    LOG.info("schema=\n{}", Utilities.patternTreeToDepth(schema, 2));
    Validator validator = Validator.ofPattern(schema);
    return validator.validate(events);
  }

  private void assertValidationSucceeds(Pattern schema, List<Event> events) {
    ValidationResult validationResult = validate(schema, events);
    assertThat(validationResult)//
        .isSuccess()//
        .hasNoUnexpectedEvent();
  }

  private void assertValidationFailsWithUnexpectedEvent(Pattern schema, List<Event> events, Event unexpectedEvent) {
    ValidationResult validationResult = validate(schema, events);
    assertThat(validationResult)//
        .isFailure()//
        .hasUnexpectedEvent(unexpectedEvent);
  }

}
