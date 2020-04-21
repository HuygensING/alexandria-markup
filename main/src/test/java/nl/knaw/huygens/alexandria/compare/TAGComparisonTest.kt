package nl.knaw.huygens.alexandria.compare

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter
import nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest
import nl.knaw.huygens.alexandria.storage.TAGStore
import nl.knaw.huygens.alexandria.view.TAGView
import org.junit.Ignore
import org.junit.Test
import org.slf4j.LoggerFactory

class TAGComparisonTest : AlexandriaBaseStoreTest() {

    @Ignore("unexpected additional newlines")
    @Test
    fun testSplitCase() {
        val originText = """
            :[TAGML|+M>
            :[text|M>
            :[l|M>
            :Une [del|M>[add|M>jolie<add]<del][add|M>belle<add] main de femme, élégante et fine<l] [l|M>malgré l'agrandissement du close-up.
            :<l]
            :<text]<TAGML]
            """.trimMargin(":")
        val editedText = """
            :[TAGML|+M,+N>
            :[text|M,N>
            :[l|M>
            :[s|N>Une [del|M>[add|M>jolie<add]<del][add|M>belle<add] main de femme, élégante et fine.<l]<s] [l|M>[s|N>Malgré l'agrandissement du close-up.
            :<s]
            :<l]
            :<text]<TAGML]
            """.trimMargin(":")
        val comparison = compare(originText, editedText)
        val expected: List<String> = listOf(
                " [TAGML>[text>[l>",
                "+[s>",
                " Une [del>[add>jolie<add]<del][add>belle<add] main de femme, élégante et fine",
                "+.<s]",
                " <l] [l>",
                "-malgré ",
                "+[s>Malgré ",
                " l'agrandissement du close-up.",
                "+<s]",
                " <l]",
                " <text]<TAGML]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Test
    fun testNoChanges() {
        val originText = "[quote>Any sufficiently advanced technology is indistinguishable from magic.<quote]"
        val editedText = "[quote>Any sufficiently advanced technology is indistinguishable from magic.<quote]"
        val comparison = compare(originText, editedText)
        assertThat(comparison).hasFoundNoDifference()
    }

    @Test
    fun testOmission() {
        val originText = "[quote>Any sufficiently advanced technology is indistinguishable from magic.<quote]"
        val editedText = "[quote>Any sufficiently advanced technology is magic.<quote]"
        val comparison = compare(originText, editedText)
        val expected: List<String> = listOf(
                " [quote>Any sufficiently advanced technology is ",
                "-indistinguishable from ",
                " magic.<quote]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    // @Ignore("change to TAGML first")
    @Test
    fun testAddition() {
        val originText = "[quote>Any sufficiently advanced technology is indistinguishable from magic.<quote]"
        val editedText = "[quote>Any sufficiently advanced technology is virtually indistinguishable from magic.<quote]"
        val comparison = compare(originText, editedText)
        val expected: List<String> = listOf(
                " [quote>Any sufficiently advanced technology is ",
                "+virtually ",
                " indistinguishable from magic.<quote]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Test
    fun testReplacement() {
        val originText = "[quote>Any sufficiently advanced technology is indistinguishable from magic.<quote]"
        val editedText = "[quote>Any sufficiently advanced code is indistinguishable from magic.<quote]"
        val comparison = compare(originText, editedText)
        val expected: List<String> = listOf(
                " [quote>Any sufficiently advanced ",
                "-technology ",
                "+code ",
                " is indistinguishable from magic.<quote]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Test
    fun testReplacement2() {
        val originText = "[quote>Any sufficiently advanced technology is indistinguishable from magic.<quote]"
        val editedText = "[s>Any sufficiently advanced code is indistinguishable from magic.<s]"
        val comparison = compare(originText, editedText)
        assertThat(comparison.hasDifferences()).isTrue()

        val expected: List<String> = listOf(
                "-[quote>",
                "+[s>",
                " Any sufficiently advanced ",
                "-technology ",
                "+code ",
                " is indistinguishable from magic.",
                "-<quote]",
                "+<s]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Test
    fun testJoin() {
        val originText = "[t>[l>one two<l]\n[l>three four<l]<t]"
        val editedText = "[t>[l>one two three four<l]<t]"
        val comparison = compare(originText, editedText)
        assertThat(comparison.hasDifferences()).isTrue()

        val expected: List<String> = listOf(" [t>[l>one two", "-<l]", "-[l>", " three four<l]<t]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Test
    fun testSplit() {
        val originText = "[t>[l>one two three four<l]<t]"
        val editedText = "[t>[l>one two<l]\n[l>five three four<l]<t]"
        val comparison = compare(originText, editedText)
        assertThat(comparison.hasDifferences()).isTrue()

        val expected: List<String> = listOf(" [t>[l>one two ", "+<l]", "+[l>five ", " three four<l]<t]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Ignore("unexpected additional newlines")
    @Test
    fun testAddedNewlines() {
        val originText = "[t>one two three four<t]"
        val editedText = "[t>one two three four<t]\n"
        val comparison = compare(originText, editedText)
        assertThat(comparison.hasDifferences()).isTrue()

        val expected: List<String> = listOf(" [t>one two three four<t]")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    @Ignore("[l><l] not recognized as unit of change")
    @Test
    fun testNewlinesInText() {
        val originText = "[b>[l>line 1<l]\n[l>line 2<l]\n[l>line 3<l]<b]"
        val editedText = "[b>[l>line 1<l]\n[l>line 1a<l]\n[l>line 2<l]\n[l>line 3<l]<b]"
        val comparison = compare(originText, editedText)
        assertThat(comparison.hasDifferences()).isTrue()

        val expected: List<String> = listOf(" [l>line 1<l]\n", "+[l>line 1a<l]\n", " [l>line 2<l]\n", " [l>line 3<l]\n")
        assertThat(comparison.getDiffLines()).containsExactlyElementsOf(expected)
    }

    private fun compare(originBody: String, editedBody: String): TAGComparison =
            runInStoreTransaction<TAGComparison> { store: TAGStore ->
                val originText = addTAGMLHeader(originBody)
                val editedText = addTAGMLHeader(editedBody)
                val importer = TAGMLImporter(store)
                val original = importer.importTAGML(originText)
                val edited = importer.importTAGML(editedText)
                val none: Set<String> = setOf()
                val allTags = TAGView(store).withMarkupToExclude(none)
                val comparison = TAGComparison(original, allTags, edited)
                log.info(
                        "diffLines = \n{}",
                        comparison.getDiffLines().joinToString("\n") { l: String -> "'$l'" })
                comparison
            }

    companion object {
        private val log = LoggerFactory.getLogger(TAGComparisonTest::class.java)
    }
}
