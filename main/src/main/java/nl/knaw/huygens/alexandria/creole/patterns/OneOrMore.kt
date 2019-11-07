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
import nl.knaw.huygens.alexandria.creole.Pattern

class OneOrMore(pattern: Pattern) : PatternWithOnePatternParameter(pattern) {

    override fun textDeriv(cx: Basics.Context, s: String): Pattern {
        // textDeriv cx (OneOrMore p) s =
        //   group (textDeriv cx p s)
        //         (choice (OneOrMore p) Empty)
        return group(//
                pattern.textDeriv(cx, s), //
                choice(OneOrMore(pattern), empty())//
        )
    }

    override fun startTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv (OneOrMore p) qn id =
        //   group (startTagDeriv p qn id)
        //         (choice (OneOrMore p) Empty)
        return group(//
                pattern.startTagDeriv(qn, id), //
                choice(OneOrMore(pattern), empty())//
        )
    }

    override fun endTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv (OneOrMore p) qn id =
        //   group (endTagDeriv p qn id)
        //         (choice (OneOrMore p) Empty)
        return group(//
                pattern.endTagDeriv(qn, id), //
                choice(OneOrMore(pattern), empty())//
        )
    }
}
