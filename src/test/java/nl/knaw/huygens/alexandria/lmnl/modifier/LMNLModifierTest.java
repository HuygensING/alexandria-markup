package nl.knaw.huygens.alexandria.lmnl.modifier;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Annotation;
import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;
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
    Markup countryRange = new Markup(document.value(), "country");

    Position spainPosition = calculatePosition(content, "Spain");
    assertThat(spainPosition.getOffset()).isEqualTo(12);
    assertThat(spainPosition.getLength()).isEqualTo(5);
    modifier.addMarkup(countryRange, spainPosition);

    LMNLExporter exporter = new LMNLExporter();
    String modifiedLMNL = exporter.toLMNL(document);
    LOG.info("document.value().textNodeList={}", document.value().textNodeList);
    LOG.info("modifiedLMNL={}", modifiedLMNL);
    assertThat(document.value().textNodeList.toString()).isEqualTo("[\"The rain in \", \"Spain\", \" falls mainly on the plain.\"]");
    assertThat(modifiedLMNL).isEqualTo("[text}The rain in [country}Spain{country] falls mainly on the plain.{text]");

    List<Position> positions = new ArrayList<>();
    positions.add(calculatePosition(content, "rain"));
    positions.add(calculatePosition(content, "Spain"));
    positions.add(calculatePosition(content, "plain"));
    Markup rhymeRange = new Markup(document.value(), "rhyme=r1");
    rhymeRange.addAnnotation(new Annotation("suffix", "ain"));

    modifier.addMarkup(rhymeRange, positions);
    modifiedLMNL = exporter.toLMNL(document);
    LOG.info("document.value().textNodeList={}", document.value().textNodeList);
    LOG.info("modifiedLMNL={}", modifiedLMNL);
    assertThat(document.value().textNodeList.toString()).isEqualTo("[\"The \", \"rain\", \" in \", \"Spain\", \" falls mainly on the \", \"plain\", \".\"]");
    assertThat(modifiedLMNL).isEqualTo(
        "[text}The [rhyme=r1 [suffix}ain{suffix]}rain{rhyme=r1] in [country}[rhyme=r1 [suffix}ain{suffix]}Spain{rhyme=r1]{country] falls mainly on the [rhyme=r1 [suffix}ain{suffix]}plain{rhyme=r1].{text]");
  }

  private Position calculatePosition(String content, String substring) {
    int offset = content.indexOf(substring);
    return new Position(offset, substring.length());
  }

}
