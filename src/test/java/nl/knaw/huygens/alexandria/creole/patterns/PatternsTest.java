package nl.knaw.huygens.alexandria.creole.patterns;

    /*-
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

import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;
import nl.knaw.huygens.alexandria.creole.CreoleTest;
import nl.knaw.huygens.alexandria.creole.NameClass;
import static nl.knaw.huygens.alexandria.creole.NameClasses.name;
import nl.knaw.huygens.alexandria.creole.Pattern;
import org.junit.Test;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PatternsTest extends CreoleTest {

  private static final Random RANDOM = new Random();

  @Test
  public void testHashCode1() {
    assertThat(Patterns.EMPTY.hashCode()).isNotEqualTo(Patterns.NOT_ALLOWED.hashCode());
  }

  @Test
  public void testHashCode2() {
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    assertThat(p1).isNotEqualTo(p2);
    assertThat(p1.hashCode()).isNotEqualTo(p2.hashCode());
  }

  @Test
  public void testHashCode3() {
    Pattern d1 = new DummyPattern();
    Pattern d2 = new DummyPattern();
    assertThat(d1).isNotEqualTo(d2);
    assertThat(d1.hashCode()).isNotEqualTo(d2.hashCode());
  }

  @Test
  public void testHashCode4() {
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern choice = new Choice(p1, p2);
    Pattern concur = new Concur(p1, p2);
    assertThat(choice).isNotEqualTo(concur);
    assertThat(choice.hashCode()).isNotEqualTo(concur.hashCode());
  }

  @Test
  public void testHashCode5() {
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern choice1 = new Choice(p1, p2);
    Pattern choice2 = new Choice(p1, p2);
    assertThat(choice1).isEqualTo(choice2);
    assertThat(choice2.hashCode()).isEqualTo(choice2.hashCode());
  }

  @Test
  public void testHashCode6() {
    NameClass nc1 = name("name");
    NameClass nc2 = name("name");
    assertThat(nc1.hashCode()).isEqualTo(nc2.hashCode());
    assertThat(nc1).isEqualToComparingFieldByField(nc2);
    assertThat(nc1).isNotEqualTo(nc2);
    Set<NameClass> ncSet = new HashSet<>();
    ncSet.add(nc1);
    ncSet.add(nc2);
    assertThat(ncSet).hasSize(2);
  }

  class DummyPattern extends AbstractPattern {
    DummyPattern() {
      setHashcode(RANDOM.nextInt());
    }

    @Override
    void init() {
      nullable = false;
      allowsText = false;
    }
  }

}
