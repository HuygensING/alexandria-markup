package nl.knaw.huygens.alexandria.creole;

/*
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
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class DerivativesTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testEventsDerivation() {
    // [text}tekst{text]
    Basics.QName qName = Basics.qName("text");
    Event startE = Events.startTagEvent(qName);
    Basics.Context context = Basics.context();
    Event textE = Events.textEvent("tekst", context);
    Event endE = Events.endTagEvent(qName);
    List<Event> events = new ArrayList<>();
    events.addAll(asList(startE, textE, endE));

    NameClass nameClass = NameClasses.name("text");
    Pattern schemaPattern = Patterns.partition(//
        Patterns.range(//
            nameClass,//
            Patterns.text()//
        )//
    );
    Pattern pattern = Derivatives.eventsDeriv(schemaPattern, events);
    LOG.info("derived pattern={}", pattern);
    assertThat(pattern).isEqualTo(Patterns.empty());

    Pattern pattern1 = Derivatives.eventsDeriv(schemaPattern, endE);
    LOG.info("derived pattern={}", pattern1);
    assertThat(pattern1).isEqualTo(Patterns.notAllowed());
  }
}
