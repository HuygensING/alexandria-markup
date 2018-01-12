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


import static java.util.stream.Collectors.toSet;

import javax.json.*;
import java.io.StringReader;
import java.util.Set;

public class TAGViewFactory {
  public static final TAGView SHOW_ALL_VIEW = new TAGView();

  public static TAGView fromJsonString(String json) {

    JsonReader reader = Json.createReader(new StringReader(json));
    JsonObject jsonObject = reader.readObject();
    reader.close();

    TAGView tagView = new TAGView();

    JsonArray includeArray = jsonObject.getJsonArray("include");
    if (includeArray != null) {
      Set<String> include = getMarkupTags(includeArray);
      tagView.setMarkupToInclude(include);
    }

    JsonArray excludeArray = jsonObject.getJsonArray("exclude");
    if (excludeArray != null) {
      Set<String> exclude = getMarkupTags(excludeArray);
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
}
