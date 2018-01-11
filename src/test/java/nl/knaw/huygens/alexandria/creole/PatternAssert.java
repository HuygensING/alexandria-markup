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

import org.assertj.core.api.AbstractAssert;

public class PatternAssert extends AbstractAssert<PatternAssert, Pattern> {

  public PatternAssert(Pattern actual) {
    super(actual, PatternAssert.class);
  }

  public PatternAssert isNullable() {
    isNotNull();

    boolean actualNullable = actual.isNullable();
    if (!actualNullable) {
      String assertjErrorMessage = "\nExpecting isNullable of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, true, false);
    }

    return myself;
  }

  public PatternAssert isNotNullable() {
    isNotNull();

    boolean actualNotNullable = !actual.isNullable();
    if (!actualNotNullable) {
      String assertjErrorMessage = "\nExpecting isNullable of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, false, true);
    }

    return myself;
  }

  public PatternAssert allowsText() {
    isNotNull();

    boolean allowsText = actual.allowsText();
    if (!allowsText) {
      String assertjErrorMessage = "\nExpecting allowsText of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, true, false);
    }

    return myself;
  }

  public PatternAssert doesNotAllowText() {
    isNotNull();

    boolean doesNotAllowText = !actual.allowsText();
    if (!doesNotAllowText) {
      String assertjErrorMessage = "\nExpecting allowsText of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>";
      failWithMessage(assertjErrorMessage, actual, false, true);
    }

    return myself;
  }


}
