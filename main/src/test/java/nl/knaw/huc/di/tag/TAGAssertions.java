package nl.knaw.huc.di.tag;

/*-
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

import org.assertj.core.api.Assertions;
import org.assertj.core.util.CheckReturnValue;

import nl.knaw.huc.di.tag.schema.TAGMLSchemaParseResult;
import nl.knaw.huc.di.tag.schema.TAGMLSchemaParseResultAssert;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huc.di.tag.validate.TAGValidationResult;
import nl.knaw.huc.di.tag.validate.TAGValidationResultAssert;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.AnnotationInfoAssert;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentAssert;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupAssert;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeAssert;

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

  /**
   * Creates a new instance of <code>{@link TAGValidationResultAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static TAGValidationResultAssert assertThat(TAGValidationResult actual) {
    return new TAGValidationResultAssert(actual);
  }

  /**
   * Creates a new instance of <code>{@link TAGMLSchemaParseResultAssert}</code>.
   *
   * @param actual the actual value.
   * @return the created assertion object.
   */
  @CheckReturnValue
  public static TAGMLSchemaParseResultAssert assertThat(TAGMLSchemaParseResult actual) {
    return new TAGMLSchemaParseResultAssert(actual);
  }
}
