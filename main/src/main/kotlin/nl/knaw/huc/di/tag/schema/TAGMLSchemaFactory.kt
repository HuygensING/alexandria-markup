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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.google.common.collect.Lists
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object TAGMLSchemaFactory {
  var LOG: Logger = LoggerFactory.getLogger(TAGMLSchemaFactory::class.java)
  private val YAML_F = YAMLFactory()
  private val mapper = ObjectMapper(YAML_F)

  @JvmStatic
  fun parseYAML(schemaYAML: String): TAGMLSchemaParseResult {
    val result = TAGMLSchemaParseResult()
    try {
      mapper
          .readTree(schemaYAML)
          .fields()
          .forEachRemaining { entry: Map.Entry<String, JsonNode> ->
            var layerName = entry.key
            if (layerName == "$") {
              layerName = DEFAULT_LAYER
            }
            result.schema.addLayer(layerName)
            val jsonNode = entry.value
            LOG.info("layer={}", layerName)
            LOG.info("jsonNode={}", jsonNode)
            if (!jsonNode.isObject) {
              result
                  .errors
                  .add("expected root markup with list of child markup, found (as json) $jsonNode")
            } else {
              if (jsonNode.size() > 1) {
                result
                    .errors
                    .add(
                        "only 1 root markup allowed; found ${jsonNode.size()} ${Lists.newArrayList(jsonNode.fieldNames())} in layer $layerName")
              } else {
                val layerHierarchy = buildLayerHierarchy(jsonNode)
                result.schema.setLayerHierarchy(layerName, layerHierarchy)
              }
            }
          }
    } catch (e: Exception) {
      result.errors.add(e.message!!)
    }
    if (result.schema.getLayers().isEmpty()) {
      result.errors.add("no layer definitions found")
    }
    LOG.info("result={}", result)
    return result
  }

  private fun buildLayerHierarchy(jsonNode: JsonNode): TreeNode<String> {
    var newJsonNode = jsonNode
    var content = newJsonNode.textValue()
    if (newJsonNode.isObject) {
      val next = newJsonNode.fields().next()
      content = next.key
      newJsonNode = next.value
    }
    val hierarchy = TreeNode(content)
    newJsonNode.elements().forEachRemaining { n: JsonNode -> hierarchy.addChild(buildLayerHierarchy(n)) }
    return hierarchy
  }
}
