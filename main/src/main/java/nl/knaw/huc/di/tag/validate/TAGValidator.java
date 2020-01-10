package nl.knaw.huc.di.tag.validate;

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

import nl.knaw.huc.di.tag.schema.TAGMLSchema;
import nl.knaw.huc.di.tag.schema.TreeNode;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.stream.Collectors.joining;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class TAGValidator {

  private TAGStore store;

  public TAGValidator(final TAGStore store) {
    this.store = store;
  }

  public TAGValidationResult validate(TAGDocument document, TAGMLSchema schema) {
    final TAGValidationResult result = new TAGValidationResult();
    final Set<String> layersInDocument = document.getLayerNames();
    final List<String> layersInSchema = schema.getLayers();
    for (String layer : layersInSchema) {
      if (layersInDocument.contains(layer)) {
        validateForLayer(document, layer, schema.getLayerHierarchy(layer), result);
      }
    }

    List<String> layersMissingInSchema = new ArrayList(layersInDocument);
    layersMissingInSchema.removeAll(layersInSchema);
    if (!layersMissingInSchema.isEmpty()) {
      String warning =
          (layersMissingInSchema.size() == 1)
              ? "Layer " + layerName(layersMissingInSchema.get(0)) + " is"
              : "Layers "
                  + layersMissingInSchema.stream().map(this::layerName).collect(joining(", "))
                  + " are";
      result.warnings.add(warning + " used in the document, but not defined in the schema.");
    }

    List<String> layersMissingInDocument = new ArrayList(layersInSchema);
    layersMissingInDocument.removeAll(layersInDocument);
    if (!layersMissingInDocument.isEmpty()) {
      String warning =
          (layersMissingInDocument.size() == 1)
              ? "Layer " + layerName(layersMissingInDocument.get(0)) + " is"
              : "Layers "
                  + layersMissingInDocument.stream().map(this::layerName).collect(joining(", "))
                  + " are";
      result.warnings.add(warning + " defined in the schema, but not used in the document.");
    }

    return result;
  }

  private void validateForLayer(
      final TAGDocument document,
      final String layer,
      final TreeNode<String> layerHierarchyRoot,
      final TAGValidationResult result) {
    String expectedRootMarkup = layerHierarchyRoot.getData();
    Long rootMarkupId = document.getDTO().textGraph.getLayerRootMap().get(layer);
    TAGMarkup markup = store.getMarkup(rootMarkupId);
    AtomicBoolean hasErrors = new AtomicBoolean(false);
    if (!markup.hasTag(expectedRootMarkup)) {
      result.errors.add(
          "Layer "
              + layerName(layer)
              + ": expected root markup "
              + expectedRootMarkup
              + ", but was "
              + openTag(markup));
      hasErrors.set(true);
    }

    List<Long> markupIdsToHandle = new ArrayList<>();
    markupIdsToHandle.add(rootMarkupId);
    Map<String, TreeNode<String>> schemaChildNodeMap = new HashMap<>();
    schemaChildNodeMap.put(layerHierarchyRoot.getData(), layerHierarchyRoot);
    while (!markupIdsToHandle.isEmpty() && !hasErrors.get()) {
      Long parentMarkupId = markupIdsToHandle.remove(0);
      String parentTag = store.getMarkup(parentMarkupId).getTag();
      TreeNode<String> layerHierarchyNode = schemaChildNodeMap.get(parentTag);
      Set<String> expectedTags = new HashSet<>();
      layerHierarchyNode
          .iterator()
          .forEachRemaining(
              childNode -> {
                String tag = childNode.getData();
                schemaChildNodeMap.put(tag, childNode);
                expectedTags.add(tag);
              });
      document
          .getChildMarkupIdStream(parentMarkupId, layer)
          .forEach(
              mId -> {
                TAGMarkup markup1 = store.getMarkup(mId);
                String tag = markup1.getTag();
                if (expectedTags.contains(tag)) {
                  markupIdsToHandle.add(mId);
                } else {
                  result.errors.add(
                      "Layer "
                          + layerName(layer)
                          + ": expected "
                          + expectedTags.stream()
                              .map(t -> openTag(t, layer))
                              .collect(joining(" or "))
                          + " as child markup of "
                          + openTag(parentTag, layer)
                          + ", but found "
                          + openTag(markup1));
                  hasErrors.set(true);
                }
              });
    }
  }

  private String openTag(TAGMarkup markup) {
    return OPEN_TAG_STARTCHAR + markup.getExtendedTag() + OPEN_TAG_ENDCHAR;
  }

  private String openTag(String parentTag, String layer) {
    String layerInfo = layer.equals(DEFAULT_LAYER) ? "" : DIVIDER + layer;
    return OPEN_TAG_STARTCHAR + parentTag + layerInfo + OPEN_TAG_ENDCHAR;
  }

  private String layerName(String layer) {
    return layer.equals(TAGML.DEFAULT_LAYER) ? "$ (default)" : layer;
  }
}
