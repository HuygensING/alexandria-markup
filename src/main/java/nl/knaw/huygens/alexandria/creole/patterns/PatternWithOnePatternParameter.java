package nl.knaw.huygens.alexandria.creole.patterns;

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
import com.google.common.base.Preconditions;
import nl.knaw.huygens.alexandria.creole.Pattern;

public abstract class PatternWithOnePatternParameter extends AbstractPattern {
  final Pattern pattern;

  PatternWithOnePatternParameter(Pattern pattern) {
    Preconditions.checkNotNull(pattern);
    this.pattern = pattern;
    setHashcode(getClass().hashCode() * pattern.hashCode());
  }

  public Pattern getPattern() {
    return pattern;
  }

  @Override
  void init() {
    nullable = pattern.isNullable();
    allowsText = pattern.allowsText();
    allowsAnnotations = pattern.allowsText();
    onlyAnnotations = pattern.onlyAnnotations();
  }
}
