package nl.knaw.huygens.alexandria.compare;

/*-
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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGComparison2Test {

  @Test
  public void testPairsAreIndependent1() {
    Pair<Integer, Integer> p00 = pair(0, 0);
    Pair<Integer, Integer> p01 = pair(0, 1);
    Pair<Integer, Integer> p11 = pair(1, 1);
    Pair<Integer, Integer> p10 = pair(1, 0);
    List<Pair<Integer, Integer>> singleton = collection(p10);
    List<Pair<Integer, Integer>> independent = collection(p00, p11);
    List<Pair<Integer, Integer>> dependent = collection(p00, p01, p11);
    assertThat(TAGComparison2.pairsAreIndependent(singleton)).isTrue();
    assertThat(TAGComparison2.pairsAreIndependent(independent)).isTrue();
    assertThat(TAGComparison2.pairsAreIndependent(dependent)).isFalse();
  }

  private List<Pair<Integer, Integer>> collection(Pair<Integer, Integer>... pair) {
    List<Pair<Integer, Integer>> collection = new ArrayList<>();
    for (Pair<Integer, Integer> p : pair) {
      collection.add(p);
    }
    return collection;
  }

  private Pair<Integer, Integer> pair(int l, int r) {
    return new ImmutablePair<>(l, r);
  }
}
