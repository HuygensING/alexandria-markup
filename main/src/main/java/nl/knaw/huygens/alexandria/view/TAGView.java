package nl.knaw.huygens.alexandria.view;

/*
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

import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.TAGMarkupDAO;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.view.TAGView.RelevanceStyle.*;

public class TAGView {
  private final TAGStore store;

  enum RelevanceStyle {include, exclude, undefined}

  private RelevanceStyle layerRelevance = undefined;
  private RelevanceStyle markupRelevance = undefined;

  private Set<String> layersToInclude = new HashSet<>();
  private Set<String> layersToExclude = new HashSet<>();
  private Set<String> markupToInclude = new HashSet<>();
  private Set<String> markupToExclude = new HashSet<>();

  public TAGView(TAGStore store) {
    this.store = store;
  }

  public Set<String> filterRelevantLayers(final Set<String> layerNames) {
    Set<String> relevantLayers = new HashSet<>(layerNames);
    if (layerRelevance.equals(RelevanceStyle.include)) {
      relevantLayers.clear();
      relevantLayers.addAll(layersToInclude);

    } else if (layerRelevance.equals(RelevanceStyle.exclude)) {
      relevantLayers.removeAll(layersToExclude);
    }
    return relevantLayers;
  }

  public Set<Long> filterRelevantMarkup(Set<Long> markupIds) {
    Set<Long> relevantMarkupIds = new LinkedHashSet<>(markupIds);
    if (include.equals(layerRelevance)) {
      List<Long> retain = markupIds.stream()//
          .filter(m -> hasOverlap(layersToInclude, getLayers(m)) || isInDefaultLayerOnly(m))//
          .collect(toList());
      relevantMarkupIds.retainAll(retain);

    } else if (exclude.equals(layerRelevance)) {
      List<Long> remove = markupIds.stream()//
          .filter(m -> hasOverlap(layersToExclude, getLayers(m)) && !isInDefaultLayerOnly(m))//
          .collect(toList());

      relevantMarkupIds.removeAll(remove);
    }
    if (include.equals(markupRelevance)) {
      List<Long> retain = markupIds.stream()//
          .filter(m -> markupToInclude.contains(getTag(m)))//
          .collect(toList());
      relevantMarkupIds.retainAll(retain);

    } else if (exclude.equals(markupRelevance)) {
      List<Long> remove = markupIds.stream()//
          .filter(m -> markupToExclude.contains(getTag(m)))//
          .collect(toList());

      relevantMarkupIds.removeAll(remove);
    }
    return relevantMarkupIds;
  }

//  private boolean isInDefaultLayerOnly(final Long markupId) {
//    return getLayers(markupId).stream().anyMatch(TAGML.DEFAULT_LAYER::equals);
//  }

  private boolean isInDefaultLayerOnly(final Long markupId) {
    Set<String> layers = getLayers(markupId);
    return layers.size() == 1
        && layers.iterator().next().equals(TAGML.DEFAULT_LAYER);
  }

  private boolean hasOverlap(Set<String> layersToInclude, Set<String> layers) {
    Set<String> overlap = new HashSet<>(layers);
    overlap.retainAll(layersToInclude);
    return !overlap.isEmpty();
  }

  private Set<String> getLayers(Long markupId) {
    return store.getMarkup(markupId).getLayers();
  }

  public TAGView setLayersToInclude(Set<String> layersToInclude) {
    if (exclude.equals(layerRelevance)) {
      throw new RuntimeException("This TAGView already has set layersToExclude");
    }
    this.layersToInclude = layersToInclude;
    layerRelevance = include;
    return this;
  }

  public Set<String> getLayersToInclude() {
    return layersToInclude;
  }

  public TAGView setLayersToExclude(Set<String> layersToExclude) {
    if (include.equals(layerRelevance)) {
      throw new RuntimeException("This TAGView already has set layersToInclude");
    }
    this.layersToExclude = layersToExclude;
    layerRelevance = exclude;
    return this;
  }

  public Set<String> getLayersToExclude() {
    return layersToExclude;
  }

  public TAGView setMarkupToInclude(Set<String> markupToInclude) {
    if (exclude.equals(markupRelevance)) {
      throw new RuntimeException("This TAGView already has set markupToExclude");
    }
    this.markupToInclude = markupToInclude;
    markupRelevance = include;
    return this;
  }

  public Set<String> getMarkupToInclude() {
    return markupToInclude;
  }

  public TAGView setMarkupToExclude(Set<String> markupToExclude) {
    if (include.equals(markupRelevance)) {
      throw new RuntimeException("This TAGView already has set markupToInclude");
    }
    this.markupToExclude = markupToExclude;
    markupRelevance = exclude;
    return this;
  }

  public Set<String> getMarkupToExclude() {
    return markupToExclude;
  }

  public TAGViewDefinition getDefinition() {
    return new TAGViewDefinition()
        .setIncludeLayers(layersToInclude)
        .setExcludeLayers(layersToExclude)
        .setIncludeMarkup(markupToInclude)
        .setExcludeMarkup(markupToExclude);
  }

  public boolean markupStyleIsInclude() {
    return include.equals(markupRelevance);
  }

  public boolean markupStyleIsExclude() {
    return exclude.equals(markupRelevance);
  }

  public boolean layerStyleIsInclude() {
    return include.equals(layerRelevance);
  }

  public boolean layerStyleIsExclude() {
    return exclude.equals(layerRelevance);
  }

  public boolean isIncluded(TAGMarkupDAO tagMarkupDAO) {
    String tag = tagMarkupDAO.getTag();
    if (include.equals(markupRelevance)) {
      return markupToInclude.contains(tag);
    }
    return exclude.equals(markupRelevance) && !markupToExclude.contains(tag);
  }

  private String getTag(Long markupId) {
    return store.getMarkup(markupId).getTag();
  }

}
