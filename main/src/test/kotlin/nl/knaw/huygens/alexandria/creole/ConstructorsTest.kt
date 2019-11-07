package nl.knaw.huygens.alexandria.creole

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

import nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat
import nl.knaw.huygens.alexandria.creole.Constructors.after
import nl.knaw.huygens.alexandria.creole.Constructors.all
import nl.knaw.huygens.alexandria.creole.Constructors.concur
import nl.knaw.huygens.alexandria.creole.Constructors.concurOneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.empty
import nl.knaw.huygens.alexandria.creole.Constructors.notAllowed
import nl.knaw.huygens.alexandria.creole.Constructors.oneOrMore
import nl.knaw.huygens.alexandria.creole.Constructors.partition
import nl.knaw.huygens.alexandria.creole.patterns.*
import nl.knaw.huygens.alexandria.creole.patterns.Patterns.EMPTY
import nl.knaw.huygens.alexandria.creole.patterns.Patterns.NOT_ALLOWED
import org.junit.Test

class ConstructorsTest : CreoleTest() {

    @Test
    fun testChoice1() {
        //    choice p NotAllowed = p
        val p1 = CreoleTest.TestPattern()
        val p2 = notAllowed()
        val choice = Constructors.choice(p1, p2)
        assertThat(choice).isEqualTo(p1)
    }

    @Test
    fun testChoice2() {
        //    choice NotAllowed p = p
        val p1 = notAllowed()
        val p2 = CreoleTest.TestPattern()
        val choice = Constructors.choice(p1, p2)
        assertThat(choice).isEqualTo(p2)
    }

    @Test
    fun testChoice3() {
        //    choice Empty Empty = Empty
        val p1 = empty()
        val p2 = empty()
        val choice = Constructors.choice(p1, p2)
        assertThat(choice).isInstanceOf(Empty::class.java)
    }

    @Test
    fun testChoice4() {
        //    choice p1 p2 = Choice p1 p2
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val choice = Constructors.choice(p1, p2)
        assertThat(choice).isEqualTo(Choice(p1, p2))
    }

    @Test
    fun testGroup1() {
        //  group p NotAllowed = NotAllowed
        val p1 = CreoleTest.TestPattern()
        val p2 = notAllowed()
        val p = Constructors.group(p1, p2)
        assertThat(p).isEqualTo(p2)
    }

    @Test
    fun testGroup2() {
        //  group NotAllowed p = NotAllowed
        val p1 = notAllowed()
        val p2 = CreoleTest.TestPattern()
        val p = Constructors.group(p1, p2)
        assertThat(p).isEqualTo(p1)
    }

    @Test
    fun testGroup3() {
        //  group p Empty = p
        val p1 = CreoleTest.TestPattern()
        val p2 = empty()
        val p = Constructors.group(p1, p2)
        assertThat(p).isEqualTo(p1)
    }

    @Test
    fun testGroup4() {
        //  group Empty p = p
        val p1 = empty()
        val p2 = CreoleTest.TestPattern()
        val p = Constructors.group(p1, p2)
        assertThat(p).isEqualTo(p2)
    }

