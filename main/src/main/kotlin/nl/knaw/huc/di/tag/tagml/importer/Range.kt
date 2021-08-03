package nl.knaw.huc.di.tag.tagml.importer

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

import com.sleepycat.persist.model.Persistent
import java.util.*

@Persistent
class Range {
    var startPosition: Position? = null
    var endPosition: Position? = null

    constructor()
    constructor(startPosition: Position?, endPosition: Position?) {
        this.startPosition = startPosition
        this.endPosition = endPosition
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Range) return false
        return startPosition == o.startPosition && endPosition == o.endPosition
    }

    override fun hashCode(): Int {
        return Objects.hash(startPosition, endPosition)
    }

    override fun toString(): String {
        return "Range{$startPosition - $endPosition}"
    }
}
