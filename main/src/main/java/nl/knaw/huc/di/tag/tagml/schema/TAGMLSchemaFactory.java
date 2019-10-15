package nl.knaw.huc.di.tag.tagml.schema;

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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.util.Map;

public class TAGMLSchemaFactory {
  static final YAMLFactory YAML_F = new YAMLFactory();

  public static TAGMLSchemaParseResult parseYAML(String schemaYAML) {
    try {
      final TAGMLSchemaParseResult result = new TAGMLSchemaParseResult();
      ObjectMapper mapper = new ObjectMapper(YAML_F);
      mapper
          .readTree(schemaYAML)
          .fields()
          .forEachRemaining(
              entry -> {
                result.schema.addLayer(entry.getKey());
                JsonNode jsonNode = entry.getValue();
                TreeNode<String> layerHierarchy = buildLayerHierarchy(jsonNode);
                result.schema.setLayerHierarchy(entry.getKey(), layerHierarchy);
              });
      return result;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static TreeNode<String> buildLayerHierarchy(JsonNode jsonNode) {
    String content = jsonNode.textValue();
    if (jsonNode.isObject()) {
      Map.Entry<String, JsonNode> next = jsonNode.fields().next();
      content = next.getKey();
      jsonNode = next.getValue();
    }
    TreeNode<String> hierarchy = new TreeNode<>(content);
    jsonNode.elements().forEachRemaining(n -> hierarchy.addChild(buildLayerHierarchy(n)));
    return hierarchy;
  }
}
