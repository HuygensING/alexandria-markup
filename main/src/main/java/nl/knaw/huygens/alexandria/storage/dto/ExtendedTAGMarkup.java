package nl.knaw.huygens.alexandria.storage.dto;

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
import nl.knaw.huc.di.tag.model.graph.edges.ContinuationEdge;
import nl.knaw.huc.di.tag.tagml.TAGML;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Set;

import static java.util.stream.Collectors.joining;
import static nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER;

public class ExtendedTAGMarkup {

  private TAGMarkup markup;

  public ExtendedTAGMarkup(TAGMarkup markup) {
    this.markup = markup;
  }

  public String getExtendedTag() {
    return getExtendedTag(Collections.emptySet());
  }

  public String getExtendedTag(final Set<String> newLayers) {
    String layerSuffix = layerSuffix(newLayers);
    String tag = markup.getTag();
    if (markup.isOptional()) {
      return TAGML.OPTIONAL_PREFIX + tag + layerSuffix;
    }
    String suffix = markup.getSuffix();
    if (StringUtils.isNotEmpty(suffix)) {
      return tag + "~" + suffix + layerSuffix;
    }
    return tag + layerSuffix;
  }

  String layerSuffix(final Set<String> newLayers) {
    String layerSuffix = markup.getLayers().stream()
        .filter(l -> !DEFAULT_LAYER.equals(l))
        .map(l -> newLayers.contains(l) ? "+" + l : l)
        .collect(joining(","));
    return layerSuffix.isEmpty() ? "" : TAGML.DIVIDER + layerSuffix;
  }

  public boolean hasTag(String tag) {
    return tag.equals(markup.getTag());
  }

  public boolean hasMarkupId() {
    return markup.getMarkupId() != null;
  }

  @Deprecated
  public int getTextNodeCount() {
    return 0;
  }

}
