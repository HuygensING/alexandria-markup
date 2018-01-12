package nl.knaw.huygens.alexandria;

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

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.TAGView.RelevanceStyle.*;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class TAGView {
  public static final TAGView SHOW_ALL_VIEW = new TAGView();

  enum RelevanceStyle {include, exclude, undefined}

  private RelevanceStyle relevanceStyle = undefined;

  private Set<String> markupToInclude = new HashSet<>();
  private Set<String> markupToExclude = new HashSet<>();

  public Set<Markup> filterRelevantMarkup(Set<Markup> markups) {
    Set<Markup> relevantMarkups = new LinkedHashSet<>(markups);
    if (include.equals(relevanceStyle)) {
      List<Markup> retain = markups.stream()//
          .filter(m -> markupToInclude.contains(m.getTag()))//
          .collect(toList());
      relevantMarkups.retainAll(retain);

    } else if (exclude.equals(relevanceStyle)) {
      List<Markup> remove = markups.stream()//
          .filter(m -> markupToExclude.contains(m.getTag()))//
          .collect(toList());

      relevantMarkups.removeAll(remove);
    }
    return relevantMarkups;
  }

  public TAGView setMarkupToInclude(Set<String> markupToInclude) {
    if (exclude.equals(relevanceStyle)) {
      throw new RuntimeException("This TAGView already has set markupToExclude");
    }
    this.markupToInclude = markupToInclude;
    relevanceStyle = include;
    return this;
  }

  public TAGView setMarkupToExclude(Set<String> markupToExclude) {
    if (include.equals(relevanceStyle)) {
      throw new RuntimeException("This TAGView already has set markupToInclude");
    }
    this.markupToExclude = markupToExclude;
    relevanceStyle = exclude;
    return this;
  }
}
