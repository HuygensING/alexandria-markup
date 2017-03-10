package nl.knaw.huygens.alexandria.lmnl.exporter;

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

/**
 * Created by bramb on 15-2-2017.
 */
public class LaTexExporterTest {
  private static Logger LOG = LoggerFactory.getLogger(LaTeXExporter.class);

  @Test
  public void testLaTeXOutput1() {
    String laTeX = laTeXFromLMNLString("[l [n}144{n]}He manages to keep the upper hand{l]");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTeXOutput2() throws IOException {
    String laTeX = laTeXFromLMNLFile("data/1kings12.lmnl");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTeXOutput3() throws IOException {
    String laTeX = laTeXFromLMNLFile("data/ozymandias-voices-wap.lmnl");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
  }

  @Test
  public void testLaTeXOutputWithDiscontinuation() {
    String laTeX = laTeXFromLMNLString("'[e=e1}Ai,{e=e1]' riep Piet, '[e=e1}wat doe je, Mien?{e=e1]'");
    printLaTeX(laTeX);
    assertThat(laTeX).isNotBlank();
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
//    LOG.info("latex=\n{}", laTeX);
  }
}
