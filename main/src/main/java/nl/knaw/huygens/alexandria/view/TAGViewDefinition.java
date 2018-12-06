package nl.knaw.huygens.alexandria.view;

/*
 * #%L
 * main
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import java.util.HashSet;
import java.util.Set;

public class TAGViewDefinition {
  private Set<String> includeLayers = new HashSet<>();
  private Set<String> excludeLayers = new HashSet<>();
  private Set<String> includeMarkup = new HashSet<>();
  private Set<String> excludeMarkup = new HashSet<>();

  public Set<String> getIncludeLayers() {
    return includeLayers;
  }

  public TAGViewDefinition setIncludeLayers(final Set<String> includeLayers) {
    this.includeLayers = includeLayers;
    return this;
  }

  public Set<String> getExcludeLayers() {
    return excludeLayers;
  }

  public TAGViewDefinition setExcludeLayers(final Set<String> excludeLayers) {
    this.excludeLayers = excludeLayers;
    return this;
  }

  public Set<String> getIncludeMarkup() {
    return includeMarkup;
  }

  public TAGViewDefinition setIncludeMarkup(Set<String> includeMarkup) {
    this.includeMarkup = includeMarkup;
    return this;
  }

  public Set<String> getExcludeMarkup() {
    return excludeMarkup;
  }

  public TAGViewDefinition setExcludeMarkup(Set<String> excludeMarkup) {
    this.excludeMarkup = excludeMarkup;
    return this;
  }

}
