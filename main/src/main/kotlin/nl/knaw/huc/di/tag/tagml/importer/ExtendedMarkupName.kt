package nl.knaw.huc.di.tag.tagml.importer

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.TAGML.OPTIONAL_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.RESUME_PREFIX
import nl.knaw.huc.di.tag.tagml.TAGML.SUSPEND_PREFIX
import java.util.regex.Pattern

class ExtendedMarkupName {
    var tagName: String? = null
    var isOptional = false
    var isResume = false
    var isSuspend = false
    var id: String? = null

    val extendedMarkupName: String
        get() = (if (id != null) "$tagName=$id" else tagName)!!

    companion object {
        fun of(text1: String): ExtendedMarkupName {
            var text = text1
            val extendedMarkupName = ExtendedMarkupName()
            if (text.startsWith(OPTIONAL_PREFIX)) {
                text = text.replaceFirst(Pattern.quote(OPTIONAL_PREFIX).toRegex(), "")
                extendedMarkupName.isOptional = true
            }
            if (text.startsWith(SUSPEND_PREFIX)) {
                text = text.replaceFirst(Pattern.quote(SUSPEND_PREFIX).toRegex(), "")
                extendedMarkupName.isSuspend = true
            }
            if (text.startsWith(RESUME_PREFIX)) {
                text = text.replaceFirst(Pattern.quote(RESUME_PREFIX).toRegex(), "")
                extendedMarkupName.isResume = true
            }
            if (text.contains("~")) {
                val parts = text.split("~".toRegex()).toTypedArray()
                text = parts[0]
                val id = parts[1]
                extendedMarkupName.id = id
            }
            extendedMarkupName.tagName = text
            return extendedMarkupName
        }
    }
}
