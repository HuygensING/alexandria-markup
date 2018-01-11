package nl.knaw.huygens.alexandria.creole;

    /*-
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

import static nl.knaw.huygens.alexandria.creole.Constructors.notAllowed;
import nl.knaw.huygens.alexandria.creole.patterns.NotAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Validator {
  private final static Logger LOG = LoggerFactory.getLogger(Validator.class);
  private final Pattern schemaPattern;
  private final ValidationErrorListener errorListener;

  Validator(Pattern schemaPattern) {
    this.schemaPattern = schemaPattern;
    this.errorListener = new ValidationErrorListener();
  }

  public static Validator ofPattern(Pattern schemaPattern) {
    return new Validator(schemaPattern);
  }

  public ValidationResult validate(List<Event> events) {
    Pattern pattern = eventsDeriv(schemaPattern, events);
    LOG.debug("end pattern = {}", pattern);
    return new ValidationResult()//
        .setSuccess(pattern.isNullable())//
        .setUnexpectedEvent(errorListener.getUnexpectedEvent());
  }

  Pattern eventsDeriv(Pattern pattern, List<Event> events) {
//    LOG.debug("expected events: {}", expectedEvents(pattern).stream().map(Event::toString).sorted().distinct().collect(toList()));
//    LOG.debug("pattern:\n{}", patternTreeToDepth(pattern, 10));
//    LOG.debug("leafpatterns:\n{}", leafPatterns(pattern).stream().map(Pattern::toString).distinct().collect(toList()));
    // eventDeriv p [] = p
    if (events.isEmpty()) {
      LOG.debug("\n{}", Utilities.patternTreeToDepth(pattern, 20));
      return pattern;
    }

    //  eventDeriv p (h:t) = eventDeriv (eventDeriv p h) t
    Event head = events.remove(0);
    LOG.debug("{}: {}", head.getClass().getSimpleName(), head);
    Pattern headDeriv = head.eventDeriv(pattern);
//    LOG.debug("\n{}", Utilities.patternTreeToDepth(headDeriv, 20));

    if (headDeriv instanceof NotAllowed) {
      // fail fast
      LOG.error("Unexpected " + head.getClass().getSimpleName() + ": {}", head);
      errorListener.setUnexpectedEvent(head);
      return notAllowed();
    }
    return eventsDeriv(headDeriv, events);
  }

}
