package nl.knaw.huygens.alexandria.creole;

    /*
     * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import java.lang.reflect.Constructor;

public interface Pattern {
  default Pattern flip() {
    if (!(this instanceof Patterns.PatternWithTwoPatternParameters)) {
      return this;
    }
    Patterns.PatternWithTwoPatternParameters p0 = (Patterns.PatternWithTwoPatternParameters) this;
    Pattern p1 = p0.getPattern1();
    Pattern p2 = p0.getPattern2();
    try {
      Constructor<? extends Pattern> constructor = getClass()//
          .getConstructor(new Class[]{Pattern.class, Pattern.class});
      return constructor.newInstance(p2, p1);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  boolean isNullable();
}
