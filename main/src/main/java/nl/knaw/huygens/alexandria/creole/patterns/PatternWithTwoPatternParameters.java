package nl.knaw.huygens.alexandria.creole.patterns;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import com.google.common.base.Preconditions;
import nl.knaw.huygens.alexandria.creole.Pattern;

public abstract class PatternWithTwoPatternParameters extends AbstractPattern {
  final Pattern pattern1;
  final Pattern pattern2;

  PatternWithTwoPatternParameters(Pattern pattern1, Pattern pattern2) {
    Preconditions.checkNotNull(pattern1);
    Preconditions.checkNotNull(pattern2);
    this.pattern1 = pattern1;
    this.pattern2 = pattern2;
    setHashcode(getClass().hashCode() + pattern1.hashCode() * pattern2.hashCode());
  }

  public Pattern getPattern1() {
    return pattern1;
  }

  public Pattern getPattern2() {
    return pattern2;
  }

  @Override
  public boolean equals(Object obj) {
    return obj.getClass().equals(this.getClass())
        && pattern1.equals(((PatternWithTwoPatternParameters) obj).getPattern1())
        && pattern2.equals(((PatternWithTwoPatternParameters) obj).getPattern2());
  }
}