    @Test
    fun testGroup5() {
        //  group (After p1 p2) p3 = after p1 (group p2 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        assertThat(p1).isNotEqualTo(p2)
        val after = After(p1, p2)
        val p3 = CreoleTest.TestPattern()
        val p = Constructors.group(after, p3)
        val expected = After(p1, Group(p2, p3))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testGroup6() {
        //  group p1 (After p2 p3) = after p2 (group p1 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val after = After(p2, p3)
        val p = Constructors.group(p1, after)
        val expected = After(p2, Group(p1, p3))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testGroup7() {
        //  group p1 p2 = Group p1 p2
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p = Constructors.group(p1, p2)
        val expected = Group(p1, p2)
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testInterleave1() {
        //  interleave p NotAllowed = NotAllowed
        val p1 = CreoleTest.TestPattern()
        val p2 = notAllowed()
        val p = Constructors.interleave(p1, p2)
        assertThat(p).isEqualTo(p2)
    }

    @Test
    fun testInterleave2() {
        //  interleave NotAllowed p = NotAllowed
        val p1 = notAllowed()
        val p2 = CreoleTest.TestPattern()
        val p = Constructors.interleave(p1, p2)
        assertThat(p).isEqualTo(p1)
    }

    @Test
    fun testInterleave3() {
        //  interleave p Empty = p
        val p1 = CreoleTest.TestPattern()
        val p2 = empty()
        val p = Constructors.interleave(p1, p2)
        assertThat(p).isEqualTo(p1)
    }

    @Test
    fun testInterleave4() {
        //  interleave Empty p = p
        val p1 = empty()
        val p2 = CreoleTest.TestPattern()
        val p = Constructors.interleave(p1, p2)
        assertThat(p).isEqualTo(p2)
    }

    @Test
    fun testInterleave5() {
        //  interleave (After p1 p2) p3 = after p1 (interleave p2 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val after = After(p1, p2)
        val p = Constructors.interleave(after, p3)
        val expected = After(p1, Interleave(p2, p3))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testInterleave6() {
        //  interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val after = After(p2, p3)
        val p = Constructors.interleave(p1, after)
        val expected = After(p2, Interleave(p1, p3))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testInterleave7() {
        //  interleave p1 p2 = Interleave p1 p2
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p = Constructors.interleave(p1, p2)
        assertThat(p).isEqualTo(Interleave(p1, p2))
    }

    @Test
    fun testConcur1() {
        //  concur p NotAllowed = NotAllowed
        val p1 = CreoleTest.TestPattern()
        val p2 = notAllowed()
        val p = concur(p1, p2)
        assertThat(p).isEqualTo(p2)
    }

    @Test
    fun testConcur2() {
        //  concur NotAllowed p = NotAllowed
        val p1 = notAllowed()
        val p2 = CreoleTest.TestPattern()
        val p = concur(p1, p2)
        assertThat(p).isEqualTo(p1)
    }

    @Test
    fun testConcur3() {
        //  concur p Text = p
        val p1 = CreoleTest.TestPattern()
        val p2 = Text()
        val p = concur(p1, p2)
        assertThat(p).isEqualTo(p1)
    }

    @Test
    fun testConcur4() {
        //  concur Text p = p
        val p1 = Text()
        val p2 = CreoleTest.TestPattern()
        val p = concur(p1, p2)
        assertThat(p).isEqualTo(p2)
    }

    @Test
    fun testConcur5() {
        //  concur (After p1 p2) (After p3 p4) = after (all p1 p3) (concur p2 p4)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val p4 = CreoleTest.TestPattern()
        val after1 = After(p1, p2)
        val after2 = After(p3, p4)
        val p = concur(after1, after2)
        val expected = After(All(p1, p3), Concur(p2, p4))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testConcur6() {
        //  concur (After p1 p2) p3 = after p1 (concur p2 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val after = After(p1, p2)
        val p = concur(after, p3)
        val expected = After(p1, Concur(p2, p3))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testConcur7() {
        //  concur p1 (After p2 p3) = after p2 (concur p1 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val after = After(p2, p3)
        val p = concur(p1, after)
        val expected = After(p2, Concur(p1, p3))
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testConcur8() {
        //  concur p1 p2 = Concur p1 p2
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p = concur(p1, p2)
        val expected = Concur(p1, p2)
        assertThat(p).isEqualTo(expected)
    }

    @Test
    fun testPartition1() {
        //  partition NotAllowed = NotAllowed
        val p = partition(notAllowed())
        assertThat(p).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testPartition2() {
        //  partition Empty = Empty
        val p = partition(empty())
        assertThat(p).isEqualTo(EMPTY)
    }

    @Test
    fun testPartition3() {
        //  partition p = Partition p
        val p = CreoleTest.TestPattern()
        val partition = partition(p)
//        assertThat(partition).isEqualToComparingFieldByField(Partition(p))
        assertThat(partition).isEqualTo(Partition(p))
    }

    @Test
    fun testOneOrMore1() {
        //  oneOrMore NotAllowed = NotAllowed
        val p = oneOrMore(notAllowed())
        assertThat(p).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testOneOrMore2() {
        //  oneOrMore Empty = Empty
        val p = oneOrMore(empty())
        assertThat(p).isEqualTo(EMPTY)
    }

    @Test
    fun testOneOrMore3() {
        //  oneOrMore p = OneOrMore p
        val p = CreoleTest.TestPattern()
        val oneOrMore = oneOrMore(p)
//        assertThat(oneOrMore).isEqualToComparingFieldByField(OneOrMore(p))
        assertThat(oneOrMore).isEqualTo(OneOrMore(p))
    }

    @Test
    fun testConcurOneOrMore1() {
        //  concurOneOrMore NotAllowed = NotAllowed
        val p = concurOneOrMore(notAllowed())
        assertThat(p).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testConcurOneOrMore2() {
        //  concurOneOrMore Empty = Empty
        val p = concurOneOrMore(empty())
        assertThat(p).isEqualTo(EMPTY)
    }

    @Test
    fun testConcurOneOrMore3() {
        //  concurOneOrMore p = ConcurOneOrMore p
        val p = CreoleTest.TestPattern()
        val concurOneOrMore = concurOneOrMore(p)
//        assertThat(concurOneOrMore).isEqualToComparingFieldByField(ConcurOneOrMore(p))
        assertThat(concurOneOrMore).isEqualTo(ConcurOneOrMore(p))
    }

    @Test
    fun testAfter1() {
        //  after p NotAllowed = NotAllowed
        val p = CreoleTest.TestPattern()
        val after = after(p, notAllowed())
        assertThat(after).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testAfter2() {
        //  after NotAllowed p = NotAllowed
        val p = CreoleTest.TestPattern()
        val after = after(notAllowed(), p)
        assertThat(after).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testAfter3() {
        //  after Empty p = p
        val p = CreoleTest.TestPattern()
        val after = after(empty(), p)
        assertThat(after).isEqualTo(p)
    }

    @Test
    fun testAfter4() {
        //  after (After p1 p2) p3 = after p1 (after p2 p3)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val after = After(p1, p2)
        val p = after(after, p3)
        assertThat(p).isEqualTo(after(p1, after(p2, p3)))
    }

    @Test
    fun testAfter5() {
        //  after p1 p2 = After p1 p2
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p = after(p1, p2)
        assertThat(p).isEqualTo(After(p1, p2))
    }

    @Test
    fun testAll1() {
        //  all p NotAllowed = NotAllowed
        val p = CreoleTest.TestPattern()
        val all = all(p, notAllowed())
        assertThat(all).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testAll2() {
        //  all NotAllowed p = NotAllowed
        val p = CreoleTest.TestPattern()
        val all = all(notAllowed(), p)
        assertThat(all).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testAll3a() {
        //  all p Empty = if nullable p then Empty else NotAllowed
        val p = CreoleTest.NullablePattern()
        val all = all(p, empty())
        assertThat(all).isEqualTo(EMPTY)
    }

    @Test
    fun testAll3b() {
        //  all p Empty = if nullable p then Empty else NotAllowed
        val p = CreoleTest.NotNullablePattern()
        val all = all(p, empty())
        assertThat(all).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testAll4a() {
        //  all Empty p  = if nullable p then Empty else NotAllowed
        val p = CreoleTest.NullablePattern()
        val all = all(empty(), p)
        assertThat(all).isEqualTo(EMPTY)
    }


    @Test
    fun testAll4b() {
        //  all Empty p = if nullable p then Empty else NotAllowed
        val p = CreoleTest.NotNullablePattern()
        val all = all(empty(), p)
        assertThat(all).isEqualTo(NOT_ALLOWED)
    }

    @Test
    fun testAll5() {
        //  all (After p1 p2) (After p3 p4) = after (all p1 p3) (all p2 p4)
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val p3 = CreoleTest.TestPattern()
        val p4 = CreoleTest.TestPattern()
        val after1 = After(p1, p2)
        val after2 = After(p3, p4)
        val all = all(after1, after2)
        assertThat(all).isEqualTo(after(all(p1, p3), all(p2, p4)))
    }

    @Test
    fun testAll6() {
        //  all p1 p2 = All p1 p2
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val all = all(p1, p2)
        assertThat(all).isEqualTo(All(p1, p2))
    }

}
