package nl.knaw.huygens.alexandria.view

import java.util.*

/*
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

class TAGViewDefinition {
    var includeLayers: Set<String> = HashSet()
        private set

    var excludeLayers: Set<String> = HashSet()
        private set

    var includeMarkup: Set<String> = HashSet()
        private set

    var excludeMarkup: Set<String> = HashSet()
        private set

    var markupWithLayerExclusiveText: Set<String> = HashSet()
        private set

    fun withIncludeLayers(includeLayers: Set<String>): TAGViewDefinition {
        this.includeLayers = includeLayers
        return this
    }

    fun withExcludeLayers(excludeLayers: Set<String>): TAGViewDefinition {
        this.excludeLayers = excludeLayers
        return this
    }

    fun withIncludeMarkup(includeMarkup: Set<String>): TAGViewDefinition {
        this.includeMarkup = includeMarkup
        return this
    }

    fun withExcludeMarkup(excludeMarkup: Set<String>): TAGViewDefinition {
        this.excludeMarkup = excludeMarkup
        return this
    }

    fun withMarkupWithLayerExclusiveText(
            markupWithLayerExclusiveText: Set<String>): TAGViewDefinition {
        this.markupWithLayerExclusiveText = markupWithLayerExclusiveText
        return this
    }
}
