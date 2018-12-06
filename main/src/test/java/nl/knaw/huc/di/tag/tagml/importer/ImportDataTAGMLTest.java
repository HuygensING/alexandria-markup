package nl.knaw.huc.di.tag.tagml.importer;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError;
import nl.knaw.huygens.alexandria.exporter.LaTeXExporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class ImportDataTAGMLTest extends TAGBaseStoreTest {
  private static final Logger LOG = LoggerFactory.getLogger(ImportDataTAGMLTest.class);
  private static final IOFileFilter TAGML_FILE_FILTER = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return isTAGML(file.getName());
    }

    @Override
    public boolean accept(File dir, String name) {
      return isTAGML(name);
    }

    private boolean isTAGML(String name) {
      return name.endsWith(".tagml");
    }
  };
  private final String basename;

  public ImportDataTAGMLTest(String basename) {
    this.basename = basename;
  }

  @Parameters
  public static Collection<String[]> parameters() {
    return FileUtils.listFiles(new File("data/tagml"), TAGML_FILE_FILTER, null)//
        .stream()//
        .map(File::getName)//
        .map(n -> n.replace(".tagml", ""))//
        .map(b -> new String[]{b})//
        .collect(Collectors.toList());
  }

  @Test
  public void testTAGMLFile() throws TAGMLSyntaxError {
    LOG.info("testing data/tagml/{}.tagml", basename);
    processTAGMLFile(basename);
    LOG.info("done testing data/tagml/{}.tagml", basename);
  }

  private void processTAGMLFile(String basename) throws TAGMLSyntaxError {
    store.runInTransaction(() -> {
      try {
        InputStream input = getInputStream(basename);
        List<String> lines = IOUtils.readLines(input, Charset.defaultCharset());
        LOG.info("\nTAGML:\n{}\n", String.join("\n", lines));

        input = getInputStream(basename);
        printTokens(input);

        input = getInputStream(basename);
        LOG.info("testing data/tagml/{}.tagml", basename);
        LOG.info("importTAGML\n");
        TAGDocument document = new TAGMLImporter(store).importTAGML(input);
        logDocumentGraph(document, "");
//        generateLaTeX(basename, document);
      } catch (IOException e) {
        e.printStackTrace();
        throw new UncheckedIOException(e);
      }
    });
  }

  private InputStream getInputStream(String basename) throws IOException {
    return FileUtils.openInputStream(new File("data/tagml/" + basename + ".tagml"));
  }

  private void generateLaTeX(String basename, TAGDocument document) throws IOException {
    LaTeXExporter exporter = new LaTeXExporter(store, document);
    String outDir = "out/";

    String laTeX = exporter.exportDocument();
    assertThat(laTeX).isNotBlank();
    // LOG.info("document=\n{}", laTeX);
    FileUtils.writeStringToFile(new File(outDir + basename + ".tex"), laTeX, "UTF-8");

    String overlap = exporter.exportGradient();
    assertThat(overlap).isNotBlank();
    // LOG.info("overlap=\n{}", overlap);
    FileUtils.writeStringToFile(new File(outDir + basename + "-gradient.tex"), overlap, "UTF-8");

    String coloredText = exporter.exportMarkupOverlap();
    assertThat(coloredText).isNotBlank();
    FileUtils.writeStringToFile(new File(outDir + basename + "-colored-text.tex"), coloredText, "UTF-8");

    String matrix = exporter.exportMatrix();
    assertThat(matrix).isNotBlank();
    // LOG.info("matrix=\n{}", laTeX);
    FileUtils.writeStringToFile(new File(outDir + basename + "-matrix.tex"), matrix, "UTF-8");

    String kdTree = exporter.exportKdTree();
    assertThat(kdTree).isNotBlank();
    // LOG.info("k-d tree=\n{}", kdTree);
    FileUtils.writeStringToFile(new File(outDir + basename + "-kdtree.tex"), kdTree, "UTF-8");
  }

}
