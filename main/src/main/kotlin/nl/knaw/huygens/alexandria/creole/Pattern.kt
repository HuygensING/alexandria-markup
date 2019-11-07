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

import nl.knaw.huygens.alexandria.creole.Constructors.notAllowed
import nl.knaw.huygens.alexandria.creole.patterns.PatternWithTwoPatternParameters
import java.util.function.Function

interface Pattern {

    val isNullable: Boolean

    fun allowsText(): Boolean

    fun allowsAnnotations(): Boolean

    fun onlyAnnotations(): Boolean

    fun textDeriv(cx: Basics.Context, s: String): Pattern {
        // No other patterns can match a text event; the default is specified as
        // textDeriv _ _ _ = NotAllowed
        return notAllowed()
    }

    fun startTagDeriv(qName: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv _ _ _ = NotAllowed
        return notAllowed()
    }

    fun endTagDeriv(qName: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv _ _ _ = NotAllowed
        return notAllowed()
    }

    fun startAnnotationDeriv(qName: Basics.QName): Pattern {
        return notAllowed()
    }

    fun endAnnotationDeriv(qName: Basics.QName): Pattern {
        return notAllowed()
    }

    fun startTagOpenDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        return notAllowed()
    }

    fun applyAfter(f: Function<Pattern, Pattern>): Pattern {
        return notAllowed()
    }

    fun flip(): Pattern {
        if (this !is PatternWithTwoPatternParameters) {
            return this
        }
        val p0 = this
        val p1 = p0.pattern1
        val p2 = p0.pattern2
        try {
            val constructor = javaClass
                    .getConstructor(*arrayOf<Class<*>>(Pattern::class.java, Pattern::class.java))
            return constructor.newInstance(p2, p1)
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

}
