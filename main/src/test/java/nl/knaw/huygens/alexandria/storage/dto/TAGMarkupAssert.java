package nl.knaw.huygens.alexandria.storage.dto;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.assertj.core.api.AbstractObjectAssert;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMarkupAssert extends AbstractObjectAssert<TAGMarkupAssert, TAGMarkup> {

  public TAGMarkupAssert(final TAGMarkup actual) {
    super(actual, TAGMarkupAssert.class);
  }

  public TAGMarkupAssert isOptional() {
    isNotNull();
    String errorMessage = "\nExpected markup %s to be optional, but it wasn't.";
    if (!actual.isOptional()) {
      failWithMessage(errorMessage, actual);
    }
    return myself;
  }

  public TAGMarkupAssert withTextNodesWithText(String... text) {
    isNotNull();
    List<String> actualTexts = actual.getTextNodeStream().map(TAGTextNode::getText).collect(toList());
    assertThat(actualTexts).containsExactly(text);
    return myself;
  }

  public void hasMarkupId(String markupId) {
    isNotNull();
    String actualMarkupId = actual.getMarkupId();
    if (!markupId.equals(actualMarkupId)) {
      failWithMessage("\nExpected markup %s to have markupId %s, but was %s.",
          actual, markupId, actualMarkupId);
    }

  }
}
