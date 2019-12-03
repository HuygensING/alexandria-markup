package nl.knaw.huc.di.tag.validate;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.schema.TAGMLSchemaFactory;
import nl.knaw.huc.di.tag.schema.TAGMLSchemaParseResult;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGValidatorTest extends TAGBaseStoreTest {
  static final Logger LOG = LoggerFactory.getLogger(TAGValidatorTest.class);

  @Test
  public void testSimpleTAGMLValidationThatSucceeds() {
    String tagML =
        "[book>\n"
            + "[chapter>\n"
            + "[paragraph>It was the best of days, it was the worst of days....<paragraph]\n"
            + "[paragraph>And then...<paragraph]\n"
            + "[paragraph>Finally...<paragraph]\n"
            + "<chapter]\n"
            + "<book]";
    // how to address default layer?
    String schemaYAML = "---\n" + "$:\n" + "  book:\n" + "    - chapter:\n" + "      - paragraph";
    validate(tagML, schemaYAML);
  }

  @Test
  public void testSimpleTAGMLValidationThatFails() {
    String tagML =
        "[book>\n"
            + "[chapter>\n"
            + "[paragraph>It was the best of days, it was the worst of days....<paragraph]\n"
            + "[paragraph>And then...<paragraph]\n"
            + "[paragraph>Finally...<paragraph]\n"
            + "<chapter]\n"
            + "<book]";
    // how to address default layer?
    String schemaYAML = "---\n" + "$:\n" + "  book:\n" + "    - chapter:\n" + "      - sentence";
    List<String> expectedErrors =
        newArrayList(
            "Layer $ (default): expected [sentence> as child markup of [chapter>, but found [paragraph>");
    List<String> expectedWarnings = newArrayList();
    validateWithErrorsAndWarnings(tagML, schemaYAML, expectedErrors, expectedWarnings);
  }

  @Test
  public void testSimpleTAGMLValidation2() {
    String tagML = "[tagml>[l>test [w>word<w]<l]<tagml]";
    // how to address default layer?
    String schemaYAML = "$:\n" + "  tagml:\n" + "    - l:\n" + "      - w\n";
    validate(tagML, schemaYAML);
  }

  @Test
  public void testSimpleTAGMLValidation4() {
    String tagML = "[tagml|+A,+B,+C>[l|A>[c|C>test<c] [b|B>[w|A>word<w]<b]<l]<tagml]";
    // how to address default layer?
    String schemaYAML =
        "A:\n"
            + "  tagml:\n"
            + "    - chapter:\n"
            + "        - paragraph:\n"
            + "            - sentence\n"
            + "$:\n"
            + "  tagml:\n"
            + "    - something\n"
            + "V:\n"
            + "  tagml:\n"
            + "    - poem:\n"
            + "        - verse:\n"
            + "            - line\n";
    final Collection<String> errors =
        newArrayList("Layer A: expected [chapter|A> as child markup of [tagml|A>, but found [l|A>");
    final Collection<String> warnings =
        newArrayList(
            "Layers $ (default), V are defined in the schema, but not used in the document.",
            "Layers B, C are used in the document, but not defined in the schema.");
    validateWithErrorsAndWarnings(tagML, schemaYAML, errors, warnings);
  }

  @Test
  public void testSimpleTAGMLValidation3() {
    String tagML =
        "[tagml|+A,+B>[a|A>The rain [b|B>in [aa|A>Spain<aa] falls [bb|B>mainly<bb] on the plain.<b]<a]<tagml]";
    String schemaYAML =
        "A:\n"
            + "  tagml:\n"
            + "    - a:\n"
            + "      - aa\n"
            + "B:\n"
            + "  tagml:\n"
            + "    - b:\n"
            + "      - bb\n";
    validate(tagML, schemaYAML);
  }

  //  @Test
  public void testMoreComplicatedTAGMLValidation() {
    String tagML =
        "[root>"
            + "[s><|[del>Dit kwam van een<del]|[del>[add>Gevolg van een<add]<del]|[add>De<add]|>"
            + " te streng doorgedreven rationalisatie van zijne "
            + "<|[del>opvoeding<del]|[del>[add>prinselijke jeugd<add]<del]|[add>prinsenjeugd [?del>bracht<?del] had dit met zich meegebracht<add]|><s]"
            + "<root]";
    // how to address default layer?
    String schemaYAML = "";
    validate(tagML, schemaYAML);
  }

  private void validate(final String tagML, final String schemaYAML) {
    LOG.info("schemaYAML={}", schemaYAML);
    runInStoreTransaction(
        store -> {
          TAGDocument document = parseTAGML(tagML, store);
          assertThat(document).isNotNull();

          final TAGMLSchemaParseResult schemaParseResult = TAGMLSchemaFactory.parseYAML(schemaYAML);
          assertThat(schemaParseResult).hasSchema().hasNoErrors();

          TAGValidator validator = new TAGValidator(store);
          final TAGValidationResult validationResult =
              validator.validate(document, schemaParseResult.schema);
          LOG.info("validationResult={}", validationResult);
          assertThat(validationResult).isValid();
        });
  }

  private void validateWithErrorsAndWarnings(
      final String tagML,
      final String schemaYAML,
      final Collection<String> expectedErrors,
      final Collection<String> expectedWarnings) {
    LOG.info("schemaYAML={}", schemaYAML);
    runInStoreTransaction(
        store -> {
          TAGDocument document = parseTAGML(tagML, store);
          assertThat(document).isNotNull();

          final TAGMLSchemaParseResult schemaParseResult = TAGMLSchemaFactory.parseYAML(schemaYAML);
          assertThat(schemaParseResult).hasSchema().hasNoErrors();

          TAGValidator validator = new TAGValidator(store);
          final TAGValidationResult validationResult =
              validator.validate(document, schemaParseResult.schema);
          LOG.info("validationResult={}", validationResult);
          assertThat(validationResult)
              .isNotValid()
              .hasErrors(expectedErrors)
              .hasWarnings(expectedWarnings);
        });
  }

  //  private void validate(final TAGDocument document, final TAGMLSchema schema) {}

  private TAGDocument parseTAGML(final String tagML, final TAGStore store) {
    //    LOG.info("TAGML=\n{}\n", tagML);
    String trimmedTagML = tagML.trim();
    TAGDocument document = new TAGMLImporter(store).importTAGML(trimmedTagML);
    return document;
  }
}
