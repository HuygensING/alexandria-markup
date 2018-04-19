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
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.view.TAGView.RelevanceStyle.*;

public class TAGView {
  private final TAGStore store;

  enum RelevanceStyle {include, exclude, undefined}

  private RelevanceStyle relevanceStyle = undefined;

  private Set<String> markupToInclude = new HashSet<>();
  private Set<String> markupToExclude = new HashSet<>();

  public TAGView(TAGStore store) {
    this.store = store;
  }

  public Set<Long> filterRelevantMarkup(Set<Long> markupIds) {
    Set<Long> relevantMarkupIds = new LinkedHashSet<>(markupIds);
    if (include.equals(relevanceStyle)) {
      List<Long> retain = markupIds.stream()//
          .filter(m -> markupToInclude.contains(getTag(m)))//
          .collect(toList());
      relevantMarkupIds.retainAll(retain);

    } else if (exclude.equals(relevanceStyle)) {
      List<Long> remove = markupIds.stream()//
          .filter(m -> markupToExclude.contains(getTag(m)))//
          .collect(toList());

      relevantMarkupIds.removeAll(remove);
    }
    return relevantMarkupIds;
  }

  public TAGView setMarkupToInclude(Set<String> markupToInclude) {
    if (exclude.equals(relevanceStyle)) {
      throw new RuntimeException("This TAGView already has set markupToExclude");
    }
    this.markupToInclude = markupToInclude;
    relevanceStyle = include;
    return this;
  }

  public Set<String> getMarkupToInclude() {
    return markupToInclude;
  }

  public TAGView setMarkupToExclude(Set<String> markupToExclude) {
    if (include.equals(relevanceStyle)) {
      throw new RuntimeException("This TAGView already has set markupToInclude");
    }
    this.markupToExclude = markupToExclude;
    relevanceStyle = exclude;
    return this;
  }

  public Set<String> getMarkupToExclude() {
    return markupToExclude;
  }

  public TAGViewDefinition getDefinition() {
    return new TAGViewDefinition()
        .setInclude(markupToInclude)
        .setExclude(markupToExclude);
  }

  public boolean styleIsInclude() {
    return include.equals(relevanceStyle);
  }

  public boolean styleIsExclude() {
    return exclude.equals(relevanceStyle);
  }

  public boolean isIncluded(MarkupWrapper markupWrapper) {
    String tag = markupWrapper.getTag();
    if (include.equals(relevanceStyle)) {
      return markupToInclude.contains(tag);
    }
    return exclude.equals(relevanceStyle) && !markupToExclude.contains(tag);
  }

  private String getTag(Long markupId) {
    return store.getMarkupWrapper(markupId).getTag();
  }

}
