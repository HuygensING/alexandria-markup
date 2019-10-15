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

  public static TAGMLSchema fromYAML(String schemaYAML) {
    try {
      ObjectMapper mapper = new ObjectMapper(YAML_F);
      JsonNode jsonNode = mapper.readTree(schemaYAML);
      TAGMLSchema tagmlSchema = new TAGMLSchema();
      jsonNode
          .fields()
          .forEachRemaining(
              entry -> {
                tagmlSchema.addLayer(entry.getKey());
                JsonNode jsonNode1 = entry.getValue();
                TreeNode<String> layerHierarchy = buildLayerHierarchy(jsonNode1);
                tagmlSchema.setLayerHierarchy(entry.getKey(), layerHierarchy);
              });
      return tagmlSchema;
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
