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

import nl.knaw.huygens.alexandria.creole.Basics
import nl.knaw.huygens.alexandria.creole.Constructors.choice
import nl.knaw.huygens.alexandria.creole.Pattern

import java.util.function.Function

class Choice(pattern1: Pattern, pattern2: Pattern) : PatternWithTwoPatternParameters(pattern1, pattern2) {

    override fun init() {
        nullable = pattern1.isNullable || pattern2.isNullable
        allowsText = pattern1.allowsText() || pattern2.allowsText()
        allowsAnnotations = pattern1.allowsAnnotations() || pattern2.allowsAnnotations()
        onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations()
    }

    override fun textDeriv(cx: Basics.Context, s: String): Pattern {
        // textDeriv cx (Choice p1 p2) s =
        //  choice (textDeriv cx p1 s) (textDeriv cx p2 s)
        return choice(
                pattern1.textDeriv(cx, s),
                pattern2.textDeriv(cx, s)
        )
    }

    override fun startTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv (Choice p1 p2) qn id =
        //   choice (startTagDeriv p1 qn id)
        //          (startTagDeriv p2 qn id)
        return choice(
                pattern1.startTagDeriv(qn, id),
                pattern2.startTagDeriv(qn, id)
        )
    }

    override fun startTagOpenDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        return choice(
                pattern1.startTagOpenDeriv(qn, id),
                pattern2.startTagOpenDeriv(qn, id)
        )
    }

    override fun endTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv (Choice p1 p2) qn id =
        //   choice (endTagDeriv p1 qn id)
        //          (endTagDeriv p2 qn id)
        return choice(
                pattern1.endTagDeriv(qn, id),
                pattern2.endTagDeriv(qn, id)
        )
    }

    override fun startAnnotationDeriv(qn: Basics.QName): Pattern {
        return choice(
                pattern1.startAnnotationDeriv(qn),
                pattern2.startAnnotationDeriv(qn)
        )
    }

    override fun endAnnotationDeriv(qn: Basics.QName): Pattern {
        return choice(
                pattern1.endAnnotationDeriv(qn),
                pattern2.endAnnotationDeriv(qn)
        )
    }

    override fun applyAfter(f: Function<Pattern, Pattern>): Pattern {
        return choice(
                pattern1.applyAfter(f),
                pattern2.applyAfter(f)
        )
    }

}
