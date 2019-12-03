package nl.knaw.huc.di.tag.schema;

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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMLSchemaTest {
  private Logger LOG = LoggerFactory.getLogger(TAGMLSchemaTest.class);

  @Test
  public void testCorrectSchema() {
    String schemaYAML =
        "---\n"
            + "L1:\n"
            + "   root:\n"
            + "     - a\n"
            + "     - b\n"
            + "     - c\n"
            + "     - d:\n"
            + "         - d1\n"
            + "         - d2\n"
            + "L2:\n"
            + "  root:\n"
            + "    - x\n"
            + "    - 'y'\n" // because y = yes = TRUE according to the jackson parser
            + "    - z:\n"
            + "        - z1\n"
            + "        - z2\n";
    System.out.println(schemaYAML);
    final TAGMLSchemaParseResult result = TAGMLSchemaFactory.parseYAML(schemaYAML);
    assertThat(result).hasNoErrors();
    assertThat(result).hasLayers("L1", "L2");

    TreeNode<String> layerHierarchy1 = result.schema.getLayerHierarchy("L1");
    assertThat(layerHierarchy1.data).isEqualTo("root");
    List<TreeNode<String>> rootChildren = layerHierarchy1.children;
    Assertions.assertThat(rootChildren).hasSize(4);
    assertThat(rootChildren.get(0).data).isEqualTo("a");
    assertThat(rootChildren.get(1).data).isEqualTo("b");
    assertThat(rootChildren.get(2).data).isEqualTo("c");
    assertThat(rootChildren.get(3).data).isEqualTo("d");
    List<TreeNode<String>> dChildren = rootChildren.get(3).children;
    Assertions.assertThat(dChildren).hasSize(2);
    assertThat(dChildren.get(0).data).isEqualTo("d1");
    assertThat(dChildren.get(1).data).isEqualTo("d2");

    TreeNode<String> layerHierarchy2 = result.schema.getLayerHierarchy("L2");
    assertThat(layerHierarchy2.data).isEqualTo("root");
    List<TreeNode<String>> rootChildren2 = layerHierarchy2.children;
    Assertions.assertThat(rootChildren2).hasSize(3);
    assertThat(rootChildren2.get(0).data).isEqualTo("x");
    assertThat(rootChildren2.get(1).data).isEqualTo("y");
    assertThat(rootChildren2.get(2).data).isEqualTo("z");
    List<TreeNode<String>> zChildren = rootChildren2.get(2).children;
    Assertions.assertThat(zChildren).hasSize(2);
    assertThat(zChildren.get(0).data).isEqualTo("z1");
    assertThat(zChildren.get(1).data).isEqualTo("z2");
  }

  @Test
  public void testInCorrectSchemaWithMultipleRoots() throws IOException, ProcessingException {
    String schemaYAML =
        "---\n"
            + "L1:\n"
            + "  root1:\n"
            + "     - a\n"
            + "     - b\n"
            + "     - c\n"
            + "     - d:\n"
            + "         - d1\n"
            + "         - d2\n"
            + "  root2:\n"
            + "    - k\n"
            + "    - l\n"
            + "    - m:\n"
            + "        - m1\n"
            + "        - m2\n";
    System.out.println(schemaYAML);

    ProcessingReport report = validateSchema(schemaYAML);
    assertThat(report.isSuccess()).isFalse();

    TAGMLSchemaParseResult result = TAGMLSchemaFactory.parseYAML(schemaYAML);
    LOG.info("errors={}", result.errors);
    assertThat(result).hasErrors("only 1 root markup allowed; found 2 [root1, root2] in layer L1");
  }

  @Test
  public void testInCorrectYaml() throws IOException, ProcessingException {
    String schemaYAML = "i am not yaml";

    ProcessingReport report = validateSchema(schemaYAML);
    assertThat(report.isSuccess()).isFalse();

    TAGMLSchemaParseResult result = TAGMLSchemaFactory.parseYAML(schemaYAML);
    LOG.info("errors={}", result.errors);
    assertThat(result).hasErrors("no layer definitions found");
  }

  @Test
  public void testInCorrectYaml2() throws IOException, ProcessingException {
    String schemaYAML =
        "L1:\n"
            + "  - boolean: true\n"
            + "  - integer: 3\n"
            + "  - float: 3.14\n"
            + "  - string: \"something\"";

    ProcessingReport report = validateSchema(schemaYAML);
    assertThat(report.isSuccess()).isFalse();

    TAGMLSchemaParseResult result = TAGMLSchemaFactory.parseYAML(schemaYAML);
    LOG.info("errors={}", result.errors);
    assertThat(result)
        .hasErrors(
            "expected root markup with list of child markup, found (as json) [{\"boolean\":true},{\"integer\":3},{\"float\":3.14},{\"string\":\"something\"}]");
  }

  //  @Test
  public void testInCorrectSchema() throws IOException, ProcessingException {
    String schemaYAML = "yaml: no\n" + "x";

    ProcessingReport report = validateSchema(schemaYAML);
    assertThat(report.isSuccess()).isFalse();

    TAGMLSchemaParseResult result = TAGMLSchemaFactory.parseYAML(schemaYAML);
    LOG.info("errors={}", result.errors);
    assertThat(result)
        .hasErrors(
            "while scanning a simple key\n"
                + " in 'reader', line 2, column 1:\n"
                + "    x\n"
                + "    ^\n"
                + "could not find expected ':'\n"
                + " in 'reader', line 2, column 2:\n"
                + "    x\n"
                + "     ^\n"
                + "\n"
                + " at [Source: (StringReader); line: 1, column: 9]");
  }

  @Ignore("fix tagschemaschema.json first: find 'or' function")
  @Test
  public void testSchemaValidator() throws ProcessingException, IOException {
    String yaml =
        "---\n"
            + "L1:\n"
            + "   root:\n"
            + "     - a\n"
            + "     - b\n"
            + "     - c\n"
            + "     - d:\n"
            + "         - d1\n"
            + "         - d2\n"
            + "L2:\n"
            + "  root:\n"
            + "    - x\n"
            + "    - 'y'\n" // because y = yes = TRUE according to the jackson parser
            + "    - z:\n"
            + "        - z1\n"
            + "        - z2\n";

    ProcessingReport report = validateSchema(yaml);
    assertThat(report.isSuccess()).isTrue();
  }

  private ProcessingReport validateSchema(final String yaml)
      throws IOException, ProcessingException {
    // This turns out to be not very suitable for yaml validating, at least for incorrect yaml: the
    // yaml is first converted to json, which is then validated; the error messages would need to be
    // adapted to the original yaml
    final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
    final ObjectMapper jsonMapper = new ObjectMapper().enable(JsonParser.Feature.ALLOW_COMMENTS);
    final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    final JsonNode tagSchemaSchema =
        jsonMapper.readTree(this.getClass().getResource("tagschemaschema.json"));
    final JsonSchema schema = factory.getJsonSchema(tagSchemaSchema);

    final JsonNode yamlSchema = yamlMapper.readTree(yaml);

    ProcessingReport report = schema.validate(yamlSchema);
    LOG.info("{}", report);
    return report;
  }
}
