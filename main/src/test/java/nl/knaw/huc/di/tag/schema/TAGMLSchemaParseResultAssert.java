package nl.knaw.huc.di.tag.schema;

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

import java.util.Arrays;
import java.util.Collection;

public class TAGMLSchemaParseResultAssert
    extends AbstractObjectAssert<TAGMLSchemaParseResultAssert, TAGMLSchemaParseResult> {

  public TAGMLSchemaParseResultAssert(final TAGMLSchemaParseResult actual) {
    super(actual, TAGMLSchemaParseResultAssert.class);
  }

  public TAGMLSchemaParseResultAssert hasNoErrors() {
    isNotNull();
    String errorMessage = "\nExpected errors to be empty, but was %s";
    if (!actual.errors.isEmpty()) {
      failWithMessage(errorMessage, actual.errors);
    }
    return myself;
  }

  protected Iterables iterables = Iterables.instance();

  public TAGMLSchemaParseResultAssert hasLayers(final String... expectedLayers) {
    iterables.assertContainsAll(info, actual.schema.getLayers(), Arrays.asList(expectedLayers));
    return myself;
  }

  public TAGMLSchemaParseResultAssert hasErrors(final String expectedError) {
    return hasErrors(Arrays.asList(expectedError));
  }

  public TAGMLSchemaParseResultAssert hasErrors(final Collection<String> expectedErrors) {
    iterables.assertContainsAll(info, actual.errors, expectedErrors);
    return myself;
  }

  public TAGMLSchemaParseResultAssert hasSchema() {
    isNotNull();
    String errorMessage = "\nExpected schema to not be null, but it was.";
    if (actual.schema == null) {
      failWithMessage(errorMessage);
    }
    return myself;
  }
}
