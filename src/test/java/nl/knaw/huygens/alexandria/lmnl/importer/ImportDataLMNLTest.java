package nl.knaw.huygens.alexandria.lmnl.importer;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.exporter.LaTeXExporter;

@RunWith(Parameterized.class)
public class ImportDataLMNLTest extends AlexandriaLMNLBaseTest {
  private static Logger LOG = LoggerFactory.getLogger(ImportDataLMNLTest.class);

  private String basename;
  public static final IOFileFilter LMNL_FILE_FILTER = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return isLMNL(file.getName());
    }

    @Override
    public boolean accept(File dir, String name) {
      return isLMNL(name);
    }

    private boolean isLMNL(String name) {
      return name.endsWith(".lmnl") && name.startsWith("f");
    }
  };

  @Parameters
  public static Collection<String[]> parameters() {
    return FileUtils.listFiles(new File("data/lmnl"), LMNL_FILE_FILTER, null)//
        .stream()//
        .map(File::getName)//
        .map(n -> n.replace(".lmnl", ""))//
        .map(b -> new String[] { b })//
        .collect(Collectors.toList());
  }

  public ImportDataLMNLTest(String basename) {
    this.basename = basename;
  }

  @Test
  public void testLMNLFile() throws IOException, LMNLSyntaxError {
    LOG.info("testing data/lmnl/{}.lmnl", basename);
    processLMNLFile(basename);
    LOG.info("done testing data/lmnl/{}.lmnl", basename);
  }

  private void processLMNLFile(String basename) throws IOException, LMNLSyntaxError {
    InputStream input = getInputStream(basename);
    LOG.info("showTokens\n");
    printTokens(input);

    input = getInputStream(basename);
    LOG.info("testing data/lmnl/{}.lmnl", basename);
    LOG.info("importLMNL\n");
    Document document = new LMNLImporter().importLMNL(input);

    generateLaTeX(basename, document);
  }

  private InputStream getInputStream(String basename) throws IOException {
    return FileUtils.openInputStream(new File("data/lmnl/" + basename + ".lmnl"));
  }

  private void generateLaTeX(String basename, Document document) throws IOException {
    LaTeXExporter exporter = new LaTeXExporter(document);
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
