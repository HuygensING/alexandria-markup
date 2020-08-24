package nl.knaw.huc.di.tag.schema

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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.github.fge.jsonschema.core.exceptions.ProcessingException
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.JsonSchemaFactory
import nl.knaw.huc.di.tag.TAGAssertions.assertThat
import nl.knaw.huc.di.tag.schema.TAGMLSchemaFactory.parseYAML
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.IOException

class TAGMLSchemaTest {
  private val LOG = LoggerFactory.getLogger(TAGMLSchemaTest::class.java)

  @Test
  fun testCorrectSchema() {
    val schemaYAML = """---
        |L1:
        |   root:
        |     - a
        |     - b
        |     - c
        |     - d:
        |         - d1
        |         - d2
        |L2:
        |  root:
        |    - x
        |    - 'y'
        |    - z:
        |        - z1
        |        - z2
        """.trimMargin()
    println(schemaYAML)
    val result = parseYAML(schemaYAML)
    assertThat(result).hasNoErrors().hasLayers("L1", "L2")

    val layerHierarchy1 = result.schema.getLayerHierarchy("L1")
    assertThat(layerHierarchy1.data).isEqualTo("root")

    val rootChildren: List<TreeNode<String>> = layerHierarchy1.children
    assertThat(rootChildren).hasSize(4)
    assertThat(rootChildren[0].data).isEqualTo("a")
    assertThat(rootChildren[1].data).isEqualTo("b")
    assertThat(rootChildren[2].data).isEqualTo("c")
    assertThat(rootChildren[3].data).isEqualTo("d")

    val dChildren: List<TreeNode<String>> = rootChildren[3].children
    assertThat(dChildren).hasSize(2)
    assertThat(dChildren[0].data).isEqualTo("d1")
    assertThat(dChildren[1].data).isEqualTo("d2")

    val layerHierarchy2 = result.schema.getLayerHierarchy("L2")
    assertThat(layerHierarchy2.data).isEqualTo("root")

    val rootChildren2: List<TreeNode<String>> = layerHierarchy2.children
    assertThat(rootChildren2).hasSize(3)
    assertThat(rootChildren2[0].data).isEqualTo("x")
    assertThat(rootChildren2[1].data).isEqualTo("y")
    assertThat(rootChildren2[2].data).isEqualTo("z")

    val zChildren: List<TreeNode<String>> = rootChildren2[2].children
    assertThat(zChildren).hasSize(2)
    assertThat(zChildren[0].data).isEqualTo("z1")
    assertThat(zChildren[1].data).isEqualTo("z2")
  }

  @Test
  @Throws(IOException::class, ProcessingException::class)
  fun testInCorrectSchemaWithMultipleRoots() {
    val schemaYAML = """---
        |L1:
        |  root1:
        |     - a
        |     - b
        |     - c
        |     - d:
        |         - d1
        |         - d2
        |  root2:
        |    - k
        |    - l
        |    - m:
        |        - m1
        |        - m2
        """.trimMargin()
    println(schemaYAML)
    val report = validateSchema(schemaYAML)
    assertThat(report.isSuccess).isFalse()

    val result = parseYAML(schemaYAML)
    LOG.info("errors={}", result.errors)
    assertThat(result).hasErrors("only 1 root markup allowed; found 2 [root1, root2] in layer L1")
  }

  @Test
  @Throws(IOException::class, ProcessingException::class)
  fun testInCorrectYaml() {
    val schemaYAML = "i am not yaml"
    val report = validateSchema(schemaYAML)
    assertThat(report.isSuccess).isFalse()

    val result = parseYAML(schemaYAML)
    LOG.info("errors={}", result.errors)
    assertThat(result).hasErrors("no layer definitions found")
  }

  @Test
  @Throws(IOException::class, ProcessingException::class)
  fun testInCorrectYaml2() {
    val schemaYAML = """L1:
        |  - boolean: true
        |  - integer: 3
        |  - float: 3.14
        |  - string: "something"""".trimMargin()
    val report = validateSchema(schemaYAML)
    assertThat(report.isSuccess).isFalse()

    val result = parseYAML(schemaYAML)
    LOG.info("errors={}", result.errors)
    assertThat(result)
        .hasErrors(
            "expected root markup with list of child markup, found (as json) [{\"boolean\":true},{\"integer\":3},{\"float\":3.14},{\"string\":\"something\"}]")
  }

    @Disabled
    @Test
  @Throws(IOException::class, ProcessingException::class)
  fun testInCorrectSchema() {
    val schemaYAML = """
        |yaml: no
        |x
        """.trimMargin()
    val report = validateSchema(schemaYAML)
    assertThat(report.isSuccess).isFalse()

    val result = parseYAML(schemaYAML)
    LOG.info("errors={}", result.errors)
    assertThat(result)
        .hasErrors(
            """while scanning a simple key
            | in 'reader', line 2, column 1:
            |    x
            |    ^
            |could not find expected ':'
            | in 'reader', line 2, column 2:
            |    x
            |     ^
            |
            | at [Source: (StringReader); line: 1, column: 9]""".trimMargin())
  }

    @Disabled("fix tagschemaschema.json first: find 'or' function")
    @Test
  @Throws(ProcessingException::class, IOException::class)
  fun testSchemaValidator() {
    // because y = yes = TRUE according to the jackson parser
    val yaml = """---
        |L1:
        |   root:
        |     - a
        |     - b
        |     - c
        |     - d:
        |         - d1
        |         - d2
        |L2:
        |  root:
        |    - x
        |    - 'y'
        |    - z:
        |        - z1
        |        - z2
        """.trimMargin()
    val report = validateSchema(yaml)
    assertThat(report.isSuccess).isTrue()
  }

  @Throws(IOException::class, ProcessingException::class)
  private fun validateSchema(yaml: String): ProcessingReport {
    // This turns out to be not very suitable for yaml validating, at least for incorrect yaml: the
    // yaml is first converted to json, which is then validated; the error messages would need to be
    // adapted to the original yaml
    val factory = JsonSchemaFactory.byDefault()
    val jsonMapper = ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS)
    val yamlMapper = ObjectMapper(YAMLFactory())
    val tagSchemaSchema = jsonMapper.readTree(this.javaClass.getResource("tagschemaschema.json"))
    val schema = factory.getJsonSchema(tagSchemaSchema)
    val yamlSchema = yamlMapper.readTree(yaml)
    val report = schema.validate(yamlSchema)
    LOG.info("{}", report)
    return report
  }
}
