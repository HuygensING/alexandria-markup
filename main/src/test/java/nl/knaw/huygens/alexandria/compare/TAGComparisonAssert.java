package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * main
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import java.util.List;

public class TAGComparisonAssert extends AbstractAssert<TAGComparisonAssert, TAGComparison> {

  public TAGComparisonAssert(TAGComparison actual) {
    super(actual, TAGComparisonAssert.class);
  }

  public TAGComparisonAssert hasFoundNoDifference() {
    List<String> diffLines = actual.getDiffLines();
    if (!diffLines.isEmpty()) {
      failWithMessage("Expected there to be no differences, but found diff lines: %n%s",//
          String.join("\n", diffLines));
    }
    return this;

  }
}
