package nl.knaw.huc.di.tag.sparql

/*-
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

class SPARQLResult @JvmOverloads constructor(val query: String = "") {

  val errors: MutableList<String> = mutableListOf()

  private val results: MutableList<SPARQLResult> = mutableListOf()
  private val values: MutableList<Any> = mutableListOf()

  fun addResult(subResult: SPARQLResult) {
    results.add(subResult)
  }

  fun addValue(value: Any) {
    values.add(value)
  }

  fun getValues(): List<Any> =
      if (!isOk) {
        ArrayList()
      } else if (results.isEmpty()) {
        values
      } else {
        if (results.size == 1) results[0].getValues() else results
            .map { obj: SPARQLResult -> obj.getValues() }

      }

  val isOk: Boolean
    get() = errors.isEmpty()

}
