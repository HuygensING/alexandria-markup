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

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.util.Objects;

public class ValidationResultAssert extends AbstractAssert<ValidationResultAssert, ValidationResult> {

  public ValidationResultAssert(ValidationResult actual) {
    super(actual, ValidationResultAssert.class);
  }

  public ValidationResultAssert isSuccess() {
    isNotNull();

    boolean actualSuccess = actual.isSuccess();
    if (!actualSuccess) {
      String assertjErrorMessage = "\nExpecting success of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, true, false);
    }

    return myself;
  }

  public ValidationResultAssert isFailure() {
    isNotNull();

    boolean actualFailure = !actual.isSuccess();
    if (!actualFailure) {
      String assertjErrorMessage = "\nExpecting failure of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, true, false);
    }

    return myself;
  }

  public ValidationResultAssert hasUnexpectedEvent(Event unexpectedEvent) {
    isNotNull();

    Event actualUnexpectedEvent = actual.getUnexpectedEvent();
    if (!Objects.areEqual(actualUnexpectedEvent, unexpectedEvent)) {
      String assertjErrorMessage = "\nExpecting unexpectedEvent of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, unexpectedEvent, actualUnexpectedEvent);
    }
    return myself;
  }

  public ValidationResultAssert hasNoUnexpectedEvent() {
    isNotNull();

    if (actual.getUnexpectedEvent() != null) {
      String assertjErrorMessage = "\nExpecting :\n  <%s>\nnot to have unexpectedEvent but had :\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, actual.getUnexpectedEvent());
    }

    return myself;

  }
}