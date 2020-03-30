package nl.knaw.huygens.alexandria.view;

/*
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

import nl.knaw.huygens.alexandria.storage.TAGStore;

import javax.json.*;
import java.io.StringReader;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

public class TAGViewFactory {
  public static final String INCLUDE_LAYERS = "includeLayers";
  public static final String EXCLUDE_LAYERS = "excludeLayers";
  public static final String INCLUDE_MARKUP = "includeMarkup";
  public static final String EXCLUDE_MARKUP = "excludeMarkup";
  public static final String MARKUP_WITH_LAYER_EXCLUSIVE_TEXT = "markupWithLayerExclusiveText";
  private final TAGStore store;

  public TAGViewFactory(TAGStore store) {
    this.store = store;
  }

  private static Set<String> getElements(JsonArray jsonArray) {
    return jsonArray.getValuesAs(JsonString.class) //
        .stream() //
        .map(JsonString::getString) //
        .collect(toSet());
  }

  public TAGView fromJsonString(String json) {
    JsonReader reader = Json.createReader(new StringReader(json));
    JsonObject jsonObject = reader.readObject();
    reader.close();

    return toTagView(jsonObject);
  }

  public TAGView fromDefinition(TAGViewDefinition definition) {
    return createTAGView(
        definition.getIncludeLayers(),
        definition.getExcludeLayers(),
        definition.getIncludeMarkup(),
        definition.getExcludeMarkup(),
        definition.getMarkupWithLayerExclusiveText());
  }

  private TAGView toTagView(JsonObject jsonObject) {
    JsonArray includeLayerArray = jsonObject.getJsonArray(INCLUDE_LAYERS);
    JsonArray excludeLayerArray = jsonObject.getJsonArray(EXCLUDE_LAYERS);
    JsonArray includeMarkupArray = jsonObject.getJsonArray(INCLUDE_MARKUP);
    JsonArray excludeMarkupArray = jsonObject.getJsonArray(EXCLUDE_MARKUP);
    JsonArray markupWithLayerExclusiveTextArray =
        jsonObject.getJsonArray(MARKUP_WITH_LAYER_EXCLUSIVE_TEXT);
    Set<String> includeLayers = null;
    Set<String> excludeLayers = null;
    Set<String> includeMarkup = null;
    Set<String> excludeMarkup = null;
    Set<String> markupWithLayerExclusiveText = null;

    if (includeLayerArray != null) {
      includeLayers = getElements(includeLayerArray);
    }

    if (excludeLayerArray != null) {
      excludeLayers = getElements(excludeLayerArray);
    }

    if (includeMarkupArray != null) {
      includeMarkup = getElements(includeMarkupArray);
    }

    if (excludeMarkupArray != null) {
      excludeMarkup = getElements(excludeMarkupArray);
    }

    if (markupWithLayerExclusiveTextArray != null) {
      markupWithLayerExclusiveText = getElements(markupWithLayerExclusiveTextArray);
    }

    return createTAGView(
        includeLayers, excludeLayers, includeMarkup, excludeMarkup, markupWithLayerExclusiveText);
  }

  private boolean notEmpty(final Set<String> stringSet) {
    return stringSet != null && !stringSet.isEmpty();
  }

  private TAGView createTAGView(
      final Set<String> includeLayers,
      final Set<String> excludeLayers,
      Set<String> includeMarkup,
      Set<String> excludeMarkup,
      Set<String> markupWithLayerExclusiveText) {
    TAGView tagView = new TAGView(store);
    if (notEmpty(includeLayers)) {
      tagView.setLayersToInclude(includeLayers);
    }
    if (notEmpty(excludeLayers)) {
      tagView.setLayersToExclude(excludeLayers);
    }
    if (notEmpty(includeMarkup)) {
      tagView.setMarkupToInclude(includeMarkup);
    }
    if (notEmpty(excludeMarkup)) {
      tagView.setMarkupToExclude(excludeMarkup);
    }
    if (notEmpty(markupWithLayerExclusiveText)) {
      tagView.setMarkupWithLayerExclusiveText(markupWithLayerExclusiveText);
    }
    return tagView;
  }

  public TAGView getDefaultView() {
    return new TAGView(store);
  }
}
