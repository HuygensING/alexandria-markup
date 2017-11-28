package nl.knaw.huygens.alexandria.creole;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.NameClasses.name;

public class ValidatorTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testValidator() throws Exception {
    // [text}tekst{text]
    Basics.QName qName = qName("text");
    Event startE = Events.startTagEvent(qName);
    Basics.Context context = Basics.context();
    Event textE = Events.textEvent("tekst", context);
    Event endE = Events.endTagEvent(qName);
    List<Event> validEvents = new ArrayList<>();
    validEvents.addAll(asList(startE, textE, endE));

    Pattern schemaPattern = element(//
        "text",//
        text()//
    );

    Validator validator = Validator.ofPattern(schemaPattern);
    ValidationResult validationResult1 = validator.validate(validEvents);
    assertThat(validationResult1).isSuccess().hasNoUnexpectedEvent();

    List<Event> invalidEvents = new ArrayList<>();
    invalidEvents.addAll(asList(textE, startE, startE, endE));
    ValidationResult validationResult = validator.validate(invalidEvents);
    assertThat(validationResult).isFailure().hasUnexpectedEvent(textE);
  }

  @Test
  public void testValidator2() throws Exception {
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
    List<Event> validEvents = new ArrayList<>();
    validEvents.addAll(asList(startE, textE, startBoldE, boldTextE, endBoldE, textE, endE));

    assertThat(validator.validate(validEvents)).isSuccess().hasNoUnexpectedEvent();
  }

  // test cases from Jeni's validate-lmnl.xsl
  @Test
  public void testValidatePlainText() {
    // <text />
    Pattern schema = text();

    Event someText = Events.textEvent("...");

    List<Event> events = new ArrayList<>();
    // ...
    events.addAll(Collections.singletonList(someText));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void testValidatePlainTextAgainstElementPatternIIsInvalid() {
    // <element name="foo">
    //   <text />
    // </element>
    Pattern schema = element("foo",//
        text()//
    );//

    Event someText = Events.textEvent("...");

    List<Event> events = new ArrayList<>();
    // ...
    events.addAll(Collections.singletonList(someText));

    assertValidationFailsWithUnexpectedEvent(schema, events, someText);
  }


  @Test
  public void testValidateSingleRangeAgainstElementPattern() {
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

    List<Event> events = new ArrayList<>();
    // [foo}...{foo]
    events.addAll(asList(openFoo, someText, closeFoo));

    assertValidationSucceeds(schema, events);
  }

  @Test
  public void testValidateSingleRangeAgainstElementPatternInvalidName() {
    // <element name="foo">
    //   <text />
    // </element>
    Pattern schema = element("bar",//
        text()//
    );//

    Basics.QName foo = qName("foo");
    Event openFoo = Events.startTagEvent(foo);
    Event closeFoo = Events.endTagEvent(foo);
    Event someText = Events.textEvent("...");

    List<Event> events = new ArrayList<>();
    // [foo}...{foo]
    events.addAll(asList(openFoo, someText, closeFoo));

    assertValidationFailsWithUnexpectedEvent(schema, events, openFoo);
  }

  @Test
  public void testValidateElementsAppearingInConcur() {
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
    List<Event> events = new ArrayList<>();
    events.addAll(asList(//
        openChapter,
        openSection, openHeading, someText, closeHeading,
        openPara, openV, someText, closeV, openV, someText, closePara,
        openPara, someText, closeV, openV, someText, closeV, closePara,
        closeSection,
        closeChapter
    ));
    assertValidationSucceeds(schema, events);
  }

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
