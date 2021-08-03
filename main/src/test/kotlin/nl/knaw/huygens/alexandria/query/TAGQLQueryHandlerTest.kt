package nl.knaw.huygens.alexandria.query

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import com.google.common.collect.ImmutableList
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.apache.commons.io.FileUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class TAGQLQueryHandlerTest : AlexandriaBaseStoreTest() {
    private val LOG = LoggerFactory.getLogger(javaClass)

    @Disabled
    @Test
    @Throws(LMNLSyntaxError::class)
    fun testTAGQLQuery1() {
        val lmnl = """
      [excerpt}[p}
      Alice was beginning to get very tired of sitting by her sister on the bank,
      and of having nothing to do: once or twice she had peeped into the book her sister
      was reading, but it had no pictures or conversations in it, 
      [q [n}a{]}and what is the use of a book,{q]
      thought Alice
      [q [n}a{]}without pictures or conversation?{q]
      {p]{excerpt]
      """.trimIndent()
        runInStoreTransaction { store: TAGStore? ->
            val alice = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(alice)
            // String statement = "select m.text from markup m where m.name='q' and m.id='a'";
            // String statement = "select m.text from markup m where m.name='q=a'";
            val statement = "select text from markup where name='q'"
            val result = h.execute(statement)
            LOG.info("result={}", result)
            assertQuerySucceeded(result)
            val expected: MutableList<String?> = ArrayList()
            expected.add("and what is the use of a book,without pictures or conversation?")
            assertThat(result.getValues()).containsExactlyElementsOf(expected)
        }
    }

    @Disabled
    @Test
    @Throws(LMNLSyntaxError::class)
    fun testTAGQLQuery2() {
        val lmnl = """
      [text}
      [l}line 1{l]
      [l}line 2{l]
      [l}line 3{l]
      {text]
      """.trimIndent()
        runInStoreTransaction { store: TAGStore? ->
            val alice = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(alice)
            val statement1 = "select text from markup('l')[0]"
            val result1 = h.execute(statement1)
            LOG.info("result1.getValues()={}", result1.getValues())
            assertQuerySucceeded(result1)
            val expected1: MutableList<String?> = ArrayList()
            expected1.add("line 1")
            assertThat(result1.getValues()).containsExactlyElementsOf(expected1)
            val statement2 = "select text from markup('l')[2]"
            val result2 = h.execute(statement2)
            LOG.info("result2.getValues()={}", result2.getValues())
            assertQuerySucceeded(result2)
            val expected2: MutableList<String?> = ArrayList()
            expected2.add("line 3")
            assertThat(result2.getValues()).containsExactlyElementsOf(expected2)
            val statement3 = "select text from markup('l')"
            val result3 = h.execute(statement3)
            LOG.info("result3.getValues()={}", result3.getValues())
            assertQuerySucceeded(result3)
            val expected3: MutableList<String?> = ArrayList()
            expected3.add("line 1")
            expected3.add("line 2")
            expected3.add("line 3")
            assertThat(result3.getValues()).containsExactlyElementsOf(expected3)
        }
    }

    @Disabled
    @Test
    @Throws(LMNLSyntaxError::class)
    fun testTAGQLQuery3() {
        val lmnl = """
      [excerpt [source [book}1 Kings{book] [chapter}12{chapter]]}
      [verse}And he said unto them, [q}What counsel give ye that we may answer this people, who have spoken to me, saying, [q}Make the yoke which thy father did put upon us lighter?{q]{q]{verse]
      [verse}And the young men that were grown up with him spake unto him, saying, [q}Thus shalt thou speak unto this people that spake unto thee, saying, [q=i}Thy father made our yoke heavy, but make thou it lighter unto us;{q=i] thus shalt thou say unto them, [q=j}My little finger shall be thicker than my father's loins.{verse]
      [verse}And now whereas my father did lade you with a heavy yoke, I will add to your yoke: my father hath chastised you with whips, but I will chastise you with scorpions.{q=j]{q]{verse]
      [verse}So Jeroboam and all the people came to Rehoboam the third day, as the king had appointed, saying, [q}Come to me again the third day.{q]{verse]
      {excerpt]
      """.trimIndent()
        runInStoreTransaction { store: TAGStore? ->
            val kings = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(kings)
            val statement1 = "select annotationText('source:chapter') from markup where name='excerpt'"
            val result1 = h.execute(statement1)
            LOG.info("result1={}", result1)
            assertQuerySucceeded(result1)
            val expected1: MutableList<MutableList<String?>?> = ArrayList()
            val expectedEntry: MutableList<String?> = ArrayList()
            expectedEntry.add("12")
            expected1.add(expectedEntry)
            assertThat(result1.getValues()).containsExactlyElementsOf(expected1)
        }
    }

    @Disabled
    @Test
    @Throws(LMNLSyntaxError::class)
    fun testTAGQLQuery4() {
        val lmnl = """
      [text}
      [l [type}A{]}line 1{l]
      [l [type}B{]}line 2{l]
      [l [type}A{]}line 3{l]
      {text]
      """.trimIndent()
        runInStoreTransaction { store: TAGStore? ->
            val alice = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(alice)
            val statement1 = "select m.text from markup m where m.name='l' and m.annotationText('type')='A'"
            val result1 = h.execute(statement1)
            LOG.info("result1.getValues()={}", result1.getValues())
            assertQuerySucceeded(result1)
            val expected1: MutableList<String?>? = ImmutableList.of("line 1", "line 3")
            assertThat(result1.getValues())
                .containsExactlyElementsOf(expected1)
            val statement2 = "select m.text from markup m where m.name='l' and m.annotationText('type')='B'"
            val result2 = h.execute(statement2)
            LOG.info("result2.getValues()={}", result2.getValues())
            assertQuerySucceeded(result2)
            val expected2: MutableList<String?>? = ImmutableList.of("line 2")
            assertThat(result2.getValues())
                .containsExactlyElementsOf(expected2)
        }
    }

    @Disabled
    @Test
    @Throws(IOException::class, LMNLSyntaxError::class)
    fun testLuminescentQuery1() {
        val lmnl = FileUtils.readFileToString(File("data/lmnl/frankenstein.lmnl"), "UTF-8")
        runInStoreTransaction { store: TAGStore? ->
            val frankenstein = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(frankenstein)
            val statement1 = "select annotationText('n') from markup where name='page' and text contains 'Volney'"
            val result1 = h.execute(statement1)
            LOG.info("result1={}", result1)
            assertQuerySucceeded(result1)
            val expected1: MutableList<MutableList<String?>?> = ArrayList()
            val expectedEntry: MutableList<String?> = ArrayList()
            expectedEntry.add("102")
            expected1.add(expectedEntry)
            assertThat(result1.getValues()).containsExactlyElementsOf(expected1)
        }
    }

    @Disabled
    @Test
    @Throws(IOException::class, LMNLSyntaxError::class)
    fun testLuminescentQuery2() {
        val lmnl = FileUtils.readFileToString(File("data/lmnl/frankenstein.lmnl"), "UTF-8")
        runInStoreTransaction { store: TAGStore? ->
            val frankenstein = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(frankenstein)
            val statement1 = "select distinct(annotationText('who')) from markup where name='said'"
            val result1 = h.execute(statement1)
            LOG.info("result1={}", result1)
            assertQuerySucceeded(result1)
            val expected1: MutableList<MutableList<String?>?> = ArrayList()
            val expectedEntry: MutableList<String?> = ArrayList()
            expectedEntry.add("Creature")
            expected1.add(expectedEntry)
            assertThat(result1.getValues()).containsExactlyElementsOf(expected1)
        }
    }

    @Test
    @Throws(LMNLSyntaxError::class)
    fun testTAGQLQueryWithSyntaxError() {
        val lmnl = """
          [text}
          [l}line 1{l]
          [l}line 2{l]
          [l}line 3{l]
          {text]
          """.trimIndent()
        runInStoreTransaction { store: TAGStore ->
            val doc = LMNLImporter(store).importLMNL(lmnl)
            val h = TAGQLQueryHandler(doc)
            val statement1 = "select tekst from murkap"
            val result1 = h.execute(statement1)
            LOG.info("result1={}", result1)
            assertThat(result1.isOk()).isFalse()
        }
    }

    private fun assertQuerySucceeded(result: TAGQLResult) {
        assertThat(result).isNotNull
        assertThat(result.isOk()).isTrue()
    }
}
