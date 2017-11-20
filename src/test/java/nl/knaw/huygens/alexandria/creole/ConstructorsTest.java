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
    Pattern p2 = Patterns.notAllowed();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(p1);
  }

  @Test
  public void testChoice2() {
    //    choice NotAllowed p = p
    Pattern p1 = Patterns.notAllowed();
    Pattern p2 = new TestPattern();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(p2);
  }

  @Test
  public void testChoice3() {
    //    choice Empty Empty = Empty
    Pattern p1 = Patterns.empty();
    Pattern p2 = Patterns.empty();
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

  @Test
  public void testGroup1() {
    //  group p NotAllowed = NotAllowed
    Pattern p1 = new TestPattern();
    Pattern p2 = Patterns.notAllowed();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testGroup2() {
    //  group NotAllowed p = NotAllowed
    Pattern p1 = Patterns.notAllowed();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testGroup3() {
    //  group p Empty = p
    Pattern p1 = new TestPattern();
    Pattern p2 = Patterns.empty();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testGroup4() {
    //  group Empty p = p
    Pattern p1 = Patterns.empty();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testGroup5() {
    //  group (After p1 p2) p3 = after p1 (group p2 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    assertThat(p1).isNotEqualTo(p2);
    Pattern after = Patterns.after(p1, p2);
    Pattern p3 = new TestPattern();
    Pattern p = Constructors.group(after, p3);
    Pattern expected = Patterns.after(p1, Patterns.group(p2, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testGroup6() {
    //  group p1 (After p2 p3) = after p2 (group p1 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = Patterns.after(p2, p3);
    Pattern p = Constructors.group(p1, after);
    Pattern expected = Patterns.after(p2, Patterns.group(p1, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testGroup7() {
    //  group p1 p2 = Group p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.group(p1, p2);
    Pattern expected = Patterns.group(p1, p2);
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testInterleave1() {
    //  interleave p NotAllowed = NotAllowed
    Pattern p1 = new TestPattern();
    Pattern p2 = Patterns.notAllowed();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testInterleave2() {
    //  interleave NotAllowed p = NotAllowed
    Pattern p1 = Patterns.notAllowed();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testInterleave3() {
    //  interleave p Empty = p
    Pattern p1 = new TestPattern();
    Pattern p2 = Patterns.empty();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testInterleave4() {
    //  interleave Empty p = p
    Pattern p1 = Patterns.empty();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testInterleave5() {
    //  interleave (After p1 p2) p3 = after p1 (interleave p2 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = Patterns.after(p1, p2);
    Pattern p = Constructors.interleave(after, p3);
    Pattern expected = Patterns.after(p1, Patterns.interleave(p2, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testInterleave6() {
    //  interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = Patterns.after(p2, p3);
    Pattern p = Constructors.interleave(p1, after);
    Pattern expected = Patterns.after(p2, Patterns.interleave(p1, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testInterleave7() {
    //  interleave p1 p2 = Interleave p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(Patterns.interleave(p1,p2));
  }

  @Test
  public void testConcur1() {
    //  concur p NotAllowed = NotAllowed
    Pattern p1 = new TestPattern();
    Pattern p2 = Patterns.notAllowed();
    Pattern p = Constructors.concur(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testConcur2() {
    //  concur NotAllowed p = NotAllowed
    Pattern p1 = Patterns.notAllowed();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.concur(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testConcur3() {
    //  concur p Text = p
    Pattern p1 = new TestPattern();
    Pattern p2 = Patterns.text();
    Pattern p = Constructors.concur(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testConcur4() {
    //  concur Text p = p
    Pattern p1 = Patterns.text();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.concur(p1, p2);
    assertThat(p).isEqualTo(p2);
  }


  //TODO: make more tests




//  concur (After p1 p2) (After p3 p4) = after (all p1 p3) (concur p2 p4)
//  concur (After p1 p2) p3 = after p1 (concur p2 p3)
//  concur p1 (After p2 p3) = after p2 (concur p1 p3)
//  concur p1 p2 = Concur p1 p2

//  partition NotAllowed = NotAllowed
//  partition Empty = Empty
//  partition p = Partition p

//  oneOrMore NotAllowed = NotAllowed
//  oneOrMore Empty = Empty
//  oneOrMore p = OneOrMore p

//  concurOneOrMore NotAllowed = NotAllowed
//  concurOneOrMore Empty = Empty
//  concurOneOrMore p = ConcurOneOrMore p

//  after p NotAllowed = NotAllowed
//  after NotAllowed p = NotAllowed
//  after Empty p = p
//  after (After p1 p2) p3 = after p1 (after p2 p3)
//  after p1 p2 = After p1 p2

//  all p NotAllowed = NotAllowed
//  all NotAllowed p = NotAllowed
//  all p Empty = if nullable p then Empty else NotAllowed
//  all Empty p = if nullable p then Empty else NotAllowed
//  all (After p1 p2) (After p3 p4) = after (all p1 p3) (all p2 p4)
//  all p1 p2 = All p1 p2
}
