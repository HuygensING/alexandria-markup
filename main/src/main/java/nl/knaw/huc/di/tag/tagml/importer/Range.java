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

import java.util.Objects;

public class Range {

  private final Position startPosition;
  private final Position endPosition;

  public Range(Position startPosition, Position endPosition) {
    this.startPosition = startPosition;
    this.endPosition = endPosition;
  }

  public Position getEndPosition() {
    return endPosition;
  }

  public Position getStartPosition() {
    return startPosition;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (!(o instanceof Range)) return false;
    final Range range = (Range) o;
    return startPosition.equals(range.startPosition) && endPosition.equals(range.endPosition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(startPosition, endPosition);
  }

  @Override
  public String toString() {
    return "Range{" + startPosition + " -" + endPosition + '}';
  }
}
