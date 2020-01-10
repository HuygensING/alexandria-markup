package nl.knaw.huc.di.tag;

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

import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.*;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.CheckReturnValue;

public class TAGAssertions extends Assertions {

  /**
   * Creates a new instance of <code>{@link TAGDocumentAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static TAGDocumentAssert assertThat(TAGDocument actual) {
    return new TAGDocumentAssert(actual);
  }

  /**
   * Creates a new instance of <code>{@link TAGMarkupAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue

  public static TAGMarkupAssert assertThat(TAGMarkup actual) {
    return new TAGMarkupAssert(actual);
  }

  /**
   * Creates a new instance of <code>{@link AnnotationInfoAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static AnnotationInfoAssert assertThat(AnnotationInfo actual) {
    return new AnnotationInfoAssert(actual);
  }

  /**
   * Creates a new instance of <code>{@link TAGTextNodeAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static TAGTextNodeAssert assertThat(TAGTextNode actual) {
    return new TAGTextNodeAssert(actual);
  }

}
