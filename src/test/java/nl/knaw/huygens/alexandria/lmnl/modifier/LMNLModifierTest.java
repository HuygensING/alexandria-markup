package nl.knaw.huygens.alexandria.lmnl.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;

public class LMNLModifierTest {
  static final Logger LOG = LoggerFactory.getLogger(LMNLModifierTest.class);

  @Test
  public void testAddRange() {
    String content = "The rain in Spain falls mainly on the plain.";
    String LMNL = "[text}" + content + "{text]";
    LMNLImporter importer = new LMNLImporter();
    Document document = importer.importLMNL(LMNL);
    LMNLModifier modifier = new LMNLModifier(document.value());
    TextRange countryRange = new TextRange(document.value(), "country");

    Position spainPosition = calculatePosition(content, "Spain");
    assertThat(spainPosition.getOffset()).isEqualTo(12);
    assertThat(spainPosition.getLength()).isEqualTo(5);
    modifier.addTextRange(countryRange, spainPosition);

    LMNLExporter exporter = new LMNLExporter();
    String modifiedLMNL = exporter.toLMNL(document);
    LOG.info("document.value().textNodeList={}", document.value().textNodeList);
    LOG.info("modifiedLMNL={}", modifiedLMNL);
    assertThat(document.value().textNodeList.toString()).isEqualTo("[\"The rain in \", \"Spain\", \" falls mainly on the plain.\"]");
    assertThat(modifiedLMNL).isEqualTo("[text}The rain in [country}Spain{country] falls mainly on the plain.{text]");
  }

  private Position calculatePosition(String content, String substring) {
    int offset = content.indexOf(substring);
    return new Position(offset, substring.length());
  }

}
