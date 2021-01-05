package nl.knaw.huc.di.tag.schema

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

import org.assertj.core.api.AbstractObjectAssert
import org.assertj.core.internal.Iterables

class TAGMLSchemaParseResultAssert(actual: TAGMLSchemaParseResult?) : AbstractObjectAssert<TAGMLSchemaParseResultAssert, TAGMLSchemaParseResult>(actual, TAGMLSchemaParseResultAssert::class.java) {

  fun hasNoErrors(): TAGMLSchemaParseResultAssert {
    isNotNull
    val errorMessage = "\nExpected errors to be empty, but was %s"
    if (actual!!.errors.isNotEmpty()) {
      failWithMessage(errorMessage, actual.errors)
    }
    return myself!!
  }

  private var iterables = Iterables.instance()

  fun hasLayers(vararg expectedLayers: String): TAGMLSchemaParseResultAssert {
    iterables.assertContainsAll(
        info, actual!!.schema.getLayers(), listOf(*expectedLayers))
    return myself!!
  }

  fun hasErrors(expectedError: String): TAGMLSchemaParseResultAssert {
    return hasErrors(listOf(expectedError))
  }

  fun hasErrors(expectedErrors: Collection<String>): TAGMLSchemaParseResultAssert {
    iterables.assertContainsAll(info, actual!!.errors, expectedErrors)
    return myself!!
  }

  fun hasSchema(): TAGMLSchemaParseResultAssert {
    isNotNull
    val errorMessage = "\nExpected schema to not be null, but it was."
    if (actual?.schema == null) {
      failWithMessage(errorMessage)
    }
    return myself!!
  }
}
