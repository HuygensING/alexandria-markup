package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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
import java.util.function.Predicate;

public class SegmentMatcher implements Predicate<Segment> {
  private final Score.Type scoreType;

  private SegmentMatcher(Score.Type scoreType) {
    this.scoreType = scoreType;
  }

  @Override
  public boolean test(Segment segment) {
    return segment.type().equals(scoreType);
  }

  public static SegmentMatcher sM(Score.Type scoreType) {
    return new SegmentMatcher(scoreType);
  }
}
