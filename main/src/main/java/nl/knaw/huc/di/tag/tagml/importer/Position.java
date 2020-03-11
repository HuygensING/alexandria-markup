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

import org.antlr.v4.runtime.ParserRuleContext;

import java.util.Objects;

import static java.lang.String.format;

public class Position {
  private final int line;
  private final int character;

  public Position(int line, int character) {
    this.line = line;
    this.character = character;
  }

  public static Position startOf(final ParserRuleContext ctx) {
    return new Position(ctx.start.getLine(), ctx.start.getCharPositionInLine() + 1);
  }

  public static Position endOf(final ParserRuleContext ctx) {
    return new Position(
            ctx.stop.getLine(),
            ctx.stop.getCharPositionInLine() + ctx.stop.getStopIndex() - ctx.stop.getStartIndex() + 2);
  }

  public int getLine() {
    return line;
  }

  public int getCharacter() {
    return character;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof Position)) return false;
    final Position position = (Position) o;
    return line == position.line && character == position.character;
  }

  @Override
  public int hashCode() {
    return Objects.hash(line, character);
  }

  @Override
  public String toString() {
    return format("%d:%d", line, character);
  }
}
