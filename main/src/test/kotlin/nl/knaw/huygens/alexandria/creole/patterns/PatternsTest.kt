package nl.knaw.huygens.alexandria.creole.patterns

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

import nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat
import nl.knaw.huygens.alexandria.creole.CreoleTest
import nl.knaw.huygens.alexandria.creole.NameClass
import nl.knaw.huygens.alexandria.creole.NameClasses.name
import org.junit.Test
import java.util.*

class PatternsTest : CreoleTest() {

    @Test
    fun testHashCode1() {
        Assertions.assertThat(Patterns.EMPTY.hashCode()).isNotEqualTo(Patterns.NOT_ALLOWED.hashCode())
    }

    @Test
    fun testHashCode2() {
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        assertThat(p1).isNotEqualTo(p2)
        Assertions.assertThat(p1.hashCode()).isNotEqualTo(p2.hashCode())
    }

    @Test
    fun testHashCode3() {
        val d1 = DummyPattern()
        val d2 = DummyPattern()
        assertThat(d1).isNotEqualTo(d2)
        Assertions.assertThat(d1.hashCode()).isNotEqualTo(d2.hashCode())
    }

    @Test
    fun testHashCode4() {
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val choice = Choice(p1, p2)
        val concur = Concur(p1, p2)
        assertThat(choice).isNotEqualTo(concur)
        Assertions.assertThat(choice.hashCode()).isNotEqualTo(concur.hashCode())
    }

    @Test
    fun testHashCode5() {
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val choice1 = Choice(p1, p2)
        val choice2 = Choice(p1, p2)
        assertThat(choice1).isEqualTo(choice2)
        Assertions.assertThat(choice2.hashCode()).isEqualTo(choice2.hashCode())
    }

    @Test
    fun testHashCode6() {
        val nc1 = name("name")
        val nc2 = name("name")
        Assertions.assertThat(nc1.hashCode()).isEqualTo(nc2.hashCode())
        Assertions.assertThat(nc1).isEqualToComparingFieldByField(nc2)
        Assertions.assertThat(nc1).isNotEqualTo(nc2)
        val ncSet = HashSet<NameClass>()
        ncSet.add(nc1)
        ncSet.add(nc2)
        Assertions.assertThat(ncSet).hasSize(2)
    }

    internal inner class DummyPattern : AbstractPattern() {
        init {
            setHashcode(RANDOM.nextInt())
        }

        fun init() {
            nullable = false
            allowsText = false
        }
    }

    companion object {

        private val RANDOM = Random()
    }

}
