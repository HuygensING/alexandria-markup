package nl.knaw.huygens.alexandria.lmnl.importer;

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

import nl.knaw.huygens.alexandria.creole.Basics;
import nl.knaw.huygens.alexandria.creole.Event;
import nl.knaw.huygens.alexandria.creole.events.Events;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.events.Events.*;
import static org.junit.Assert.fail;

public class LMNLImporter2Test {
  private final LMNLImporter2 importer = new LMNLImporter2();

  @Test
  public void testImporter2a() {
    String lmnl = "[range}text{range]";

    Basics.QName qName = qName("range");
    Event startRangeOpen = Events.startTagOpenEvent(qName);
    Event startRangeClose = startTagCloseEvent(qName);
    Event text = Events.textEvent("text");
    Event endRangeOpen = endTagOpenEvent(qName);
    Event endRangeClose = endTagCloseEvent(qName);

    List<Event> expectedEvents = asList(
        startRangeOpen,
        startRangeClose,
        text,
        endRangeOpen,
        endRangeClose
    );

    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void testImporter2b() {
    String lmnl = "...";
    List<Event> events = importer.importLMNL(lmnl);

    Event text = Events.textEvent("...");

    assertThat(events).hasSize(1);
    assertThat(text.equals(events.get(0)));
  }

  @Test
  public void testImporter2c() {
    String lmnl = "[page=ed1n1}bla[page=ed2n1}bla{page=ed2n1]bla{page=ed1n1]";

    Basics.QName page = qName("page");
    String id1 = "ed1n1";
    String id2 = "ed2n1";

    List<Event> expectedEvents = asList(
        startTagOpenEvent(page, id1),
        startTagCloseEvent(page, id1),
        textEvent("bla"),
        startTagOpenEvent(page, id2),
        startTagCloseEvent(page, id2),
        textEvent("bla"),
        endTagOpenEvent(page, id2),
        endTagCloseEvent(page, id2),
        textEvent("bla"),
        endTagOpenEvent(page, id1),
        endTagCloseEvent(page, id1)
    );

    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void testParsingText() {
    String lmnl = "test";
    List<Event> events = importer.importLMNL(lmnl);

    Event text = Events.textEvent("test");

    assertThat(events).hasSize(1);
    assertThat(text.equals(events.get(0)));
  }

  @Test
  public void testParsingEmptyRange() {
    String lmnl = "[test]";

    String name = "test";
    List<Event> expectedEvents = asList(
        startTagOpenEvent(name),
        startTagCloseEvent(name),
        endTagOpenEvent(name),
        endTagCloseEvent(name)
    );

    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void testSimpleAnnotationWithShortenedEndTag() {
    String lmnl = "[foo [bar}...{]]";

    String foo = "foo";
    String bar = "bar";

    List<Event> expectedEvents = asList(
        startTagOpenEvent(foo),//
        startAnnotationOpenEvent(bar),//
        startAnnotationCloseEvent(bar),//
        textEvent("..."),//
        endAnnotationOpenEvent(bar),//
        endAnnotationCloseEvent(bar),//
        startTagCloseEvent(foo),//
        endTagOpenEvent(foo),//
        endTagCloseEvent(foo)
    );

    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL0() {
    // Text
    String lmnl = "test";
    List<Event> expectedEvents = asList(//
        textEvent("test")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

//  @Test
//  public void parseLMNL1() {
//    // Atom
//    String lmnl = "{{test}}";
//    List<Event> expectedEvents = asList(//
//      atomOpenEvent("test"),//
//      atomCloseEvent("test")//
//    );
//    assertEventsAreExpected(lmnl, expectedEvents);
//  }
//
//  @Test
//  public void parseLMNL2() {
//    // Text and atom
//    String lmnl = "...{{test}}...";
//    List<Event> expectedEvents = asList(//
//      textEvent("..."),//
//      atomOpenEvent("test"),//
//      atomCloseEvent("test"),//
//      textEvent("...")//
//    );
//    assertEventsAreExpected(lmnl, expectedEvents);
//  }

  @Test
  public void parseLMNL3() {
    // Empty range
    String lmnl = "[test]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("test"),//
        startTagCloseEvent("test"),//
        endTagOpenEvent("test"),//
        endTagCloseEvent("test")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL4() {
    // Simple range
    String lmnl = "[test}...{test]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("test"),//
        startTagCloseEvent("test"),//
        textEvent("..."),//
        endTagOpenEvent("test"),//
        endTagCloseEvent("test")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL5() {
    // Simple range in text
    String lmnl = "...[test}...{test]...";
    List<Event> expectedEvents = asList(//
        textEvent("..."),//
        startTagOpenEvent("test"),//
        startTagCloseEvent("test"),//
        textEvent("..."),//
        endTagOpenEvent("test"),//
        endTagCloseEvent("test"),//
        textEvent("...")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL6() {
    // Non-overlapping ranges
    String lmnl = "[foo}...{foo]...[bar}...{bar]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startTagCloseEvent("foo"),//
        textEvent("..."),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo"),//
        textEvent("..."),//
        startTagOpenEvent("bar"),//
        startTagCloseEvent("bar"),//
        textEvent("..."),//
        endTagOpenEvent("bar"),//
        endTagCloseEvent("bar")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL7() {
    // Overlapping ranges
    String lmnl = "[foo}...[bar}...{foo]...{bar]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startTagCloseEvent("foo"),//
        textEvent("..."),//
        startTagOpenEvent("bar"),//
        startTagCloseEvent("bar"),//
        textEvent("..."),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo"),//
        textEvent("..."),//
        endTagOpenEvent("bar"),//
        endTagCloseEvent("bar")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL8() {
    // Identical ranges
    String lmnl = "[foo}[bar}...{bar]{foo]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startTagCloseEvent("foo"),//
        startTagOpenEvent("bar"),//
        startTagCloseEvent("bar"),//
        textEvent("..."),//
        endTagOpenEvent("bar"),//
        endTagCloseEvent("bar"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL9() {
    // Simple annotation
    String lmnl = "[foo [bar]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationCloseEvent("bar"),//
        endAnnotationOpenEvent("bar"),//
        endAnnotationCloseEvent("bar"),//
        startTagCloseEvent("foo"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL10() {
    // Text annotation
    String lmnl = "[foo [bar}...{bar]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationCloseEvent("bar"),//
        textEvent("..."),//
        endAnnotationOpenEvent("bar"),//
        endAnnotationCloseEvent("bar"),//
        startTagCloseEvent("foo"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL11() {
    // Annotation with internal range
    String lmnl = "[foo [bar}...[baz]...{bar]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationCloseEvent("bar"),//
        textEvent("..."),//
        startTagOpenEvent("baz"),//
        startTagCloseEvent("baz"),//
        endTagOpenEvent("baz"),//
        endTagCloseEvent("baz"),//
        textEvent("..."),//
        endAnnotationOpenEvent("bar"),//
        endAnnotationCloseEvent("bar"),//
        startTagCloseEvent("foo"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL12() {
    // Two annotations
    String lmnl = "[foo [bar}...{bar] [baz}...{baz]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationCloseEvent("bar"),//
        textEvent("..."),//
        endAnnotationOpenEvent("bar"),//
        endAnnotationCloseEvent("bar"),//
        startAnnotationOpenEvent("baz"),//
        startAnnotationCloseEvent("baz"),//
        textEvent("..."),//
        endAnnotationOpenEvent("baz"),//
        endAnnotationCloseEvent("baz"),//
        startTagCloseEvent("foo"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL13() {
    // Overlapping annotations (error)
    String lmnl = "[foo [bar}...[baz}...{bar]...{baz]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationCloseEvent("bar"),//
        textEvent("..."),//
        startTagOpenEvent("baz"),//
        startTagCloseEvent("baz"),//
        textEvent("...")//
    );
    try {
      assertEventsAreExpected(lmnl, expectedEvents);
      fail();
    } catch (LMNLSyntaxError e) {
      assertThat(e).hasMessage("handleCloseRange: unexpected token: {bar]");
    }
  }

  @Test
  public void parseLMNL14() {
    // Nested annotations
    String lmnl = "[foo [bar [baz}...{baz]}...{bar]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationOpenEvent("baz"),//
        startAnnotationCloseEvent("baz"),//
        textEvent("..."),//
        endAnnotationOpenEvent("baz"),//
        endAnnotationCloseEvent("baz"),//
        startAnnotationCloseEvent("bar"),//
        textEvent("..."),//
        endAnnotationOpenEvent("bar"),//
        endAnnotationCloseEvent("bar"),//
        startTagCloseEvent("foo"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL15() {
    // l range
    String lmnl = "[l}...{l]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("l"),//
        startTagCloseEvent("l"),//
        textEvent("..."),//
        endTagOpenEvent("l"),//
        endTagCloseEvent("l")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Test
  public void parseLMNL16() {
    // Simple annotation with shortened end-tag
    String lmnl = "[foo [bar}...{]]";
    List<Event> expectedEvents = asList(//
        startTagOpenEvent("foo"),//
        startAnnotationOpenEvent("bar"),//
        startAnnotationCloseEvent("bar"),//
        textEvent("..."),//
        endAnnotationOpenEvent("bar"),//
        endAnnotationCloseEvent("bar"),//
        startTagCloseEvent("foo"),//
        endTagOpenEvent("foo"),//
        endTagCloseEvent("foo")//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  @Ignore
  @Test
  public void parseLMNL17() {
    // Namespaced range
    String lmnl = "[!ns x=\"http://www.example.com/\"]\n[x:foo}...{x:foo]";
    Basics.QName foo = qName("http://www.example.com/", "foo");
    List<Event> expectedEvents = asList(//
        textEvent("\n"),//
        startTagOpenEvent(foo),//
        startTagCloseEvent(foo),//
        textEvent("..."),//
        endTagOpenEvent(foo),//
        endTagCloseEvent(foo)//
    );
    assertEventsAreExpected(lmnl, expectedEvents);
  }

  private void assertEventsAreExpected(String lmnl, List<Event> expectedEvents) {
    List<Event> events = importer.importLMNL(lmnl);
    assertThat(events).hasSameSizeAs(expectedEvents);
    for (int i = 0; i < expectedEvents.size(); i++) {
      Event event = events.get(i);
      Event expectedEvent = expectedEvents.get(i);
      assertThat(event).hasSameClassAs(expectedEvent);
      assertThat(event).isEqualToComparingFieldByFieldRecursively(expectedEvent);
    }
  }


}
