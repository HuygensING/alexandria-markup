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
import nl.knaw.huygens.alexandria.creole.Constructors.empty
import nl.knaw.huygens.alexandria.creole.Pattern

class Partition(pattern: Pattern) : PatternWithOnePatternParameter(pattern) {

    override fun textDeriv(cx: Basics.Context, s: String): Pattern {
        //For Partition, we create an After pattern that contains the derivative.
        //
        //textDeriv cx (Partition p) s =
        //  after (textDeriv cx p s) Empty
        return after(//
                pattern.textDeriv(cx, s), //
                empty()//
        )
    }

    override fun startTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // startTagDeriv (Partition p) qn id =
        //   after (startTagDeriv p qn id) Empty
        return after(//
                pattern.startTagDeriv(qn, id), //
                empty()//
        )
    }

    override fun endTagDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        // endTagDeriv (Partition p) qn id =
        //   after (endTagDeriv p qn id)
        //         Empty
        return after(//
                pattern.endTagDeriv(qn, id), //
                empty()//
        )
    }
}
