package nl.knaw.huc.di.tag.tagml.importer

/*
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

import nl.knaw.huc.di.tag.TAGBaseStoreTest
import nl.knaw.huygens.alexandria.exporter.LaTeXExporter
import nl.knaw.huygens.alexandria.storage.TAGDocument
import nl.knaw.huygens.alexandria.storage.TAGStore
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.commons.io.filefilter.IOFileFilter
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.UncheckedIOException
import java.nio.charset.Charset
import java.util.stream.Stream

class ImportDataTAGMLTest : TAGBaseStoreTest() {

    @ParameterizedTest(name = "[{index}] testing data/tagml/{0}.tagml")
    @ArgumentsSource(CustomArgumentsProvider::class)
//    @Throws(TAGMLSyntaxError::class)
    fun testTAGMLFile(basename: String) {
        LOG.info("testing data/tagml/{}.tagml", basename)
        processTAGMLFile(basename)
        LOG.info("done testing data/tagml/{}.tagml", basename)
    }

    //    @Throws(TAGMLSyntaxError::class)
    private fun processTAGMLFile(basename: String) {
        runInStoreTransaction { store: TAGStore? ->
            try {
                var input = getInputStream(basename)
                val lines = IOUtils.readLines(input, Charset.defaultCharset())
                LOG.info("\nTAGML:\n{}\n", java.lang.String.join("\n", lines))
                input = getInputStream(basename)
                printTokens(input)
                input = getInputStream(basename)
                LOG.info("testing data/tagml/{}.tagml", basename)
                LOG.info("importTAGML\n")
                val document = TAGMLImporter(store!!).importTAGML(input)
                logDocumentGraph(document, "")
                //        generateLaTeX(basename, document);
            } catch (e: IOException) {
                e.printStackTrace()
                throw UncheckedIOException(e)
            }
        }
    }

    @Throws(IOException::class)
    private fun getInputStream(basename: String): InputStream {
        return FileUtils.openInputStream(File("data/tagml/$basename.tagml"))
    }

    @Throws(IOException::class)
    private fun generateLaTeX(basename: String, document: TAGDocument, store: TAGStore) {
        val exporter = LaTeXExporter(store, document)
        val outDir = "out/"
        val laTeX = exporter.exportDocument()
        Assertions.assertThat(laTeX).isNotBlank()
        // LOG.info("document=\n{}", laTeX);
        FileUtils.writeStringToFile(File("$outDir$basename.tex"), laTeX, "UTF-8")
        val overlap = exporter.exportGradient()
        Assertions.assertThat(overlap).isNotBlank()
        // LOG.info("overlap=\n{}", overlap);
        FileUtils.writeStringToFile(File("$outDir$basename-gradient.tex"), overlap, "UTF-8")
        val coloredText = exporter.exportMarkupOverlap()
        Assertions.assertThat(coloredText).isNotBlank()
        FileUtils.writeStringToFile(File("$outDir$basename-colored-text.tex"), coloredText, "UTF-8")
        val matrix = exporter.exportMatrix()
        Assertions.assertThat(matrix).isNotBlank()
        // LOG.info("matrix=\n{}", laTeX);
        FileUtils.writeStringToFile(File("$outDir$basename-matrix.tex"), matrix, "UTF-8")
        val kdTree = exporter.exportKdTree()
        Assertions.assertThat(kdTree).isNotBlank()
        // LOG.info("k-d tree=\n{}", kdTree);
        FileUtils.writeStringToFile(File("$outDir$basename-kdtree.tex"), kdTree, "UTF-8")
    }

    private class CustomArgumentsProvider : ArgumentsProvider {
        override fun provideArguments(extensionContext: ExtensionContext): Stream<out Arguments> =
                FileUtils.listFiles(File("data/tagml"), TAGML_FILE_FILTER, null)
                        .stream()
                        .map { obj: File -> obj.name }
                        .map { n: String -> n.replace(".tagml", "") }
                        .map { arguments: String -> Arguments.of(arguments) }

        companion object {
            private val TAGML_FILE_FILTER: IOFileFilter = object : IOFileFilter {
                override fun accept(file: File): Boolean = isTAGML(file.name)

                override fun accept(dir: File, name: String): Boolean = isTAGML(name)

                private fun isTAGML(name: String): Boolean = name.endsWith(".tagml")
            }
        }
    }

    companion object {
        private val LOG = LoggerFactory.getLogger(ImportDataTAGMLTest::class.java)
    }
}