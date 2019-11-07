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
import nl.knaw.huygens.alexandria.creole.Constructors.concur
import nl.knaw.huygens.alexandria.creole.Pattern

class Concur(pattern1: Pattern, pattern2: Pattern) : PatternWithTwoPatternParameters(pattern1, pattern2) {

    override fun init() {
        nullable = pattern1.isNullable && pattern2.isNullable
        allowsText = pattern1.allowsText() && pattern2.allowsText()
        allowsAnnotations = pattern1.allowsAnnotations() || pattern2.allowsAnnotations()
        onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations()
    }

    override fun textDeriv(cx: Basics.Context, s: String): Pattern {
        //For Concur, text is only allowed if it is allowed by both of the sub-patterns: we create a new Concur whose
        // sub-patterns are the derivatives of the original sub-patterns.
        //
        //textDeriv cx (Concur p1 p2) s =
        //  concur (textDeriv cx p1 s)
        //         (textDeriv cx p2 s)
        return concur(//
                pattern1.textDeriv(cx, s), //
                pattern2.textDeriv(cx, s)//
        )
    }

    override fun startTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv (Concur p1 p2) qn id =
        //   let d1 = startTagDeriv p1 qn id
        //       d2 = startTagDeriv p2 qn id
        //   in choice (choice (concur d1 p2) (concur p1 d2))
        //             (concur d1 d2)
        val d1 = pattern1.startTagDeriv(qn, id)
        val d2 = pattern2.startTagDeriv(qn, id)
        return choice(//
                choice(//
                        concur(d1, pattern2), //
                        concur(pattern1, d2)//
                ), //
                concur(d1, d2)//
        )
    }

    override fun endTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv (Concur p1 p2) qn id =
        //   let d1 = endTagDeriv p1 qn id
        //       d2 = endTagDeriv p2 qn id
        //   in choice (choice (concur d1 p2)
        //                     (concur p1 d2))
        //             (concur d1 d2)
        val d1 = pattern1.endTagDeriv(qn, id)
        val d2 = pattern2.endTagDeriv(qn, id)
        return choice(//
                choice(//
                        concur(d1, pattern2), //
                        concur(pattern1, d2)//
                ), //
                concur(d1, d2)//
        )
    }
}
