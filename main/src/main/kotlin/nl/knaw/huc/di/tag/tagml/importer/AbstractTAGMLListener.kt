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

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener
import nl.knaw.huygens.alexandria.ErrorListener
import org.antlr.v4.runtime.ParserRuleContext

open class AbstractTAGMLListener(var errorListener: ErrorListener) : TAGMLParserBaseListener() {

    fun addError(
        ctx: ParserRuleContext,
        messageTemplate: String,
        vararg messageArgs: Any?
    ) =
        errorListener.addError(
            Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs
        )

    fun addBreakingError(
        ctx: ParserRuleContext,
        messageTemplate: String,
        vararg messageArgs: Any?
    ) =
        errorListener.addBreakingError(
            Position.startOf(ctx), Position.endOf(ctx), messageTemplate, *messageArgs
        )
}
