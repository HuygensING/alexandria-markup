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

import java.util.*

object Basics {

    fun qName(localName: String): QName {
        return qName("", localName)
    }

    fun qName(uri: String, localName: String): QName {
        return QName(uri(uri), localName(localName))
    }

    fun localName(localName: String): LocalName {
        return LocalName(localName)
    }

    fun uri(uri: String): Uri {
        return Uri(uri)
    }

    fun id(id: String): Id {
        return Id(id)
    }

    fun context(): Context {
        return Context(uri(""), HashMap())
    }

    class Uri internal constructor(uri: String) : StringWrapper(uri)

    class LocalName internal constructor(localName: String) : StringWrapper(localName)

    class Id internal constructor(id: String) : StringWrapper(id)

    private class Prefix(prefix: String) : StringWrapper(prefix)

    class QName(val uri: Uri, val localName: LocalName) {

        override fun toString(): String {
            val prefix = if (uri.isEmpty) "" else "$uri: "
            return prefix + localName
        }
    }

    /*
   A Context represents the context of an XML element.
   It consists of a base URI and a mapping from prefixes to namespace URIs.
   */
    class Context internal constructor(val uri: Uri, private val nameSpaceURI4Prefix: Map<Prefix, Uri>) {

        fun getNameSpaceURIForPrefix(prefix: Prefix): Uri {
            return nameSpaceURI4Prefix[prefix]
        }
    }

    private open class StringWrapper internal constructor(val value: String) {
        internal val hashCode: Int

        val isEmpty: Boolean
            get() = value.isEmpty()

        init {
            val baseHashCode = javaClass.hashCode()
            val valueHashCode = value.hashCode()
            hashCode = if (valueHashCode == 0) baseHashCode else baseHashCode * valueHashCode
        }

        override fun hashCode(): Int {
            return hashCode
        }

        override fun equals(obj: Any?): Boolean {
            return (obj!!.javaClass == this.javaClass//
                    && value == (obj as StringWrapper).value)
        }

        override fun toString(): String {
            return value
        }
    }

}
