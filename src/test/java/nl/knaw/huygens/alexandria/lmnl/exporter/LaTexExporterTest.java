package nl.knaw.huygens.alexandria.lmnl.exporter;

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


import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class LaTexExporterTest {
  private static Logger LOG = LoggerFactory.getLogger(LaTexExporterTest.class);

  @Test
  public void testLaTeXOutput1() {
    String laTeX = laTeXFromLMNLString("[l [n}144{n]}He manages to keep the upper hand{l]");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTeXOutput2() throws IOException {
    String laTeX = laTeXFromLMNLFile("data/lmnl/1kings12.lmnl");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTeXOutput3() throws IOException {
    String laTeX = laTeXFromLMNLFile("data/lmnl/ozymandias-voices-wap.lmnl");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTeXOutputWithDiscontinuation() {
    String laTeX = laTeXFromLMNLString("'[e=e1}Ai,{e=e1]' riep Piet, '[e=e1}wat doe je, Mien?{e=e1]'");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTexOutputDataFiles() throws IOException {
    processLMNLFile("alice-excerpt");
    processLMNLFile("1kings12");
    processLMNLFile("ozymandias-voices-wap");
    processLMNLFile("frost-quote-nows");
    processLMNLFile("snark81");
  }

  private void processLMNLFile(String basename) throws IOException {
    InputStream input = FileUtils.openInputStream(new File("data/lmnl/" + basename + ".lmnl"));
    Document document = new LMNLImporter().importLMNL(input);
    LaTeXExporter exporter = new LaTeXExporter(document);
    String outDir = "out/";

    String laTeX = exporter.exportDocument();
    assertThat(laTeX).isNotBlank();
    LOG.info("document=\n{}", laTeX);
    FileUtils.writeStringToFile(new File(outDir + basename + ".tex"), laTeX, "UTF-8");

    String overlap = exporter.exportGradient();
    assertThat(overlap).isNotBlank();
    LOG.info("overlap=\n{}", overlap);
    FileUtils.writeStringToFile(new File(outDir + basename + "-gradient.tex"), overlap, "UTF-8");

    String coloredText = exporter.exportMarkupOverlap();
    assertThat(coloredText).isNotBlank();
    FileUtils.writeStringToFile(new File(outDir + basename + "-colored-text.tex"), coloredText, "UTF-8");

    String matrix = exporter.exportMatrix();
    assertThat(matrix).isNotBlank();
    LOG.info("matrix=\n{}", laTeX);
    FileUtils.writeStringToFile(new File(outDir + basename + "-matrix.tex"), matrix, "UTF-8");

    String kdTree = exporter.exportKdTree();
    assertThat(kdTree).isNotBlank();
    LOG.info("k-d tree=\n{}", kdTree);
    FileUtils.writeStringToFile(new File(outDir + basename + "-kdtree.tex"), kdTree, "UTF-8");
  }

  private String laTeXFromLMNLString(String input) {
    Document document = new LMNLImporter().importLMNL(input);
    return toLaTeX(document);
  }

  private String laTeXFromLMNLFile(String pathname) throws IOException {
    InputStream input = FileUtils.openInputStream(new File(pathname));
    Document document = new LMNLImporter().importLMNL(input);
    return toLaTeX(document);
  }

  private String toLaTeX(Document document) {
    LaTeXExporter exporter = new LaTeXExporter(document);
    return exporter.exportDocument();
  }

  private void printLaTeX(String laTeX) {
    System.out.println(laTeX);
    // LOG.info("latex=\n{}", laTeX);
  }
}
