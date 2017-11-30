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

import static nl.knaw.huygens.alexandria.creole.Utilities.nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Validator {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  private final Pattern schemaPattern;

  private Validator(Pattern schemaPattern) {
    this.schemaPattern = schemaPattern;
  }

  public static Validator ofPattern(Pattern schemaPattern) {
    return new Validator(schemaPattern);
  }

  public ValidationResult validate(List<Event> events) {
    ValidationErrorListener errorListener = new ValidationErrorListener();
    Derivatives derivatives = new Derivatives(errorListener);
    Pattern pattern = derivatives.eventsDeriv(schemaPattern, events);
    LOG.debug("end pattern = {}", pattern);
    return new ValidationResult()//
        .setSuccess(nullable(pattern))//
        .setUnexpectedEvent(errorListener.getUnexpectedEvent());
  }
}
