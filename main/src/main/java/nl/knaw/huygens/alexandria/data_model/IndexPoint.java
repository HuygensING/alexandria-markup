package nl.knaw.huygens.alexandria.data_model;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

/** Created by bramb on 3-3-2017. */
public class IndexPoint {
  private final int textNodeIndex;
  private final int markupIndex;

  public IndexPoint(int textNodeIndex, int markupIndex) {
    this.textNodeIndex = textNodeIndex;
    this.markupIndex = markupIndex;
  }

  public int getTextNodeIndex() {
    return textNodeIndex;
  }

  public int getMarkupIndex() {
    return markupIndex;
  }

  @Override
  public String toString() {
    return "(" + textNodeIndex + "," + markupIndex + ")";
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof IndexPoint) {
      IndexPoint other = (IndexPoint) obj;
      return (other.markupIndex == markupIndex) && (other.textNodeIndex == textNodeIndex);
    }
    return false;
  }
}
