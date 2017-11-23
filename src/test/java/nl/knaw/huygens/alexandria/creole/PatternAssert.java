package nl.knaw.huygens.alexandria.creole;

/*-
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
import org.assertj.core.api.AbstractAssert;

public class PatternAssert extends AbstractAssert<PatternAssert, Pattern> {

  public PatternAssert(Pattern actual) {
    super(actual, PatternAssert.class);
  }

  public boolean isNullable() {
    return Utilities.nullable(actual);
  }

  public boolean isNotNullable() {
    return !Utilities.nullable(actual);
  }

  public boolean allowsText() {
    return Utilities.allowsText(actual);
  }

  public boolean doesNotAllowText() {
    return !Utilities.allowsText(actual);
  }
}
