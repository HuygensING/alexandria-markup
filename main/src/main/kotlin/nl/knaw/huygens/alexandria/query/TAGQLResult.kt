package nl.knaw.huygens.alexandria.query

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import java.util.*
import java.util.stream.Collectors

class TAGQLResult {
  private val query: String
  private val results: MutableList<TAGQLResult> = ArrayList()
  internal val errors: MutableList<String> = ArrayList()

  constructor(query: String) {
    this.query = query
  }

  constructor() {
    query = ""
  }

  private val values: MutableList<Any> = ArrayList()
  fun addResult(subresult: TAGQLResult) {
    results.add(subresult)
  }

  fun addValue(value: Any) {
    values.add(value)
  }

  fun getValues(): MutableList<Any> {
    if (!isOk()) {
      return ArrayList()
    }
    if (results.isEmpty()) {
      return values
    }
    return if (results.size == 1) {
      results[0].getValues()
    } else results.stream()
        .map { obj: TAGQLResult -> obj.getValues() }
        .collect(Collectors.toList())
  }

  fun isOk(): Boolean {
    return errors.isEmpty()
  }

  fun getErrors(): MutableList<String> {
    return errors
  }

  fun getQuery(): String {
    return query
  }
}
