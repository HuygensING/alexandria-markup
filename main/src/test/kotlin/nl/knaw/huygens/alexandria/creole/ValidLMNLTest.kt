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
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter2
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError
import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.assertj.core.api.Assertions
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

@RunWith(Parameterized::class)
class ValidLMNLTest(private val basename: String) : CreoleTest() {

    @Ignore
    @Test
    @Throws(IOException::class, LMNLSyntaxError::class)
    fun testCreoleFile() {
        LOG.info("validating {}.lmnl against {}.creole", basename, basename)
        validateLMNL(basename)
        LOG.info("done validating {}.lmnl against {}.creole", basename, basename)
    }

    @Throws(IOException::class)
    private fun validateLMNL(basename: String) {
        val xml = FileUtils.readFileToString(File("$ROOTDIR$basename.creole"), "UTF-8")
        Assertions.assertThat(xml).isNotEmpty()
        LOG.info("testing {}.creole", basename)
        LOG.info("creole=\n{}", xml)
        val schema = SchemaImporter.fromXML(xml)
        assertThat(schema).isNotNull

        val lmnl = FileUtils.readFileToString(File("$LMNL_DIR$basename.lmnl"), "UTF-8")
        LOG.info("lmnl=\n{}", lmnl)
        val events = LMNLImporter2().importLMNL(lmnl)
        val validator = Validator.ofPattern(schema)
        val result = validator.validate(events)
        assertThat(result).isSuccess
    }

    companion object {
        private val ROOTDIR = "src/test/resources/"
        private val LMNL_DIR = ROOTDIR + "valid/"
        private val LOG = LoggerFactory.getLogger(ValidLMNLTest::class.java)

        private val LMNL_FILE_FILTER = object : IOFileFilter {
            override fun accept(file: File): Boolean {
                return isLMNL(file.name)
            }

            override fun accept(dir: File, name: String): Boolean {
                return isLMNL(name)
            }

            private fun isLMNL(name: String): Boolean {
                return name.endsWith(".lmnl")
            }
        }

        @Parameterized.Parameters
        fun parameters(): Collection<Array<String>> {
            return FileUtils.listFiles(File(LMNL_DIR), LMNL_FILE_FILTER, null)
                    .map({ it.name })
                    .map { it.replace(".lmnl", "", false) }
                    .map { b -> arrayOf(b) }
        }
    }

}
