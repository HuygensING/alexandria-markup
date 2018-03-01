package nl.knaw.huygens.alexandria;

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

import nl.knaw.huygens.alexandria.compare.Segment;
import nl.knaw.huygens.alexandria.compare.SegmentAssert;
import nl.knaw.huygens.alexandria.compare.TAGComparison;
import nl.knaw.huygens.alexandria.compare.TAGComparisonAssert;
import org.assertj.core.api.AbstractStandardSoftAssertions;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

public class AlexandriaSoftAssertions extends AbstractStandardSoftAssertions implements TestRule {

  public TAGComparisonAssert assertThat(TAGComparison actual) {
    return proxy(TAGComparisonAssert.class, TAGComparison.class, actual);
  }

  public SegmentAssert assertThat(Segment actual) {
    return proxy(SegmentAssert.class, Segment.class, actual);
  }

  public Statement apply(final Statement base, Description description) {
    return new Statement() {
      public void evaluate() throws Throwable {
        base.evaluate();
        MultipleFailureException.assertEmpty(AlexandriaSoftAssertions.this.errorsCollected());
      }
    };
  }

}
