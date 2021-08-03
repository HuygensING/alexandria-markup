package nl.knaw.huc.di.tag

import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.view.TAGView

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

object TAGViews {
    @JvmStatic
    fun getShowAllMarkupView(store: TAGStore): TAGView =
        TAGView(store).apply {
            layersToExclude = emptySet()
            markupToExclude = emptySet()
        }

    @JvmStatic
    fun getShowNoMarkupView(store: TAGStore): TAGView =
        TAGView(store).apply {
            layersToInclude = emptySet()
            markupToInclude = emptySet()
        }
}
