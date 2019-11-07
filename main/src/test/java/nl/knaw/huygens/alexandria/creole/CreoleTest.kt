package nl.knaw.huygens.alexandria.creole

/*
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

import nl.knaw.huygens.alexandria.creole.patterns.NotAllowed
import nl.knaw.huygens.alexandria.creole.patterns.Text

open class CreoleTest {
    //  SoftAssertions softly = new SoftAssertions();

    class TestPattern : Pattern {
        override val isNullable: Boolean
            get() = false

        override fun allowsText(): Boolean {
            return false
        }

        override fun allowsAnnotations(): Boolean {
            return false
        }

        override fun onlyAnnotations(): Boolean {
            return false
        }
    }

    internal class NullablePattern : Text()

    internal class NotNullablePattern : NotAllowed()

    companion object {

        internal val NULLABLE_PATTERN: Pattern = NullablePattern()

        internal val NOT_NULLABLE_PATTERN: Pattern = NotNullablePattern()
    }


}
