package nl.knaw.huc.di.tag.sparql;

/*-
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

import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huc.di.tag.tagml.importer2.TAG;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.RDF;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class SPARQLQueryHandlerTest extends AlexandriaBaseStoreTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testSPARQLQuerySelect() {
    String tagml = "[x>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<x]";
    runInStoreTransaction(store -> {
      TAGDocument alice = new TAGMLImporter(store).importTAGML(tagml);

      SPARQLQueryHandler h = new SPARQLQueryHandler(alice);
      String statement = "prefix tag: <" + TAG.getURI() + "> " +
          "prefix rdf: <" + RDF.getURI() + "> " +
          "select ?markup (count(?markup) as ?count) " +
          "where { [] tag:markup_name ?markup . } " +
          "group by ?markup " + // otherwise: "Non-group key variable in SELECT"
          "order by ?markup";
      LOG.info(statement);
      SPARQLResult result = h.execute(statement);
      LOG.info("result={}", result);
      assertQuerySucceeded(result);
      List<String> expected = new ArrayList<>();
      expected.add(normalizeLineEndings("------------------\n" +
          "| markup | count |\n" +
          "==================\n" +
          "| \"q\"    | 2     |\n" +
          "| \"x\"    | 1     |\n" +
          "------------------\n"));

      assertThat(result.getValues()).containsExactlyElementsOf(expected);
    });
  }

  @Test
  public void testSPARQLQueryAsk() {
    String tagml = "[x>some text<x]";
    runInStoreTransaction(store -> {
      TAGDocument alice = new TAGMLImporter(store).importTAGML(tagml);

      SPARQLQueryHandler h = new SPARQLQueryHandler(alice);
      String statement = "prefix tag: <" + TAG.getURI() + "> " +
          "prefix rdf: <" + RDF.getURI() + "> " +
          "ask {" +
          "  ?m tag:markup_name 'x' ." + // markup has name 'x'
          "  ?m tag:elements ?list ." + // markup has elements ?list = rdf:list
          "  ?list rdf:rest*/rdf:first ?t ." + // the list has a textnode ?t
          "  ?t tag:content 'some text' . " + // textnode has content 'some text'
          "}";
      SPARQLResult result = h.execute(statement);
      LOG.info("result={}", result);
      assertQuerySucceeded(result);
      List<Boolean> expected = new ArrayList<>();
      expected.add(true);
      assertThat(result.getValues()).containsExactlyElementsOf(expected);
    });
  }

  @Test
  public void testSPARQLQueryDescribe() {
    String tagml = "[l>[w>Just<w] [w>some<w] [w>words<w]<l]";
    runInStoreTransaction(store -> {
      TAGDocument alice = new TAGMLImporter(store).importTAGML(tagml);

      SPARQLQueryHandler h = new SPARQLQueryHandler(alice);
      String statement = "prefix tag: <" + TAG.getURI() + "> " +
          "prefix rdf: <" + RDF.getURI() + "> " +
          "describe ?x where { ?x tag:markup_name 'w' }";
      SPARQLResult result = h.execute(statement);
      LOG.info("result={}", result);
      assertQuerySucceeded(result);
      List<String> expected = new ArrayList<>();
      expected.add("@prefix rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .\n" +
          "@prefix tag:   <https://huygensing.github.io/TAG/TAGML/ontology/tagml.ttl#> .\n" +
          "\n" +
          "tag:markup9  a           tag:MarkupNode ;\n" +
          "        tag:elements     ( tag:text10 ) ;\n" +
          "        tag:layer        tag:layer_ ;\n" +
          "        tag:markup_name  \"w\" .\n" +
          "\n" +
          "tag:markup3  a           tag:MarkupNode ;\n" +
          "        tag:elements     ( tag:text4 ) ;\n" +
          "        tag:layer        tag:layer_ ;\n" +
          "        tag:markup_name  \"w\" .\n" +
          "\n" +
          "tag:markup6  a           tag:MarkupNode ;\n" +
          "        tag:elements     ( tag:text7 ) ;\n" +
          "        tag:layer        tag:layer_ ;\n" +
          "        tag:markup_name  \"w\" .\n");
//      assertThat(result.getValues()).containsExactlyElementsOf(expected);
    });
  }

  @Test
  public void testSPARQLQueryConstruct() {
    String tagml = "[l>[person>John<person] went to [country>Spain<country], [person>Rachel<person] went to [country>Peru<country]<l]";
    runInStoreTransaction(store -> {
      TAGDocument alice = new TAGMLImporter(store).importTAGML(tagml);

      SPARQLQueryHandler h = new SPARQLQueryHandler(alice);
      String statement = "prefix tag: <" + TAG.getURI() + "> " +
          "prefix rdf: <" + RDF.getURI() + "> " +
          "prefix foaf: <" + FOAF.getURI() + "> " +
          "construct {" +
          " ?m rdf:type  foaf:Person;" +
          "    foaf:name ?name . " +
          "} where {" +
          "  ?m  tag:markup_name                               'person' ;" +
          "      tag:elements/rdf:rest*/rdf:first/tag:content  ?name . " +
          "}";
      SPARQLResult result = h.execute(statement);
      assertQuerySucceeded(result);
      System.out.println(statement);
      System.out.println(result.getValues());
//      List<String> expected = new ArrayList<>();
//      expected.add("");
//      assertThat(result.getValues()).containsExactlyElementsOf(expected);
    });
  }

  private String normalizeLineEndings(final String string) {
    return string.replaceAll("\\n", System.lineSeparator());
  }

  private void assertQuerySucceeded(SPARQLResult result) {
    if (!result.isOk()) {
      LOG.error("errors: {}", result.getErrors());
    }
    assertThat(result).isNotNull();
    assertThat(result.isOk()).isTrue();
  }

}
