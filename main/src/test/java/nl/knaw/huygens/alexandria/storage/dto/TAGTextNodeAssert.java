package nl.knaw.huygens.alexandria.storage.dto;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.storage.TAGTextNode;

public class TAGTextNodeAssert extends AbstractObjectAssert<TAGTextNodeAssert, TAGTextNode> {

  public TAGTextNodeAssert(final TAGTextNode actual) {
    super(actual, TAGTextNodeAssert.class);
  }

  public TAGTextNodeAssert hasText(final String expectedText) {
    isNotNull();
    String errorMessage = "\nExpected text to be %s, but was %s";
    if (!actual.getText().equals(expectedText)) {
      failWithMessage(errorMessage, expectedText, actual.getText());
    }
    return myself;
  }
}
