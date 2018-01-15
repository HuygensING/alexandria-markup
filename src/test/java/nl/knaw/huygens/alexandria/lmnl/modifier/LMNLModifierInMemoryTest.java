package nl.knaw.huygens.alexandria.lmnl.modifier;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.data_model.Annotation;
import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.data_model.Markup;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporterInMemory;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporterInMemory;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LMNLModifierInMemoryTest {
  static final Logger LOG = LoggerFactory.getLogger(LMNLModifierInMemoryTest.class);

  @Test
  public void testAddRange() throws LMNLSyntaxError {
    String content = "The rain in Spain falls mainly on the plain.";
    String LMNL = "[text}" + content + "{text]";
    LMNLImporterInMemory importer = new LMNLImporterInMemory();
    Document document = importer.importLMNL(LMNL);
    LMNLModifier modifier = new LMNLModifier(document.value());
    Markup countryRange = new Markup(document.value(), "country");

    Position spainPosition = calculatePosition(content, "Spain");
    assertThat(spainPosition.getOffset()).isEqualTo(12);
    assertThat(spainPosition.getLength()).isEqualTo(5);
    modifier.addMarkup(countryRange, spainPosition);

    LMNLExporterInMemory exporter = new LMNLExporterInMemory();
//    LMNLExporter exporter = new LMNLExporter(SHOW_ALL_VIEW);
    String modifiedLMNL = exporter.toLMNL(document);
    LOG.info("document.getDocumentId().textNodeList={}", document.value().textNodeList);
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
    LOG.info("document.getDocumentId().textNodeList={}", document.value().textNodeList);
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
