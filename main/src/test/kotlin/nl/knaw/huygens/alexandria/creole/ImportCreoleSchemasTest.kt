package nl.knaw.huygens.alexandria.creole

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
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

@RunWith(Parameterized::class)
class ImportCreoleSchemasTest(private val basename: String) : CreoleTest() {

    @Test
    @Throws(IOException::class, LMNLSyntaxError::class)
    fun testCreoleFile() {
        LOG.info("testing {}.creole", basename)
        processCreoleFile(basename)
        LOG.info("done testing {}.creole", basename)
    }

    @Throws(IOException::class)
    private fun processCreoleFile(basename: String) {
        val xml = FileUtils.readFileToString(File("$ROOTDIR$basename.creole"), "UTF-8")
        Assertions.assertThat(xml).isNotEmpty()
        LOG.info("testing {}.creole", basename)
        LOG.info("{}", xml)
        val schema = SchemaImporter.fromXML(xml)
        assertThat(schema).isNotNull
    }

    companion object {
        private val ROOTDIR = "src/test/resources/"
        private val LOG = LoggerFactory.getLogger(ImportCreoleSchemasTest::class.java)

        private val CREOLE_FILE_FILTER = object : IOFileFilter {
            override fun accept(file: File): Boolean {
                return isCreoleXML(file.name)
            }

            override fun accept(dir: File, name: String): Boolean {
                return isCreoleXML(name)
            }

            private fun isCreoleXML(name: String): Boolean {
                return name.endsWith(".creole")
            }
        }

        @JvmStatic
        @Parameterized.Parameters
        fun parameters(): Collection<Array<String>> {
            return FileUtils.listFiles(File(ROOTDIR), CREOLE_FILE_FILTER, null)
                    .map { it.name }
                    .map { n -> n.replace(".creole", "",true) }
                    .map { b -> arrayOf(b) }
        }
    }


}
