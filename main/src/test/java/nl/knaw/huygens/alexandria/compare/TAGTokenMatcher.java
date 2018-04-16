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

import java.util.function.Predicate;

public class TAGTokenMatcher implements Predicate<TAGToken> {
  private final Class<? extends TAGToken> expectedClass;
  private final String expectedContent;

  private TAGTokenMatcher(Class<? extends TAGToken> expectedClass, String expectedContent) {
    this.expectedClass = expectedClass;
    this.expectedContent = expectedContent;
  }

  @Override
  public boolean test(TAGToken tagToken) {
    return
        tagToken.getClass().equals(expectedClass) //
            && tagToken.content.equals(expectedContent);
  }

  public static TAGTokenMatcher text(String expectedContent) {
    return new TAGTokenMatcher(TextToken.class, expectedContent);
  }

  public static TAGTokenMatcher markupOpen(String expectedContent) {
    return new TAGTokenMatcher(MarkupOpenToken.class, expectedContent);
  }

  public static TAGTokenMatcher markupClose(String expectedContent) {
    return new TAGTokenMatcher(MarkupCloseToken.class, expectedContent);
  }

  public String getExpectedContent() {
    return expectedContent;
  }
}
