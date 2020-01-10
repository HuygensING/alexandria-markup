package nl.knaw.huc.di.tag.sparql;

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
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class SPARQLResult {
  private final String query;
  private final List<SPARQLResult> results = new ArrayList<>();
  private final List<String> errors = new ArrayList<>();
  private final List<Object> values = new ArrayList();

  public SPARQLResult(String query) {
    this.query = query;
  }

  public SPARQLResult() {
    this("");
  }

  public void addResult(SPARQLResult subresult) {
    this.results.add(subresult);
  }

  public void addValue(Object value) {
    this.values.add(value);
  }

  public List<Object> getValues() {
    if (!this.isOk()) {
      return new ArrayList();
    } else if (this.results.isEmpty()) {
      return this.values;
    } else {
      return this.results.size() == 1 ? ((SPARQLResult) this.results.get(0)).getValues()
          : (List) this.results.stream()
          .map(SPARQLResult::getValues)
          .collect(toList());
    }
  }

  public boolean isOk() {
    return this.errors.isEmpty();
  }

  public List<String> getErrors() {
    return this.errors;
  }

  public String getQuery() {
    return this.query;
  }
}
