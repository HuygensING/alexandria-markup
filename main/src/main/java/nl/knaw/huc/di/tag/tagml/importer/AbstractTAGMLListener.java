package nl.knaw.huc.di.tag.tagml.importer;

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

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParserBaseListener;
import nl.knaw.huygens.alexandria.ErrorListener;
import org.antlr.v4.runtime.ParserRuleContext;

public class AbstractTAGMLListener extends TAGMLParserBaseListener {
  protected ErrorListener errorListener;

  protected AbstractTAGMLListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  void addError(
          final ParserRuleContext ctx, final String messageTemplate, final Object... messageArgs) {
    errorListener.addError(
            Position.startOf(ctx), Position.endOf(ctx), messageTemplate, messageArgs);
  }

  void addBreakingError(
          final ParserRuleContext ctx, final String messageTemplate, final Object... messageArgs) {
    errorListener.addBreakingError(
            Position.startOf(ctx), Position.endOf(ctx), messageTemplate, messageArgs);
  }
}
