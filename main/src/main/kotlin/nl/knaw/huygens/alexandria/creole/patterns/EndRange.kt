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
import nl.knaw.huygens.alexandria.creole.Constructors.empty
import nl.knaw.huygens.alexandria.creole.Constructors.notAllowed
import nl.knaw.huygens.alexandria.creole.Pattern

class EndRange(val qName: Basics.QName, val id: Basics.Id) : AbstractPattern() {

    init {
        setHashcode(javaClass.hashCode() + qName.hashCode() * id.hashCode())
    }

    override fun init() {
        nullable = false
        allowsText = false
        allowsAnnotations = false
        onlyAnnotations = false
    }

    override fun endTagDeriv(qn: Basics.QName, id2: Basics.Id): Pattern {
        // endTagDeriv (EndRange (QName ns1 ln1) id1)
        //             (QName ns2 ln2) id2 =
        //   if id1 == id2 ||
        //      (id1 == '' && id2 == '' && ns1 == ns2 && ln1 == ln2)
        //   then Empty
        //   else NotAllowed
        val ns1 = qn.uri
        val ln1 = qn.localName
        val ns2 = qn.uri
        val ln2 = qn.localName
        return if (id == id2 || id.isEmpty && id2.isEmpty && ns1 == ns2 && ln1 == ln2)
            empty()//
        else
            notAllowed()
    }

    override fun toString(): String {
        val postfix = if (id.isEmpty) "" else "~$id"
        return "<$qName$postfix]"
    }
}
