package nl.knaw.huygens.alexandria.texmecs.importer;

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

import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.exporter.LaTeXExporterInMemory;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ImportDataTexMECSInMemoryTest {
  private static final Logger LOG = LoggerFactory.getLogger(ImportDataTexMECSInMemoryTest.class);

  private static final IOFileFilter MECS_FILE_FILTER = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return isTexMECS(file.getName());
    }

    @Override
    public boolean accept(File dir, String name) {
      return isTexMECS(name);
    }

    private boolean isTexMECS(String name) {
      return name.endsWith(".texmecs") && !name.contains("syntax-error");
    }
  };

  public static Collection<String[]> basenameProvider() {
    return FileUtils.listFiles(new File("data/texmecs"), MECS_FILE_FILTER, null)
        .stream()
        .map(File::getName)
        .map(n -> n.replace(".texmecs", ""))
        .map(b -> new String[]{b})
        .collect(Collectors.toList());
  }

  @ParameterizedTest(name = "#{index} - data/texmecs/{0}.texmecs")
  @MethodSource("basenameProvider")
  public void testTexMECSFile(String basename) throws IOException {
    LOG.info("testing data/texmecs/{}.texmecs", basename);
    processTexMECSFile(basename);
    LOG.info("done testing data/texmecs/{}.texmecs", basename);
  }

  private void processTexMECSFile(String basename) throws IOException {
    InputStream input = getInputStream(basename);
    LOG.info("showTokens\n");
    printTokens(input);

    input = getInputStream(basename);
    LOG.info("testing data/texmecs/{}.texmecs", basename);
    LOG.info("importTexMECS\n");
    Document document = new TexMECSImporterInMemory().importTexMECS(input);

    generateLaTeX(basename, document);
  }

  private InputStream getInputStream(String basename) throws IOException {
    return FileUtils.openInputStream(new File("data/texmecs/" + basename + ".texmecs"));
  }

  private void generateLaTeX(String basename, Document document) throws IOException {
    LaTeXExporterInMemory exporter = new LaTeXExporterInMemory(document);
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

  protected void printTokens(String input) {
    System.out.println("TexMECS:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
  }

  private void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    TexMECSLexer lexer = new TexMECSLexer(inputStream);
    Token token;
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        System.out.println(token + ": " + lexer.getRuleNames()[token.getType() - 1] + ": " + lexer.getModeNames()[lexer._mode]);
      }
    } while (token.getType() != Token.EOF);
  }

}
