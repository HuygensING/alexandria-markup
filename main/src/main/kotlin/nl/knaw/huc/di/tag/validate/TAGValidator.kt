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

import nl.knaw.huc.di.tag.schema.TAGMLSchema
import nl.knaw.huc.di.tag.schema.TreeNode
import nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER
import nl.knaw.huc.di.tag.tagml.TAGML.DIVIDER
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_ENDCHAR
import nl.knaw.huc.di.tag.tagml.TAGML.OPEN_TAG_STARTCHAR
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGMarkup
import nl.knaw.huygens.alexandria.storage.TAGStore
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class TAGValidator(private val store: TAGStore) {

  fun validate(document: TAGDocument, schema: TAGMLSchema): TAGValidationResult {
    val result = TAGValidationResult()
    val layersInDocument = document.layerNames
    val layersInSchema: List<String> = schema.getLayers()
    for (layer in layersInSchema) {
      if (layersInDocument.contains(layer)) {
        validateForLayer(document, layer, schema.getLayerHierarchy(layer), result)
      }
    }
    val layersMissingInSchema: MutableList<String> = layersInDocument.toMutableList()
    layersMissingInSchema.removeAll(layersInSchema)
    if (layersMissingInSchema.isNotEmpty()) {
      val warning = if (layersMissingInSchema.size == 1)
        "Layer ${layerName(layersMissingInSchema[0])} is"
      else
        "Layers ${layersMissingInSchema.joinToString(separator = ", ") { layerName(it) }} are"
      result.warnings.add("$warning used in the document, but not defined in the schema.")
    }
    val layersMissingInDocument: MutableList<String> = layersInSchema.toMutableList()
    layersMissingInDocument.removeAll(layersInDocument)
    if (layersMissingInDocument.isNotEmpty()) {
      val warning = if (layersMissingInDocument.size == 1)
        "Layer ${layerName(layersMissingInDocument[0])} is"
      else
        "Layers ${layersMissingInDocument.joinToString(separator = ", ") { layerName(it) }} are"
      result.warnings.add("$warning defined in the schema, but not used in the document.")
    }
    return result
  }

  private fun validateForLayer(
      document: TAGDocument,
      layer: String,
      layerHierarchyRoot: TreeNode<String>,
      result: TAGValidationResult) {
    val expectedRootMarkup = layerHierarchyRoot.data
    val rootMarkupId = document.dto.textGraph.layerRootMap[layer]
    val markup = store.getMarkup(rootMarkupId)
    val hasErrors = AtomicBoolean(false)
    if (!markup.hasTag(expectedRootMarkup)) {
      result
          .errors
          .add("Layer ${layerName(layer)}: expected root markup $expectedRootMarkup, but was ${openTag(markup)}")
      hasErrors.set(true)
    }
    val markupIdsToHandle: MutableList<Long?> = ArrayList()
    markupIdsToHandle.add(rootMarkupId)
    val schemaChildNodeMap: MutableMap<String, TreeNode<String>> = HashMap()
    schemaChildNodeMap[layerHierarchyRoot.data] = layerHierarchyRoot
    while (markupIdsToHandle.isNotEmpty() && !hasErrors.get()) {
      val parentMarkupId = markupIdsToHandle.removeAt(0)
      val parentTag = store.getMarkup(parentMarkupId).tag
      val layerHierarchyNode = schemaChildNodeMap[parentTag]!!
      val expectedTags: MutableSet<String> = HashSet()
      layerHierarchyNode
          .iterator()
          .forEachRemaining { childNode: TreeNode<String> ->
            val tag = childNode.data
            schemaChildNodeMap[tag] = childNode
            expectedTags.add(tag)
          }
      document
          .getChildMarkupIdStream(parentMarkupId, layer)
          .forEach { mId: Long ->
            val markup1 = store.getMarkup(mId)
            val tag = markup1.tag
            if (expectedTags.contains(tag)) {
              markupIdsToHandle.add(mId)
            } else {
              val expectedTagString = expectedTags.joinToString(separator = " or ") { openTag(it, layer) }
              result
                  .errors
                  .add(
                      "Layer ${layerName(layer)}: expected $expectedTagString as child markup of ${openTag(parentTag, layer)}, but found ${openTag(markup1)}")
              hasErrors.set(true)
            }
          }
    }
  }

  private fun openTag(markup: TAGMarkup): String =
      OPEN_TAG_STARTCHAR + markup.extendedTag + OPEN_TAG_ENDCHAR

  private fun openTag(parentTag: String, layer: String): String {
    val layerInfo = if (layer == DEFAULT_LAYER) "" else DIVIDER + layer
    return OPEN_TAG_STARTCHAR + parentTag + layerInfo + OPEN_TAG_ENDCHAR
  }

  private fun layerName(layer: String): String =
          (if (layer == DEFAULT_LAYER) "$ (default)" else layer)

}
