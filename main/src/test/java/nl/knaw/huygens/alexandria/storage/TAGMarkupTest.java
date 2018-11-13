package nl.knaw.huygens.alexandria.storage;

/*-
 * #%L
 * alexandria-markup
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
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TAGMarkupTest {

  @Test
  public void testLayerSuffix() {
    Set<String> markupLayers = new HashSet<>(asList("A", "B"));
    Set<String> newLayers = new HashSet<>(Collections.singletonList("B"));
    String suffix = getLayerSuffix(markupLayers, newLayers);
    assertThat(suffix).isEqualTo("|A,+B");
  }

  @Test
  public void testLayerSuffix1() {
    Set<String> markupLayers = new HashSet<>(asList(TAGML.DEFAULT_LAYER, "A", "B"));
    Set<String> newLayers = new HashSet<>(Collections.singletonList(TAGML.DEFAULT_LAYER));

    String suffix = getLayerSuffix(markupLayers, newLayers);
    assertThat(suffix).isEqualTo("|A,B");
  }

  @Test
  public void testLayerSuffix2() {
    Set<String> markupLayers = new HashSet<>(Collections.singletonList(TAGML.DEFAULT_LAYER));
    Set<String> newLayers = new HashSet<>(Collections.singletonList(TAGML.DEFAULT_LAYER));

    String suffix = getLayerSuffix(markupLayers, newLayers);
    assertThat(suffix).isEqualTo("");
  }

  private String getLayerSuffix(Set<String> layers, Set<String> newLayers) {
    TAGStore mockStore = mock(TAGStore.class);
    TAGMarkupDTO mockDTO = mock(TAGMarkupDTO.class);
    when(mockDTO.getLayers()).thenReturn(layers);
    TAGMarkup tm = new TAGMarkup(mockStore, mockDTO);
    return tm.layerSuffix(newLayers);
  }


}
