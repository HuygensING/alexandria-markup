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
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.Events.endTagEvent;
import static nl.knaw.huygens.alexandria.creole.Events.startTagEvent;
import nl.knaw.huygens.alexandria.creole.Event;
import nl.knaw.huygens.alexandria.creole.Events;
import org.junit.Test;

import java.util.List;

public class LMNLImporter2Test {
  final LMNLImporter2 importer = new LMNLImporter2();

  @Test
  public void testImporter2a() {
    String lmnl = "[range}text{range]";
    List<Event> events = importer.importLMNL(lmnl);

    Event openRange = startTagEvent(qName("range"));
    Event text = Events.textEvent("text");
    Event closeRange = endTagEvent(qName("range"));

    assertThat(events).hasSize(3);
    assertThat(openRange.equals(events.get(0)));
    assertThat(text.equals(events.get(1)));
    assertThat(closeRange.equals(events.get(2)));
  }

  @Test
  public void testImporter2b() {
    String lmnl = "...";
    List<Event> events = importer.importLMNL(lmnl);

    Event text = Events.textEvent("...");

    assertThat(events).hasSize(1);
    assertThat(text.equals(events.get(0)));
  }

}
