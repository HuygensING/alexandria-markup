package nl.knaw.huygens.alexandria.compare;

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
interface Scorer {
  boolean match(TAGToken tokenA, TAGToken tokenB);

  // the method gap returns a new object ScoreIterator (that is created in the EditGraphAligner)
  // and has the fields type, x, y, parent, i
  default Score gap(int x, int y, Score parent) {
    return new Score(Boolean.FALSE, x, y, parent, parent.globalScore - 1);
  }

  default Score score(int x, int y, Score parent, boolean match) {
    if (match) {
      return new Score(Boolean.TRUE, x, y, parent, parent.globalScore);
    }
    // "replacement" means replacement (omission + addition)
    return new Score(Boolean.FALSE, x, y, parent, parent.globalScore - 2);
  }

}
