package nl.knaw.huygens.alexandria.creole;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class ConstructorsTest extends CreoleTest {

  @Test
  public void testChoice1() {
    //    choice p NotAllowed = p
    Pattern p1 = new TestPattern();
    Pattern p2 = new Patterns.NotAllowed();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(p1);
  }

  @Test
  public void testChoice2() {
    //    choice NotAllowed p = p
    Pattern p1 = new Patterns.NotAllowed();
    Pattern p2 = new TestPattern();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(p2);
  }

  @Test
  public void testChoice3() {
    //    choice Empty Empty = Empty
    Pattern p1 = new Patterns.Empty();
    Pattern p2 = new Patterns.Empty();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isInstanceOf(Patterns.Empty.class);
  }

  @Test
  public void testChoice4() {
    //    choice p1 p2 = Choice p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(new Patterns.Choice(p1, p2));
  }
}
