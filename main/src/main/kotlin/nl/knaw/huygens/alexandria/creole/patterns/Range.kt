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
import nl.knaw.huygens.alexandria.creole.NameClass
import nl.knaw.huygens.alexandria.creole.Pattern

class Range(val nameClass: NameClass, val pattern: Pattern) : AbstractPattern() {

    init {
        setHashcode(javaClass.hashCode() + nameClass.hashCode() * pattern.hashCode())
    }

    override fun init() {
        nullable = false
        allowsText = false
        allowsAnnotations = false
        onlyAnnotations = false
    }

    override fun startTagDeriv(qName: Basics.QName, id: Basics.Id): Pattern {
        //    startTagDeriv (Range nc p) qn id =
        //    if contains nc qn then group p (EndRange qn id)
        //                    else NotAllowed
        return if (nameClass.contains(qName)//
        )
            group(pattern, endRange(qName, id))//
        else
            notAllowed()
    }

    override fun startTagOpenDeriv(qn: Basics.QName, id: Basics.Id): Pattern {
        return if (nameClass.contains(qn)//
        )
            group(pattern, endRange(qn, id))//
        else
            notAllowed()
    }

    override fun toString(): String {
        return "[$nameClass>"
    }
}
