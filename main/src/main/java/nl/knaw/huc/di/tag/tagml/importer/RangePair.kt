package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import com.sleepycat.persist.model.Persistent;

@Persistent
public class RangePair {
  private Range startRange;
  private Range endRange;

  public RangePair() {
  }

  public RangePair(Range startRange, Range endRange) {
    this.startRange = startRange;
    this.endRange = endRange;
  }

  public Range getStartRange() {
    return startRange;
  }

  public void setStartRange(Range startRange) {
    this.startRange = startRange;
  }

  public Range getEndRange() {
    return endRange;
  }

  public void setEndRange(Range endRange) {
    this.endRange = endRange;
  }

  @Override
  public String toString() {
    return "RangePair{" +
        "startRange=" + startRange +
        ", endRange=" + endRange +
        '}';
  }
}
