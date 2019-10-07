package nl.knaw.huygens.alexandria.creole;

/*
 * #%L
 * alexandria-markup
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

import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import nl.knaw.huygens.alexandria.creole.patterns.*;
import static nl.knaw.huygens.alexandria.creole.patterns.Patterns.EMPTY;
import static nl.knaw.huygens.alexandria.creole.patterns.Patterns.NOT_ALLOWED;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

public class ConstructorsTest extends CreoleTest {

  @Test
  public void testChoice1() {
    //    choice p NotAllowed = p
    Pattern p1 = new TestPattern();
    Pattern p2 = notAllowed();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(p1);
  }

  @Test
  public void testChoice2() {
    //    choice NotAllowed p = p
    Pattern p1 = notAllowed();
    Pattern p2 = new TestPattern();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(p2);
  }

  @Test
  public void testChoice3() {
    //    choice Empty Empty = Empty
    Pattern p1 = empty();
    Pattern p2 = empty();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isInstanceOf(Empty.class);
  }

  @Test
  public void testChoice4() {
    //    choice p1 p2 = Choice p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern choice = Constructors.choice(p1, p2);
    assertThat(choice).isEqualTo(new Choice(p1, p2));
  }

  @Test
  public void testGroup1() {
    //  group p NotAllowed = NotAllowed
    Pattern p1 = new TestPattern();
    Pattern p2 = notAllowed();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testGroup2() {
    //  group NotAllowed p = NotAllowed
    Pattern p1 = notAllowed();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testGroup3() {
    //  group p Empty = p
    Pattern p1 = new TestPattern();
    Pattern p2 = empty();
    Pattern p = Constructors.group(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testGroup4() {
    //  group Empty p = p
    Pattern p1 = empty();
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
    Pattern after = new After(p1, p2);
    Pattern p3 = new TestPattern();
    Pattern p = Constructors.group(after, p3);
    Pattern expected = new After(p1, new Group(p2, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testGroup6() {
    //  group p1 (After p2 p3) = after p2 (group p1 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = new After(p2, p3);
    Pattern p = Constructors.group(p1, after);
    Pattern expected = new After(p2, new Group(p1, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testGroup7() {
    //  group p1 p2 = Group p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.group(p1, p2);
    Pattern expected = new Group(p1, p2);
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testInterleave1() {
    //  interleave p NotAllowed = NotAllowed
    Pattern p1 = new TestPattern();
    Pattern p2 = notAllowed();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testInterleave2() {
    //  interleave NotAllowed p = NotAllowed
    Pattern p1 = notAllowed();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testInterleave3() {
    //  interleave p Empty = p
    Pattern p1 = new TestPattern();
    Pattern p2 = empty();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testInterleave4() {
    //  interleave Empty p = p
    Pattern p1 = empty();
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
    Pattern after = new After(p1, p2);
    Pattern p = Constructors.interleave(after, p3);
    Pattern expected = new After(p1, new Interleave(p2, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testInterleave6() {
    //  interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = new After(p2, p3);
    Pattern p = Constructors.interleave(p1, after);
    Pattern expected = new After(p2, new Interleave(p1, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testInterleave7() {
    //  interleave p1 p2 = Interleave p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p = Constructors.interleave(p1, p2);
    assertThat(p).isEqualTo(new Interleave(p1, p2));
  }

  @Test
  public void testConcur1() {
    //  concur p NotAllowed = NotAllowed
    Pattern p1 = new TestPattern();
    Pattern p2 = notAllowed();
    Pattern p = concur(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testConcur2() {
    //  concur NotAllowed p = NotAllowed
    Pattern p1 = notAllowed();
    Pattern p2 = new TestPattern();
    Pattern p = concur(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testConcur3() {
    //  concur p Text = p
    Pattern p1 = new TestPattern();
    Pattern p2 = new Text();
    Pattern p = concur(p1, p2);
    assertThat(p).isEqualTo(p1);
  }

  @Test
  public void testConcur4() {
    //  concur Text p = p
    Pattern p1 = new Text();
    Pattern p2 = new TestPattern();
    Pattern p = concur(p1, p2);
    assertThat(p).isEqualTo(p2);
  }

  @Test
  public void testConcur5() {
    //  concur (After p1 p2) (After p3 p4) = after (all p1 p3) (concur p2 p4)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern p4 = new TestPattern();
    Pattern after1 = new After(p1, p2);
    Pattern after2 = new After(p3, p4);
    Pattern p = concur(after1, after2);
    Pattern expected = new After(new All(p1, p3), new Concur(p2, p4));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testConcur6() {
    //  concur (After p1 p2) p3 = after p1 (concur p2 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = new After(p1, p2);
    Pattern p = concur(after, p3);
    Pattern expected = new After(p1, new Concur(p2, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testConcur7() {
    //  concur p1 (After p2 p3) = after p2 (concur p1 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = new After(p2, p3);
    Pattern p = concur(p1, after);
    Pattern expected = new After(p2, new Concur(p1, p3));
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testConcur8() {
    //  concur p1 p2 = Concur p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p = concur(p1, p2);
    Pattern expected = new Concur(p1, p2);
    assertThat(p).isEqualTo(expected);
  }

  @Test
  public void testPartition1() {
    //  partition NotAllowed = NotAllowed
    Pattern p = partition(notAllowed());
    assertThat(p).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testPartition2() {
    //  partition Empty = Empty
    Pattern p = partition(empty());
    assertThat(p).isEqualTo(EMPTY);
  }

  @Test
  public void testPartition3() {
    //  partition p = Partition p
    Pattern p = new TestPattern();
    Pattern partition = partition(p);
    assertThat(partition).isEqualToComparingFieldByField(new Partition(p));
  }

  @Test
  public void testOneOrMore1() {
    //  oneOrMore NotAllowed = NotAllowed
    Pattern p = oneOrMore(notAllowed());
    assertThat(p).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testOneOrMore2() {
    //  oneOrMore Empty = Empty
    Pattern p = oneOrMore(empty());
    assertThat(p).isEqualTo(EMPTY);
  }

  @Test
  public void testOneOrMore3() {
    //  oneOrMore p = OneOrMore p
    Pattern p = new TestPattern();
    Pattern oneOrMore = oneOrMore(p);
    assertThat(oneOrMore).isEqualToComparingFieldByField(new OneOrMore(p));
  }

  @Test
  public void testConcurOneOrMore1() {
    //  concurOneOrMore NotAllowed = NotAllowed
    Pattern p = concurOneOrMore(notAllowed());
    assertThat(p).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testConcurOneOrMore2() {
    //  concurOneOrMore Empty = Empty
    Pattern p = concurOneOrMore(empty());
    assertThat(p).isEqualTo(EMPTY);
  }

  @Test
  public void testConcurOneOrMore3() {
    //  concurOneOrMore p = ConcurOneOrMore p
    Pattern p = new TestPattern();
    Pattern concurOneOrMore = concurOneOrMore(p);
    assertThat(concurOneOrMore).isEqualToComparingFieldByField(new ConcurOneOrMore(p));
  }

  @Test
  public void testAfter1() {
    //  after p NotAllowed = NotAllowed
    Pattern p = new TestPattern();
    Pattern after = after(p, notAllowed());
    assertThat(after).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testAfter2() {
    //  after NotAllowed p = NotAllowed
    Pattern p = new TestPattern();
    Pattern after = after(notAllowed(), p);
    assertThat(after).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testAfter3() {
    //  after Empty p = p
    Pattern p = new TestPattern();
    Pattern after = after(empty(), p);
    assertThat(after).isEqualTo(p);
  }

  @Test
  public void testAfter4() {
    //  after (After p1 p2) p3 = after p1 (after p2 p3)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern after = new After(p1, p2);
    Pattern p = after(after, p3);
    assertThat(p).isEqualTo(after(p1, after(p2, p3)));
  }

  @Test
  public void testAfter5() {
    //  after p1 p2 = After p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p = after(p1, p2);
    assertThat(p).isEqualTo(new After(p1, p2));
  }

  @Test
  public void testAll1() {
    //  all p NotAllowed = NotAllowed
    Pattern p = new TestPattern();
    Pattern all = all(p, notAllowed());
    assertThat(all).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testAll2() {
    //  all NotAllowed p = NotAllowed
    Pattern p = new TestPattern();
    Pattern all = all(notAllowed(), p);
    assertThat(all).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testAll3a() {
    //  all p Empty = if nullable p then Empty else NotAllowed
    Pattern p = new NullablePattern();
    Pattern all = all(p, empty());
    assertThat(all).isEqualTo(EMPTY);
  }

  @Test
  public void testAll3b() {
    //  all p Empty = if nullable p then Empty else NotAllowed
    Pattern p = new NotNullablePattern();
    Pattern all = all(p, empty());
    assertThat(all).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testAll4a() {
    //  all Empty p  = if nullable p then Empty else NotAllowed
    Pattern p = new NullablePattern();
    Pattern all = all(empty(), p);
    assertThat(all).isEqualTo(EMPTY);
  }


  @Test
  public void testAll4b() {
    //  all Empty p = if nullable p then Empty else NotAllowed
    Pattern p = new NotNullablePattern();
    Pattern all = all(empty(), p);
    assertThat(all).isEqualTo(NOT_ALLOWED);
  }

  @Test
  public void testAll5() {
    //  all (After p1 p2) (After p3 p4) = after (all p1 p3) (all p2 p4)
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern p3 = new TestPattern();
    Pattern p4 = new TestPattern();
    Pattern after1 = new After(p1, p2);
    Pattern after2 = new After(p3, p4);
    Pattern all = all(after1, after2);
    assertThat(all).isEqualTo(after(all(p1, p3), all(p2, p4)));
  }

  @Test
  public void testAll6() {
    //  all p1 p2 = All p1 p2
    Pattern p1 = new TestPattern();
    Pattern p2 = new TestPattern();
    Pattern all = all(p1, p2);
    assertThat(all).isEqualTo(new All(p1, p2));
  }

}
