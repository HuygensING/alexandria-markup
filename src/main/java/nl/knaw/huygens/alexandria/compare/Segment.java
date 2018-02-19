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
import java.util.List;

class Segment {
  private final List<TAGToken> segmentTokensA;
  private final List<TAGToken> segmentTokensB;
  private final Score.Type type;

  public Segment(List<TAGToken> segmentTokensA, List<TAGToken> segmentTokensB, Score.Type type) {
    this.segmentTokensA = segmentTokensA;
    this.segmentTokensB = segmentTokensB;
    this.type = type;
  }

  public Score.Type type() {
    return type;
  }

  public List<TAGToken> tokensA() {
    return segmentTokensA;
  }

  public List<TAGToken> tokensB() {
    return segmentTokensB;
  }
}
