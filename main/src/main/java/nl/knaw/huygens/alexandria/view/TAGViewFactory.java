package nl.knaw.huygens.alexandria.view;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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
  private final TAGStore store;

  public TAGViewFactory(TAGStore store) {
    this.store = store;
  }

  public TAGView fromDefinition(TAGViewDefinition definition) {
    return createTAGView(definition.getInclude(), definition.getExclude());
  }

  public TAGView fromJsonString(String json) {
    JsonReader reader = Json.createReader(new StringReader(json));
    JsonObject jsonObject = reader.readObject();
    reader.close();

    return toTagView(jsonObject);
  }

  private TAGView toTagView(JsonObject jsonObject) {
    JsonArray includeArray = jsonObject.getJsonArray("include");
    JsonArray excludeArray = jsonObject.getJsonArray("exclude");
    Set<String> include = null;
    Set<String> exclude = null;

    if (includeArray != null) {
      include = getMarkupTags(includeArray);
    }

    if (excludeArray != null) {
      exclude = getMarkupTags(excludeArray);
    }

    return createTAGView(include, exclude);
  }

  private TAGView createTAGView(Set<String> include, Set<String> exclude) {
    TAGView tagView = new TAGView(store);
    if (include != null && !include.isEmpty()) {
      tagView.setMarkupToInclude(include);
    }
    if (exclude != null && !exclude.isEmpty()) {
      tagView.setMarkupToExclude(exclude);
    }
    return tagView;
  }

  private static Set<String> getMarkupTags(JsonArray jsonArray) {
    return jsonArray.getValuesAs(JsonString.class)//
        .stream()//
        .map(JsonString::getString)//
        .collect(toSet());
  }

  public TAGView getDefaultView() {
    return new TAGView(store);
  }
}
