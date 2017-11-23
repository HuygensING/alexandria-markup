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
import static java.util.Arrays.asList;
import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.Constructors.element;
import static nl.knaw.huygens.alexandria.creole.Constructors.text;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class ValidatorTest {
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

    Validator validator = Validator.ofSchema(schemaPattern);
    assertThat(validator.validates(validEvents)).isTrue();

    List<Event> invalidEvents = new ArrayList<>();
    invalidEvents.addAll(asList(textE, startE, startE, endE));
    assertThat(validator.validates(invalidEvents)).isFalse();

  }

}
