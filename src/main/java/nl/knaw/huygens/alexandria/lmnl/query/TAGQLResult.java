package nl.knaw.huygens.alexandria.lmnl.query;

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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TAGQLResult {

  private final String query;
  private final List<TAGQLResult> results = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();

  public TAGQLResult(String query) {
    this.query = query;
  }

  public TAGQLResult() {
    this.query = "";
  }

  private List<Object> values = new ArrayList<>();

  public void addResult(TAGQLResult subresult) {
    results.add(subresult);
  }

  public void addValue(Object value) {
    values.add(value);
  }

  public List<Object> getValues() {
    if (!isOk()) {
      return new ArrayList<>();
    }
    if (results.isEmpty()) {
      return values;
    }
    if (results.size() == 1) {
      return results.get(0).getValues();
    }
    return results.stream()//
        .map(TAGQLResult::getValues)//
        .collect(Collectors.toList());
  }

  public boolean isOk() {
    return errors.isEmpty();
  }

  public List<String> getErrors() {
    return errors;
  }

  public String getQuery() {
    return query;
  }

}
