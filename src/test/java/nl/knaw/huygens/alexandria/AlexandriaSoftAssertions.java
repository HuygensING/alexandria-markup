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
import nl.knaw.huygens.alexandria.compare.TAGComparator;
import nl.knaw.huygens.alexandria.compare.TAGComparatorAssert;
import org.assertj.core.api.SoftAssertions;

public class AlexandriaSoftAssertions extends SoftAssertions {

  public TAGComparatorAssert assertThat(TAGComparator actual) {
    return proxy(TAGComparatorAssert.class, TAGComparator.class, actual);
  }

  public SegmentAssert assertThat(Segment actual) {
    return proxy(SegmentAssert.class, Segment.class, actual);
  }

}
