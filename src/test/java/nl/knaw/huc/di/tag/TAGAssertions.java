package nl.knaw.huc.di.tag;

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

import nl.knaw.huygens.alexandria.storage.wrappers.*;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.CheckReturnValue;

public class TAGAssertions extends Assertions {

  /**
   * Creates a new instance of <code>{@link nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapperAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static DocumentWrapperAssert assertThat(DocumentWrapper actual) {
    return new DocumentWrapperAssert(actual);
  }

  /**
   * Creates a new instance of <code>{@link MarkupWrapperAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue

  public static MarkupWrapperAssert assertThat(MarkupWrapper actual) {
    return new MarkupWrapperAssert(actual);
  }

  /**
   * Creates a new instance of <code>{@link nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapperAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static TextNodeWrapperAssert assertThat(TextNodeWrapper actual) {
    return new TextNodeWrapperAssert(actual);
  }

}
