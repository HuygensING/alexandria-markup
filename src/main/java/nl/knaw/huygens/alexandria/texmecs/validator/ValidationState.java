package nl.knaw.huygens.alexandria.texmecs.validator;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.texmecs.validator.events.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ValidationState {
  public enum StateValue {valid, invalid, validating}

  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private StateValue value = StateValue.validating;
  private List<ValidationEvent> expectedEvents = new ArrayList<>();

  public ValidationState(TexMECSSchema texMECSSchema) {
    expectedEvents.add(texMECSSchema.getRootEvent());
  }

  public StateValue getValue() {
    return value;
  }

  private void setExpectedEvents(List<ValidationEvent> expectedEvents) {
    this.expectedEvents = expectedEvents;
    LOG.info("expectedEvents = {}", expectedEvents);
  }

  public void process(ValidationEvent event) {
    LOG.info("event = {}", event);
    if (isExpected(event)) {
      List<ValidationEvent> expectedEvents = getExpectedEventsAfter(event);
      setExpectedEvents(expectedEvents);
      if (expectedEvents.isEmpty()) {
        value = StateValue.valid;
      }
      return;
    }
    value = StateValue.invalid;
    throw new ValidationException("unexpected event: " + event + ", expected one of: " + expectedEvents);
  }

  private List<ValidationEvent> getExpectedEventsAfter(ValidationEvent validationEvent) {
    List<ValidationEvent> events = new ArrayList<>();
    String root = "text";
    if (validationEvent instanceof TextEvent) {
      events.add(new CloseTagEvent(root));

    } else if (validationEvent.equals(new OpenTagEvent(root))) {
      events.add(new AnyTextEvent());
    }
    return events;
  }

  private boolean isExpected(ValidationEvent event) {
    return expectedEvents.stream()//
        .anyMatch(expected -> eventsMatch(expected, event));
  }

  private boolean eventsMatch(ValidationEvent expected, ValidationEvent event) {
    return expected != null && event != null//
        && (//
        (expected instanceof AnyTextEvent && event instanceof TextEvent)//
            || (expected instanceof TextEvent && event instanceof AnyTextEvent)//
            || expected.equals(event)//
    );
  }

}
