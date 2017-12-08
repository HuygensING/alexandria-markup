package nl.knaw.huygens.alexandria.lmnl.importer;

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

import static java.util.Arrays.asList;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import nl.knaw.huygens.alexandria.creole.Basics;
import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import nl.knaw.huygens.alexandria.creole.Event;
import nl.knaw.huygens.alexandria.creole.events.Events;
import static nl.knaw.huygens.alexandria.creole.events.Events.*;
import org.junit.Test;

import java.util.List;

public class LMNLImporter2Test {
  private final LMNLImporter2 importer = new LMNLImporter2();

  @Test
  public void testImporter2a() {
    String lmnl = "[range}text{range]";

    Basics.QName qName = qName("range");
    Event startRangeOpen = startTagOpenEvent(qName);
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
