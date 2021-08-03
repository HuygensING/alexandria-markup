package nl.knaw.huc.di.tag.validate

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

import nl.knaw.huc.di.tag.TAGAssertions.assertThat
import nl.knaw.huc.di.tag.TAGBaseStoreTest
import nl.knaw.huc.di.tag.schema.TAGMLSchemaFactory
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class TAGValidatorTest : TAGBaseStoreTest() {

    @Test
    fun testSimpleTAGMLValidationThatSucceeds() {
        val tagML = """
      [book>
      [chapter>
      [paragraph>It was the best of days, it was the worst of days....<paragraph]
      [paragraph>And then...<paragraph]
      [paragraph>Finally...<paragraph]
      <chapter]
      <book]
      """.trimIndent()
        // how to address default layer?
        val schemaYAML = """---
      |$:
      |  book:
      |    - chapter:
      |      - paragraph""".trimMargin()
        validate(tagML, schemaYAML)
    }

    @Test
    fun testSimpleTAGMLValidationThatFails() {
        val tagML = """
      [book>
      [chapter>
      [paragraph>It was the best of days, it was the worst of days....<paragraph]
      [paragraph>And then...<paragraph]
      [paragraph>Finally...<paragraph]
      <chapter]
      <book]
      """.trimIndent()
        val schemaYAML = """---
      |$:
      |  book:
      |    - chapter:
      |      - sentence""".trimMargin()
        val expectedErrors = listOf(
            "Layer $ (default): expected [sentence> as child markup of [chapter>, but found [paragraph>"
        )
        val expectedWarnings = listOf<String>()
        validateWithErrorsAndWarnings(tagML, schemaYAML, expectedErrors, expectedWarnings)
    }

    @Test
    fun testSimpleTAGMLValidation2() {
        val tagML = "[tagml>[l>test [w>word<w]<l]<tagml]"
        val schemaYAML = """$:
      |  tagml:
      |    - l:
      |      - w
      """.trimMargin()
        validate(tagML, schemaYAML)
    }

    @Test
    fun testSimpleTAGMLValidation4() {
        val tagML = "[tagml|+A,+B,+C>[l|A>[c|C>test<c] [b|B>[w|A>word<w]<b]<l]<tagml]"
        val schemaYAML = """A:
      |  tagml:
      |    - chapter:
      |        - paragraph:
      |            - sentence
      |$:
      |  tagml:
      |    - something
      |V:
      |  tagml:
      |    - poem:
      |        - verse:
      |            - line
      """.trimMargin()
        val errors = listOf("Layer A: expected [chapter|A> as child markup of [tagml|A>, but found [l|A>")
        val warnings = listOf(
            "Layers $ (default), V are defined in the schema, but not used in the document.",
            "Layers B, C are used in the document, but not defined in the schema."
        )
        validateWithErrorsAndWarnings(tagML, schemaYAML, errors, warnings)
    }

    @Test
    fun testSimpleTAGMLValidation3() {
        val tagML =
            "[tagml|+A,+B>[a|A>The rain [b|B>in [aa|A>Spain<aa] falls [bb|B>mainly<bb] on the plain.<b]<a]<tagml]"
        val schemaYAML = """A:
      |  tagml:
      |    - a:
      |      - aa
      |B:
      |  tagml:
      |    - b:
      |      - bb
      """.trimMargin()
        validate(tagML, schemaYAML)
    }

    //  @Test
    fun testMoreComplicatedTAGMLValidation() {
        val tagML = ("[root>"
                + "[s><|[del>Dit kwam van een<del]|[del>[add>Gevolg van een<add]<del]|[add>De<add]|>"
                + " te streng doorgedreven rationalisatie van zijne "
                + "<|[del>opvoeding<del]|[del>[add>prinselijke jeugd<add]<del]|[add>prinsenjeugd [?del>bracht<?del] had dit met zich meegebracht<add]|><s]"
                + "<root]")
        // how to address default layer?
        val schemaYAML = ""
        validate(tagML, schemaYAML)
    }

    private fun validate(tagML: String, schemaYAML: String) {
        LOG.info("schemaYAML={}", schemaYAML)
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assertThat(document).isNotNull

            val schemaParseResult = TAGMLSchemaFactory.parseYAML(schemaYAML)
            assertThat(schemaParseResult).hasSchema().hasNoErrors()

            val validator = TAGValidator(store)
            val validationResult = validator.validate(document, schemaParseResult.schema)
            LOG.info("validationResult={}", validationResult)
            assertThat(validationResult).isValid
        }
    }

    private fun validateWithErrorsAndWarnings(
        tagML: String,
        schemaYAML: String,
        expectedErrors: Collection<String>,
        expectedWarnings: Collection<String>
    ) {
        LOG.info("schemaYAML={}", schemaYAML)
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagML, store)
            assertThat(document).isNotNull

            val schemaParseResult = TAGMLSchemaFactory.parseYAML(schemaYAML)
            assertThat(schemaParseResult).hasSchema().hasNoErrors()

            val validator = TAGValidator(store)
            val validationResult = validator.validate(document, schemaParseResult.schema)
            LOG.info("validationResult={}", validationResult)
            assertThat(validationResult)
                .isNotValid
                .hasErrors(expectedErrors)
                .hasWarnings(expectedWarnings)
        }
    }

    //  private void validate(final TAGDocument document, final TAGMLSchema schema) {}
    private fun parseTAGML(tagML: String, store: TAGStore): TAGDocument {
        //    LOG.info("TAGML=\n{}\n", tagML);
        val trimmedTagML = tagML.trim { it <= ' ' }
        return TAGMLImporter(store).importTAGML(trimmedTagML)
    }

    companion object {
        val LOG = LoggerFactory.getLogger(TAGValidatorTest::class.java)
    }
}
