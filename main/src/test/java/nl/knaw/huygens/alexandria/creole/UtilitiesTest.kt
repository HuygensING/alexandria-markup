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
import nl.knaw.huygens.alexandria.creole.Basics.id
import nl.knaw.huygens.alexandria.creole.patterns.*
import org.junit.Test
import org.slf4j.LoggerFactory


class UtilitiesTest : CreoleTest() {

    @Test
    fun testEmptyIsNullable() {
        val p = Patterns.EMPTY
        assertThat(p).isNullable
    }

    @Test
    fun testNotAllowedIsNotNullable() {
        val p = Patterns.NOT_ALLOWED
        assertThat(p).isNotNullable
    }

    @Test
    fun testTextIsNullable() {
        val p = Patterns.TEXT
        assertThat(p).isNullable
    }

    @Test
    fun testChoiceNullability1() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = Choice(p1, p2)
        assertThat(p).isNullable
    }

    @Test
    fun testChoiceNullability2() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Choice(p1, p2)
        assertThat(p).isNullable
    }

    @Test
    fun testChoiceNullability3() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Choice(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testChoiceNullability4() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = Choice(p1, p2)
        assertThat(p).isNullable
    }

    @Test
    fun testInterleaveNullability1() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = Interleave(p1, p2)
        assertThat(p).isNullable
    }

    @Test
    fun testInterleaveNullability2() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Interleave(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testInterleaveNullability3() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = Interleave(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testInterleaveNullability4() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Interleave(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testConcurNullability1() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = Concur(p1, p2)
        assertThat(p).isNullable
    }

    @Test
    fun testConcurNullability2() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Concur(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testConcurNullability3() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = Concur(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testConcurNullability4() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Concur(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testPartitionNullability1() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p = Partition(p1)
        assertThat(p).isNullable
    }

    @Test
    fun testPartitionNullability2() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = Partition(p1)
        assertThat(p).isNotNullable
    }

    @Test
    fun testOneOrMoreNullability1() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p = OneOrMore(p1)
        assertThat(p).isNullable
    }

    @Test
    fun testOneOrMoreNullability2() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = OneOrMore(p1)
        assertThat(p).isNotNullable
    }

    @Test
    fun testConcurOneOrMoreNullability1() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p = ConcurOneOrMore(p1)
        assertThat(p).isNullable
    }

    @Test
    fun testConcurOneOrMoreNullability2() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = ConcurOneOrMore(p1)
        assertThat(p).isNotNullable
    }

    @Test
    fun testRangeIsNotNullable() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val nameClass = NameClasses.ANY_NAME
        val p = Range(nameClass, p1)
        assertThat(p).isNotNullable
    }

    @Test
    fun testEndRangeIsNotNullable() {
        val qName = Basics.qName("uri", "localName")
        val p = EndRange(qName, id("id"))
        assertThat(p).isNotNullable
    }

    @Test
    fun testAfterNullability() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = After(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testAllNullability1() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = All(p1, p2)
        assertThat(p).isNullable
    }

    @Test
    fun testAllNullability2() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = All(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testAllNullability3() {
        val p1 = CreoleTest.NOT_NULLABLE_PATTERN
        val p2 = CreoleTest.NULLABLE_PATTERN
        val p = All(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testAllNullability4() {
        val p1 = CreoleTest.NULLABLE_PATTERN
        val p2 = CreoleTest.NOT_NULLABLE_PATTERN
        val p = All(p1, p2)
        assertThat(p).isNotNullable
    }

    @Test
    fun testPatternTreeVisualisationForEmpty() {
        val emptyVisualisation = Utilities.patternTreeToDepth(Patterns.EMPTY, 1)
        Assertions.assertThat(emptyVisualisation).isEqualTo("Empty()")
    }

    @Test
    fun testPatternTreeVisualisationForNotAllowed() {
        val emptyVisualisation = Utilities.patternTreeToDepth(Patterns.NOT_ALLOWED, 1)
        Assertions.assertThat(emptyVisualisation).isEqualTo("NotAllowed()")
    }

    @Test
    fun testPatternTreeVisualisationForText() {
        val emptyVisualisation = Utilities.patternTreeToDepth(Patterns.TEXT, 1)
        Assertions.assertThat(emptyVisualisation).isEqualTo("Text()")
    }

    @Test
    fun testPatternTreeVisualisationForChoice() {
        val choice = choice(text(), empty())
        val emptyVisualisation = Utilities.patternTreeToDepth(choice, 15)
        Assertions.assertThat(emptyVisualisation).isEqualTo("Choice(\n" +
                "| Text(),\n" +
                "| Empty()\n" +
                ")")
    }

    @Test
    fun testPatternTreeVisualisationForElement() {
        val element = element("book", text())
        val visualisation = Utilities.patternTreeToDepth(element, 10)
        Assertions.assertThat(visualisation).isEqualTo("Partition(\n" +//

                "| Range(\"book\",\n" +//

                "| | Text()\n" +//

                "| )\n" +//

                ")")
    }

    @Test
    fun testFlip() {
        val p1 = CreoleTest.TestPattern()
        val p2 = CreoleTest.TestPattern()
        val pattern = group(p1, p2)

        val flipped = pattern.flip()
        LOG.info("flipped = ", Utilities.patternTreeToDepth(flipped, 10))
        Assertions.assertThat(flipped.javaClass).isEqualTo(pattern.javaClass)
        val group = pattern as Group
        val flippedGroup = flipped as Group
        assertThat(flippedGroup.pattern1).isEqualTo(group.pattern2)
        assertThat(flippedGroup.pattern2).isEqualTo(group.pattern1)
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(UtilitiesTest::class.java!!)
    }

}
