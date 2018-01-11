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
import nl.knaw.huygens.alexandria.creole.Basics;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class All extends PatternWithTwoPatternParameters {
  public All(Pattern pattern1, Pattern pattern2) {
    super(pattern1, pattern2);
  }

  @Override
  void init() {
    nullable = pattern1.isNullable() && pattern2.isNullable();
    allowsText = pattern1.isNullable()//
        ? (pattern1.allowsText() || pattern2.allowsText())//
        : pattern1.allowsText();
    allowsAnnotations = pattern1.allowsAnnotations() && pattern2.allowsAnnotations();
    onlyAnnotations = pattern1.onlyAnnotations() || pattern2.onlyAnnotations();
  }
}
