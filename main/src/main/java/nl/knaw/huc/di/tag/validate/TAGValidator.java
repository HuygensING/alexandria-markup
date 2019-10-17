package nl.knaw.huc.di.tag.validate;

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
import nl.knaw.huc.di.tag.schema.TAGMLSchema;
import nl.knaw.huc.di.tag.schema.TreeNode;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;

public class TAGValidator {

  private TAGStore store;

  public TAGValidator(final TAGStore store) {
    this.store = store;
  }

  public TAGValidationResult validate(TAGDocument document, TAGMLSchema schema) {
    final TAGValidationResult result = new TAGValidationResult();
    for (String layer : schema.getLayers()) {
      validateForLayer(document, layer, schema.getLayerHierarchy(layer), result);
    }
    return result;
  }

  private void validateForLayer(final TAGDocument document, final String layer, final TreeNode<String> layerHierarchyRoot, final TAGValidationResult result) {
    String expectedRootMarkup = layerHierarchyRoot.toString();
    document.getDTO().textGraph.getLayerRootMap().get(layer);
  }
}
