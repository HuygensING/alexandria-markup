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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static com.google.common.collect.Streams.zip;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class SegmentMatcher implements Predicate<Segment> {
  private final Score.Type scoreType;
  private TAGTokenMatcher[] tokenContentMatchersA;
  private TAGTokenMatcher[] tokenContentMatchersB;
  private List<String> failMessages = new ArrayList<>();

  private SegmentMatcher(Score.Type scoreType) {
    this.scoreType = scoreType;
  }

  @Override
  public boolean test(Segment segment) {
    boolean typeCheck = segment.type().equals(scoreType);
    if (!typeCheck) {
      failMessages.add(format("Expected segment.type to be '%s', but was '%s'", scoreType, segment.type()));
    }
    boolean tokenCheckA = zip(stream(tokenContentMatchersA), segment.tokensA().stream(), this::matchTest).allMatch(b -> b);
    if (!tokenCheckA) {
      failMessages.add(tokenExpectationFailure("tokensA", segment.tokensA(), tokenContentMatchersA));
    }
    boolean tokenCheckB = zip(stream(tokenContentMatchersB), segment.tokensB().stream(), this::matchTest).allMatch(b -> b);
    if (!tokenCheckB) {
      failMessages.add(tokenExpectationFailure("tokensB", segment.tokensB(), tokenContentMatchersB));
    }
    return typeCheck && tokenCheckA && tokenCheckB;
  }

  private String tokenExpectationFailure(String tokensName,//
                                         List<TAGToken> tagTokens,//
                                         TAGTokenMatcher[] tokenContentMatchers) {
    return format("Expected segment.%s to match %s, but was %s",//
        tokensName,//
        tagTokens.stream()//
            .map(t -> t.content)//
            .collect(toList()),//
        stream(tokenContentMatchers)//
            .map(TAGTokenMatcher::getExpectedContent)//
            .collect(toList())
    );
  }

  private Boolean matchTest(TAGTokenMatcher tagTokenContentMatcher, TAGToken tagToken) {
    return tagTokenContentMatcher.test(tagToken);
  }

  static SegmentMatcher sM(Score.Type scoreType) {
    return new SegmentMatcher(scoreType);
  }

  public SegmentMatcher tokensA(TAGTokenMatcher... tokenContentMatchers) {
    tokenContentMatchersA = tokenContentMatchers;
    return this;
  }

  public SegmentMatcher tokensB(TAGTokenMatcher... tokenContentMatchers) {
    tokenContentMatchersB = tokenContentMatchers;
    return this;
  }

  public List<String> getFailMessages() {
    return failMessages;
  }
}
