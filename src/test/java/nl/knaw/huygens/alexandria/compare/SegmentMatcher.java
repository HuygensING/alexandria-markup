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

import static com.google.common.collect.Streams.zip;
import static java.util.Arrays.stream;

public class SegmentMatcher implements Predicate<Segment> {
  private final Score.Type scoreType;
  private TAGTokenContentMatcher[] tokenContentMatchersA;
  private TAGTokenContentMatcher[] tokenContentMatchersB;

  private SegmentMatcher(Score.Type scoreType) {
    this.scoreType = scoreType;
  }

  @Override
  public boolean test(Segment segment) {
    boolean typeCheck = segment.type().equals(scoreType);
    boolean tokenCheckA = zip(stream(tokenContentMatchersA), segment.tokensA().stream(), this::matchTest).allMatch(b -> b);
    boolean tokenCheckB = zip(stream(tokenContentMatchersB), segment.tokensB().stream(), this::matchTest).allMatch(b -> b);
    return typeCheck && tokenCheckA && tokenCheckB;
  }

  private Boolean matchTest(TAGTokenContentMatcher tagTokenContentMatcher, TAGToken tagToken) {
    return tagTokenContentMatcher.test(tagToken);
  }

  static SegmentMatcher sM(Score.Type scoreType) {
    return new SegmentMatcher(scoreType);
  }

  public SegmentMatcher tokensA(TAGTokenContentMatcher... tokenContentMatchers) {
    tokenContentMatchersA = tokenContentMatchers;
    return this;
  }

  public SegmentMatcher tokensB(TAGTokenContentMatcher... tokenContentMatchers) {
    tokenContentMatchersB = tokenContentMatchers;
    return this;
  }
}
