package nl.knaw.huygens.alexandria.lmnl.importer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.exporter.LaTeXExporter;

@RunWith(Parameterized.class)
public class ImportDataLMNLTest extends AlexandriaLMNLBaseTest {
  private static Logger LOG = LoggerFactory.getLogger(ImportDataLMNLTest.class);

  private String basename;

  @Parameters
  public static Collection<String[]> parameters() {
    return FileUtils.listFiles(new File("data"), TrueFileFilter.INSTANCE, null)//
        .stream()//
        .map(f -> f.getName())//
        .map(n -> n.replace(".lmnl", ""))//
        .map(b -> new String[] { b })//
        .collect(Collectors.toList());
  }

  public ImportDataLMNLTest(String basename) {
    this.basename = basename;
  }

  @Test
  public void testLMNLFile() throws IOException {
    LOG.info("testing data/{}.lmnl", basename);
    processLMNLFile(basename);
  }

  private void processLMNLFile(String basename) throws IOException {
    InputStream input = getInputStream(basename);
    LOG.info("showTokens\n");
    printTokens(input);

    input = getInputStream(basename);
    LOG.info("importLMNL\n");
    Document document = new LMNLImporter().importLMNL(input);

    generateLaTeX(basename, document);
  }

  private InputStream getInputStream(String basename) throws IOException {
    return FileUtils.openInputStream(new File("data/" + basename + ".lmnl"));
  }

  private void generateLaTeX(String basename, Document document) throws IOException {
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

    String coloredText = exporter.exportTextRangeOverlap();
    assertThat(coloredText).isNotBlank();
    FileUtils.writeStringToFile(new File(outDir + basename + "-colored-text.tex"), coloredText);

    String matrix = exporter.exportMatrix();
    assertThat(matrix).isNotBlank();
    LOG.info("matrix=\n{}", laTeX);
    FileUtils.writeStringToFile(new File(outDir + basename + "-matrix.tex"), matrix, "UTF-8");

    String kdTree = exporter.exportKdTree();
    assertThat(kdTree).isNotBlank();
    LOG.info("k-d tree=\n{}", kdTree);
    FileUtils.writeStringToFile(new File(outDir + basename + "-kdtree.tex"), kdTree, "UTF-8");
  }

}
