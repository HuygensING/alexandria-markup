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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import nl.knaw.huc.di.tag.tagml.TAGML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TAGMLSchemaFactory {
  static Logger LOG = LoggerFactory.getLogger(TAGMLSchemaFactory.class);

  static final YAMLFactory YAML_F = new YAMLFactory();
  static final ObjectMapper mapper = new ObjectMapper(YAML_F);

  public static TAGMLSchemaParseResult parseYAML(String schemaYAML) {
    final TAGMLSchemaParseResult result = new TAGMLSchemaParseResult();
    try {
      mapper
          .readTree(schemaYAML)
          .fields()
          .forEachRemaining(
              entry -> {
                String layerName = entry.getKey();
                if (layerName == "$") {
                  layerName = TAGML.DEFAULT_LAYER;
                }
                result.schema.addLayer(layerName);
                JsonNode jsonNode = entry.getValue();
                LOG.info("layer={}", layerName);
                LOG.info("jsonNode={}", jsonNode);
                if (!jsonNode.isObject()) {
                  result.errors.add("expected list of child markup, found (as json) " + jsonNode);
                } else {
                  if (jsonNode.size() > 1) {
                    result.errors.add(
                        "only 1 root markup allowed; found "
                            + jsonNode.size()
                            + " "
                            + Lists.newArrayList(jsonNode.fieldNames())
                            + " in layer "
                            + layerName);
                  } else {
                    TreeNode<String> layerHierarchy = buildLayerHierarchy(jsonNode);
                    result.schema.setLayerHierarchy(layerName, layerHierarchy);
                  }
                }
              });
    } catch (Exception e) {
      result.errors.add(e.getMessage());
    }
    if (result.schema.getLayers().isEmpty()) {
      result.errors.add("no layer definitions found");
    }
    LOG.info("result={}", result);
    return result;
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
