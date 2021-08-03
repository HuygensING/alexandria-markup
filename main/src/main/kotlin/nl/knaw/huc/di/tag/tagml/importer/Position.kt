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
import org.antlr.v4.runtime.ParserRuleContext
import java.util.*

@Persistent
class Position {
    var line = 0
        private set
    var character = 0
        private set

    constructor()
    constructor(line: Int, character: Int) {
        this.line = line
        this.character = character
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o !is Position) return false
        return line == o.line && character == o.character
    }

    override fun hashCode(): Int {
        return Objects.hash(line, character)
    }

    override fun toString(): String {
        return String.format("%d:%d", line, character)
    }

    companion object {
        fun startOf(ctx: ParserRuleContext): Position {
            return Position(ctx.start.line, ctx.start.charPositionInLine + 1)
        }

        fun endOf(ctx: ParserRuleContext): Position {
            return Position(
                ctx.stop.line,
                ctx.stop.charPositionInLine + ctx.stop.stopIndex - ctx.stop.startIndex + 2
            )
        }
    }
}
