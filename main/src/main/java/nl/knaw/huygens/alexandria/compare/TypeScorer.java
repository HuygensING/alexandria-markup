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

  private static final String REGEX_NON_WORD_CHARACTERS = "\\W+";
  private static final String REGEX_WORD_CHARACTERS = "\\w+";

  @Override
  public boolean match(TAGToken tokenA, TAGToken tokenB) {
    if (tokenA instanceof MarkupOpenToken && tokenB instanceof MarkupOpenToken//
        || tokenA instanceof MarkupCloseToken && tokenB instanceof MarkupCloseToken) {
      return true;
    }
    boolean bothContainWordCharacters = (containsOnlyWordCharacters(tokenA)//
        && containsOnlyWordCharacters(tokenB));
    boolean bothContainPunctuation = (containsOnlyNonWordCharacters(tokenA)//
        && containsOnlyNonWordCharacters(tokenB));
    return tokenA instanceof TextToken//
        && tokenB instanceof TextToken//
        && (bothContainPunctuation || bothContainWordCharacters);
  }

  private boolean containsOnlyWordCharacters(TAGToken tokenA) {
    return tokenA.content.matches(REGEX_WORD_CHARACTERS);
  }

  private boolean containsOnlyNonWordCharacters(TAGToken tokenA) {
    return tokenA.content.matches(REGEX_NON_WORD_CHARACTERS);
  }
}
