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
public class TypeScorer implements Scorer {
  @Override
  public boolean match(TAGToken tokenA, TAGToken tokenB) {
    boolean punctuationType = (tokenA.content.matches("\\W+") && tokenB.content.matches("\\W+"));
    boolean contentType = (tokenA.content.matches("\\w+") && tokenB.content.matches("\\w+"));
//        System.out.println(punctuationType + " " + contentType);
    if (tokenA instanceof MarkupToken && tokenB instanceof MarkupToken) {
      return true;
    }
    return tokenA instanceof TextToken && tokenB instanceof TextToken && (punctuationType || contentType);
  }
}
