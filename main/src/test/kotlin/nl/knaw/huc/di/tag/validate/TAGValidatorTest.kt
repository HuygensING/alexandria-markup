package nl.knaw.huc.di.tag.validate

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

import nl.knaw.huc.di.tag.TAGAssertions.assertThat
import nl.knaw.huc.di.tag.TAGBaseStoreTest
import nl.knaw.huc.di.tag.schema.TAGMLSchemaFactory
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory

class TAGValidatorTest : TAGBaseStoreTest() {

    @Test
    fun testSimpleTAGMLValidationThatSucceeds() {
        val body = """
            [book>
            [chapter>
            [paragraph>It was the best of days, it was the worst of days....<paragraph]
            [paragraph>And then...<paragraph]
            [paragraph>Finally...<paragraph]
            <chapter]
            <book]
            """.trimIndent()
        // how to address default layer?
        val schemaYAML = """
            |---
            |$:
            |  book:
            |    - chapter:
            |      - paragraph""".trimMargin()
        validate(body, schemaYAML)
    }

    @Test
    fun testSimpleTAGMLValidationThatFails() {
        val body = """
            [book>
            [chapter>
            [paragraph>It was the best of days, it was the worst of days....<paragraph]
            [paragraph>And then...<paragraph]
            [paragraph>Finally...<paragraph]
            <chapter]
            <book]
            """.trimIndent()
        val schemaYAML = """
            |---
            |$:
            |  book:
            |    - chapter:
            |      - sentence""".trimMargin()
        val expectedErrors = listOf(
                "Layer $ (default): expected [sentence> as child markup of [chapter>, but found [paragraph>")
        val expectedWarnings = listOf<String>()
        validateWithErrorsAndWarnings(body, schemaYAML, expectedErrors, expectedWarnings)
    }

    @Test
    fun testSimpleTAGMLValidation2() {
        val body = "[tagml>[l>test [w>word<w]<l]<tagml]"
        val schemaYAML = """
            |$:
            |  tagml:
            |    - l:
            |      - w
            """.trimMargin()
        validate(body, schemaYAML)
    }

    @Test
    fun testSimpleTAGMLValidation4() {
        val body = "[tagml|+A,+B,+C>[l|A>[c|C>test<c] [b|B>[w|A>word<w]<b]<l]<tagml]"
        val schemaYAML = """
            |A:
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
                "Layers B, C are used in the document, but not defined in the schema.")
        validateWithErrorsAndWarnings(body, schemaYAML, errors, warnings)
    }

    @Test
    fun testSimpleTAGMLValidation3() {
        val body = "[tagml|+A,+B>[a|A>The rain [b|B>in [aa|A>Spain<aa] falls [bb|B>mainly<bb] on the plain.<b]<a]<tagml]"
        val schemaYAML = """
            |A:
            |  tagml:
            |    - a:
            |      - aa
            |B:
            |  tagml:
            |    - b:
            |      - bb
            """.trimMargin()
        validate(body, schemaYAML)
    }

    @Ignore
    @Test
    fun testMoreComplicatedTAGMLValidation() {
        val body = ("[root>"
                + "[s><|[del>Dit kwam van een<del]|[del>[add>Gevolg van een<add]<del]|[add>De<add]|>"
                + " te streng doorgedreven rationalisatie van zijne "
                + "<|[del>opvoeding<del]|[del>[add>prinselijke jeugd<add]<del]|[add>prinsenjeugd [?del>bracht<?del] had dit met zich meegebracht<add]|><s]"
                + "<root]")
        // how to address default layer?
        val schemaYAML = ""
        validate(body, schemaYAML)
    }

    private fun validate(tagmlBody: String, schemaYAML: String) {
        log.info("schemaYAML={}", schemaYAML)
        val tagml = addTAGMLHeader(tagmlBody)
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagml, store)
            assertThat(document).isNotNull

            val schemaParseResult = TAGMLSchemaFactory.parseYAML(schemaYAML)
            assertThat(schemaParseResult).hasSchema().hasNoErrors()

            val validator = TAGValidator(store)
            val validationResult = validator.validate(document, schemaParseResult.schema)
            log.info("validationResult={}", validationResult)
            assertThat(validationResult).isValid
        }
    }

    private fun validateWithErrorsAndWarnings(
            tagmlBody: String,
            schemaYAML: String,
            expectedErrors: Collection<String>,
            expectedWarnings: Collection<String>) {
        log.info("schemaYAML={}", schemaYAML)
        val tagml = addTAGMLHeader(tagmlBody)
        runInStoreTransaction { store: TAGStore ->
            val document = parseTAGML(tagml, store)
            assertThat(document).isNotNull

            val schemaParseResult = TAGMLSchemaFactory.parseYAML(schemaYAML)
            assertThat(schemaParseResult).hasSchema().hasNoErrors()

            val validator = TAGValidator(store)
            val validationResult = validator.validate(document, schemaParseResult.schema)
            log.info("validationResult={}", validationResult)
            assertThat(validationResult)
                    .isNotValid
                    .hasErrors(expectedErrors)
                    .hasWarnings(expectedWarnings)
        }
    }

    //  private void validate(final TAGDocument document, final TAGMLSchema schema) {}
    private fun parseTAGML(tagml: String, store: TAGStore): TAGDocument {
        //    LOG.info("TAGML=\n{}\n", tagML);
        val trimmedTagML = tagml.trim { it <= ' ' }
        return TAGMLImporter(store).importTAGML(trimmedTagML)
    }

    companion object {
        val log = LoggerFactory.getLogger(TAGValidatorTest::class.java)
    }
}
