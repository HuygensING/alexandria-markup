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

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class TAGViewFactoryTest extends AlexandriaBaseStoreTest {
  private final TAGViewFactory tagViewFactory = new TAGViewFactory(store);

  @Test
  public void fromJsonWithLayersInclusion() {
    Set<String> included = new HashSet<>(asList("A", "B"));
    TAGView expected = new TAGView(store).setLayersToInclude(included);

    String json = "{'includeLayers':['A','B']}".replace("'", "\"");
    TAGView view = tagViewFactory.fromJsonString(json);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void fromJsonWithLayersExclusion() {
    Set<String> excluded = new HashSet<>(asList("A", "B"));
    TAGView expected = new TAGView(store).setLayersToExclude(excluded);

    String json = "{'excludeLayers':['A','B']}".replace("'", "\"");
    TAGView view = tagViewFactory.fromJsonString(json);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void fromJsonWithMarkupInclusion() {
    Set<String> included = new HashSet<>(asList("chapter", "p"));
    TAGView expected = new TAGView(store).setMarkupToInclude(included);

    String json = "{'includeMarkup':['chapter','p']}".replace("'", "\"");
    TAGView view = tagViewFactory.fromJsonString(json);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void fromJsonWithMarkupExclusion() {
    Set<String> excluded = new HashSet<>(asList("verse", "l"));
    TAGView expected = new TAGView(store).setMarkupToExclude(excluded);

    String json = "{'excludeMarkup':['verse','l']}".replace("'", "\"");
    TAGView view = tagViewFactory.fromJsonString(json);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void fromDefinitionWithMarkupInclusion() {
    Set<String> included = new HashSet<>(asList("chapter", "p"));
    TAGView expected = new TAGView(store).setMarkupToInclude(included);

    TAGViewDefinition def = new TAGViewDefinition().setIncludeMarkup(included);
    TAGView view = tagViewFactory.fromDefinition(def);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void fromDefinitionWithMarkupExclusion() {
    Set<String> excluded = new HashSet<>(asList("verse", "l"));
    TAGView expected = new TAGView(store).setMarkupToExclude(excluded);

    TAGViewDefinition def = new TAGViewDefinition().setExcludeMarkup(excluded);
    TAGView view = tagViewFactory.fromDefinition(def);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }
  @Test
  public void fromDefinitionWithLayersInclusion() {
    Set<String> included = new HashSet<>(asList("L1", "L2"));
    TAGView expected = new TAGView(store).setLayersToInclude(included);

    TAGViewDefinition def = new TAGViewDefinition().setIncludeLayers(included);
    TAGView view = tagViewFactory.fromDefinition(def);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void fromDefinitionWithLayersExclusion() {
    Set<String> excluded = new HashSet<>(asList("L3", "L4"));
    TAGView expected = new TAGView(store).setLayersToExclude(excluded);

    TAGViewDefinition def = new TAGViewDefinition().setExcludeLayers(excluded);
    TAGView view = tagViewFactory.fromDefinition(def);

    assertThat(view).isEqualToComparingFieldByField(expected);
  }
}
