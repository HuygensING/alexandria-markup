package nl.knaw.huygens.alexandria.creole

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

import org.assertj.core.api.AbstractAssert

class PatternAssert(actual: Pattern) : AbstractAssert<PatternAssert, Pattern>(actual, PatternAssert::class.java) {

    val isNullable: PatternAssert
        get() {
            isNotNull

            val actualNullable = actual.isNullable
            if (!actualNullable) {
                val assertjErrorMessage = "\nExpecting isNullable of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
                failWithMessage(assertjErrorMessage, actual, true, false)
            }

            return myself
        }

    val isNotNullable: PatternAssert
        get() {
            isNotNull

            val actualNotNullable = !actual.isNullable
            if (!actualNotNullable) {
                val assertjErrorMessage = "\nExpecting isNullable of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
                failWithMessage(assertjErrorMessage, actual, false, true)
            }

            return myself
        }

    fun allowsText(): PatternAssert {
        isNotNull

        val allowsText = actual.allowsText()
        if (!allowsText) {
            val assertjErrorMessage = "\nExpecting allowsText of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
            failWithMessage(assertjErrorMessage, actual, true, false)
        }

        return myself
    }

    fun doesNotAllowText(): PatternAssert {
        isNotNull

        val doesNotAllowText = !actual.allowsText()
        if (!doesNotAllowText) {
            val assertjErrorMessage = "\nExpecting allowsText of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
            failWithMessage(assertjErrorMessage, actual, false, true)
        }

        return myself
    }


}
