package nl.knaw.huygens.alexandria.creole;

/*
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

import nl.knaw.huygens.alexandria.creole.patterns.NotAllowed;
import nl.knaw.huygens.alexandria.creole.patterns.Text;

public class CreoleTest {
//  SoftAssertions softly = new SoftAssertions();

  public static class TestPattern implements Pattern {
    @Override
    public boolean isNullable() {
      return false;
    }

    @Override
    public boolean allowsText() {
      return false;
    }

    @Override
    public boolean allowsAnnotations() {
      return false;
    }

    @Override
    public boolean onlyAnnotations() {
      return false;
    }
  }

  static final Pattern NULLABLE_PATTERN = new NullablePattern();

  static class NullablePattern extends Text {
  }

  static final Pattern NOT_NULLABLE_PATTERN = new NotNullablePattern();

  static class NotNullablePattern extends NotAllowed {
  }


}
