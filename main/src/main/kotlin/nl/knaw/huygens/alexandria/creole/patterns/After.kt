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
import nl.knaw.huygens.alexandria.creole.Constructors.after
import nl.knaw.huygens.alexandria.creole.Pattern

import java.util.function.Function

class After(pattern1: Pattern, pattern2: Pattern) : PatternWithTwoPatternParameters(pattern1, pattern2) {

    override fun init() {
        nullable = false
        allowsText = if (pattern1.isNullable//
        )
            pattern1.allowsText() || pattern2.allowsText()//
        else
            pattern1.allowsText()
        allowsAnnotations = if (pattern1.isNullable//
        )
            pattern1.allowsAnnotations() || pattern2.allowsAnnotations()//
        else
            pattern1.allowsAnnotations()
        onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations()
    }

    override fun textDeriv(cx: Basics.Context, s: String): Pattern {
        //textDeriv cx (After p1 p2) s =
        //  after (textDeriv cx p1 s) p2
        return after(//
                pattern1.textDeriv(cx, s), //
                pattern2//
        )
    }

    override fun startTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv (After p1 p2) qn id =
        //   after (startTagDeriv p1 qn id)
        //         p2
        return after(//
                pattern1.startTagDeriv(qn, id), //
                pattern2//
        )
    }

    override fun endTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv (After p1 p2) qn id =
        //   after (endTagDeriv p1 qn id) p2
        return after(//
                pattern1.endTagDeriv(qn, id), //
                pattern2//
        )
    }

    override fun applyAfter(f: Function<Pattern, Pattern>): Pattern {
        return after(//
                pattern1, //
                f.apply(pattern2)//
        )
    }
}
