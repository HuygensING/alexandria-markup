package nl.knaw.huc.di.tag.validate;

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
import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.internal.Iterables;

import java.util.Collection;

public class TAGValidationResultAssert
    extends AbstractObjectAssert<TAGValidationResultAssert, TAGValidationResult> {
  public TAGValidationResultAssert(final TAGValidationResult actual) {
    super(actual, TAGValidationResultAssert.class);
  }

  public TAGValidationResultAssert isValid() {
    isNotNull();
    String errorMessage = "\n expected isValid to be true, but was false";
    if (!actual.isValid()) {
      failWithMessage(errorMessage);
    }
    return myself;
  }

  public TAGValidationResultAssert isNotValid() {
    isNotNull();
    String errorMessage = "\n expected isValid to be false, but was true";
    if (actual.isValid()) {
      failWithMessage(errorMessage);
    }
    return myself;
  }

  protected Iterables iterables = Iterables.instance();

  public TAGValidationResultAssert hasErrors(Collection<String> expectedErrors) {
    iterables.assertContainsAll(info, actual.errors, expectedErrors);
    return myself;
  }

  public TAGValidationResultAssert hasWarnings(Collection<String> expectedWarnings) {
    iterables.assertContainsAll(info, actual.warnings, expectedWarnings);
    return myself;
  }
}
