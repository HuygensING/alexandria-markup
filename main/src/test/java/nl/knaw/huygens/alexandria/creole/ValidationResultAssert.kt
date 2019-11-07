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
import org.assertj.core.util.Objects

class ValidationResultAssert(actual: ValidationResult) : AbstractAssert<ValidationResultAssert, ValidationResult>(actual, ValidationResultAssert::class.java) {

    val isSuccess: ValidationResultAssert
        get() {
            isNotNull

            val actualSuccess = actual.isSuccess()
            if (!actualSuccess) {
                val assertjErrorMessage = "\nExpecting success of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
                failWithMessage(assertjErrorMessage, actual, true, false)
            }

            return myself
        }

    val isFailure: ValidationResultAssert
        get() {
            isNotNull

            val actualFailure = !actual.isSuccess()
            if (!actualFailure) {
                val assertjErrorMessage = "\nExpecting failure of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
                failWithMessage(assertjErrorMessage, actual, true, false)
            }

            return myself
        }

    fun hasUnexpectedEvent(unexpectedEvent: Event): ValidationResultAssert {
        isNotNull

        val actualUnexpectedEvent = actual.getUnexpectedEvent()
        if (!Objects.areEqual(actualUnexpectedEvent, unexpectedEvent)) {
            val assertjErrorMessage = "\nExpecting unexpectedEvent of:\n  <%s>\nto be:\n  <%s>\nbut was:\n  <%s>"
            failWithMessage(assertjErrorMessage, actual, unexpectedEvent, actualUnexpectedEvent)
        }
        return myself
    }

    fun hasNoUnexpectedEvent(): ValidationResultAssert {
        isNotNull

        if (actual.getUnexpectedEvent() != null) {
            val assertjErrorMessage = "\nExpecting :\n  <%s>\nnot to have unexpectedEvent but had :\n  <%s>"
            failWithMessage(assertjErrorMessage, actual, actual.getUnexpectedEvent())
        }

        return myself

    }
}
