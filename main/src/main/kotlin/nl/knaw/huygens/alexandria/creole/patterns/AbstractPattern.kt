package nl.knaw.huygens.alexandria.creole.patterns

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

import com.google.common.base.Preconditions
import nl.knaw.huygens.alexandria.creole.Pattern

internal abstract class AbstractPattern : Pattern {
    var nullable: Boolean? = null
    var allowsText: Boolean? = null
    var allowsAnnotations: Boolean? = null
    var onlyAnnotations: Boolean? = null

    var hashcode = javaClass.hashCode()

    override//
    val isNullable: Boolean
        get() {
            if (nullable == null) {
                init()
                if (nullable == null) {
                    throw RuntimeException("nullable == null! Make sure nullable is initialized in the init() of " + javaClass.getSimpleName())
                }
            }
            return nullable!!
        }

    internal abstract fun init()

    fun setHashcode(hashcode: Int) {
        Preconditions.checkState(hashcode != 0, "hashCode should not be 0!")
        this.hashcode = hashcode
    }

    override fun allowsText(): Boolean {
        if (allowsText == null) {
            init()
            if (allowsText == null) {
                throw RuntimeException("allowsText == null! Make sure allowsText is initialized in the init() of " //
                        + javaClass.getSimpleName())
            }
        }
        return allowsText!!
    }

    override fun allowsAnnotations(): Boolean {
        if (allowsAnnotations == null) {
            init()
            if (allowsAnnotations == null) {
                throw RuntimeException("allowsAnnotations == null! Make sure allowsAnnotations is initialized in the init() of " //
                        + javaClass.getSimpleName())
            }
        }
        return allowsAnnotations!!
    }

    override fun onlyAnnotations(): Boolean {
        if (onlyAnnotations == null) {
            init()
            if (onlyAnnotations == null) {
                throw RuntimeException("onlyAnnotations == null! Make sure onlyAnnotations is initialized in the init() of " //
                        + javaClass.getSimpleName())
            }
        }
        return onlyAnnotations!!
    }

    override fun hashCode(): Int {
        return hashcode
    }
}
