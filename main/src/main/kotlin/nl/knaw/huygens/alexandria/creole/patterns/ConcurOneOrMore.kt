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
import nl.knaw.huygens.alexandria.creole.Constructors.anyContent
import nl.knaw.huygens.alexandria.creole.Constructors.choice
import nl.knaw.huygens.alexandria.creole.Constructors.concur
import nl.knaw.huygens.alexandria.creole.Constructors.text
import nl.knaw.huygens.alexandria.creole.Pattern

class ConcurOneOrMore(pattern: Pattern) : PatternWithOnePatternParameter(pattern) {

    override fun textDeriv(cx: Basics.Context, s: String): Pattern {
        //For ConcurOneOrMore, we partially expand the ConcurOneOrMore into a Concur. This mirrors the derivative for
        // OneOrMore, except that a new Concur pattern is constructed rather than a Group, and the second sub-pattern is a
        // choice between a ConcurOneOrMore and Text.

        //textDeriv cx (ConcurOneOrMore p) s =
        //  concur (textDeriv cx p s)
        //         (choice (ConcurOneOrMore p) Text)
        return concur(
                pattern.textDeriv(cx, s),
                choice(ConcurOneOrMore(pattern), text())
        )
    }

    override fun startTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv (ConcurOneOrMore p) qn id =
        //   concur (startTagDeriv p qn id)
        //          (choice (ConcurOneOrMore p) anyContent)
        return concur(
                pattern.startTagDeriv(qn, id),
                choice(ConcurOneOrMore(pattern), anyContent())
        )
    }

    override fun endTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv (ConcurOneOrMore p) qn id =
        //   concur (endTagDeriv p qn id)
        //          (choice (ConcurOneOrMore p) anyContent)
        return concur(
                pattern.endTagDeriv(qn, id),
                choice(ConcurOneOrMore(pattern), anyContent())
        )
    }
}
