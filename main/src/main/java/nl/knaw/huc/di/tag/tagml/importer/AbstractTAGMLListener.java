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
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.String.format;

public class AbstractTAGMLListener extends TAGMLParserBaseListener {
  protected ErrorListener errorListener;

  protected AbstractTAGMLListener(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  void addError(
          final ParserRuleContext ctx, final String messageTemplate, final Object... messageArgs) {
    List<Object> newArgs = new ArrayList<>(Arrays.asList(messageArgs));
    newArgs.add(0, errorPrefix(ctx));
    errorListener.addError(
            Position.startOf(ctx), Position.endOf(ctx), "%s " + messageTemplate, newArgs.toArray());
  }

  void addBreakingError(
          final ParserRuleContext ctx, final String messageTemplate, final Object... messageArgs) {
    List<Object> newArgs = new ArrayList<>(Arrays.asList(messageArgs));
    newArgs.add(0, errorPrefix(ctx));

    errorListener.addBreakingError(
            Position.startOf(ctx), Position.endOf(ctx), "%s " + messageTemplate, newArgs.toArray());
  }

  private String errorPrefix(ParserRuleContext ctx) {
    return errorPrefix(ctx, false);
  }

  String errorPrefix(ParserRuleContext ctx, boolean useStopToken) {
    Token token = useStopToken ? ctx.stop : ctx.start;
    return format("line %d:%d :", token.getLine(), token.getCharPositionInLine() + 1);
  }
}
