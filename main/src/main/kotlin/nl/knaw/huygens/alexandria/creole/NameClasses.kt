package nl.knaw.huygens.alexandria.creole

/*
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

object NameClasses {

    val ANY_NAME = AnyName()

    /*
  A NameClass represents a name class.

  data NameClass = AnyName
                   | AnyNameExcept NameClass
                   | Name Uri LocalName
                   | NsName Uri
                   | NsNameExcept Uri NameClass
                   | NameClassChoice NameClass NameClass
   */

    fun anyName(): AnyName {
        return ANY_NAME
    }

    class AnyName : AbstractNameClass() {
        override fun contains(qName: Basics.QName): Boolean {
            return true
        }
    }

    fun anyNameExcept(nameClassToExcept: NameClass): AnyNameExcept {
        return AnyNameExcept(nameClassToExcept)
    }

    class AnyNameExcept(private val nameClassToExcept: NameClass) : AbstractNameClass() {

        override fun contains(qName: Basics.QName): Boolean {
            return !nameClassToExcept.contains(qName)
        }
    }

    fun name(localName: String): Name {
        return name("", localName)
    }

    private fun name(uri: String, localName: String): Name {
        return name(Basics.uri(uri), Basics.localName(localName))
    }

    private fun name(uri: Basics.Uri, localName: Basics.LocalName): Name {
        return Name(uri, localName)
    }

    class Name(val uri: Basics.Uri, val localName: Basics.LocalName) : AbstractNameClass() {

        init {
            Companion.setHashCode(this, javaClass.hashCode() * uri.hashCode() * localName.hashCode())
        }

        override fun contains(qName: Basics.QName): Boolean {
            return qName.uri == uri && qName.localName == localName
        }

        override fun toString(): String {
            return localName.value
        }
    }

    fun nsNameExcept(uri: String, nameClass: NameClass): NsNameExcept {
        return nsNameExcept(Basics.uri(uri), nameClass)
    }

    private fun nsNameExcept(uri: Basics.Uri, nameClass: NameClass): NsNameExcept {
        return NsNameExcept(uri, nameClass)
    }

    class NsNameExcept(val uri: Basics.Uri, val nameClass: NameClass) : AbstractNameClass() {

        override fun contains(qName: Basics.QName): Boolean {
            return (uri == qName.uri && !nameClass.contains(qName))
        }
    }

    fun nsName(uri: String): NsName {
        return nsName(Basics.uri(uri))
    }

    private fun nsName(uri: Basics.Uri): NsName {
        return NsName(uri)
    }

    class NsName(private val uri: Basics.Uri) : AbstractNameClass() {

        val value: String
            get() = uri.value

        override fun contains(qName: Basics.QName): Boolean {
            return value == qName.uri.value
        }
    }

    fun nameClassChoice(nameClass1: NameClass, nameClass2: NameClass): NameClassChoice {
        return NameClassChoice(nameClass1, nameClass2)
    }

    class NameClassChoice(val nameClass1: NameClass, val nameClass2: NameClass) : AbstractNameClass() {

        override fun contains(qName: Basics.QName): Boolean {
            return (nameClass1.contains(qName)
                    || nameClass2.contains(qName))
        }
    }

    /* abstract classes */
    abstract class AbstractNameClass : NameClass {
        var hashCode = 0

        init {
            hashCode = javaClass.hashCode()
        }

        override fun hashCode(): Int {
            return hashCode
        }

        companion object {
            fun setHashCode(abstractNameClass: AbstractNameClass, hashCode: Int) {
                abstractNameClass.hashCode = hashCode
            }
        }
    }

}
