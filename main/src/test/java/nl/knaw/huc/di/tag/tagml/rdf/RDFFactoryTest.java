package nl.knaw.huc.di.tag.tagml.rdf;

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

import nl.knaw.huc.di.tag.TAGBaseStoreTest;
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huc.di.tag.tagml.importer2.TAG;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.shared.PrefixMapping;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class RDFFactoryTest extends TAGBaseStoreTest {

  @Test
  public void testBasic() {
    String tagml = DUMMY_HEADER + "[line>The rain in Spain falls mainly on the plain.<line]";
    testRDFConversionContains(tagml, "");
    //        "tag:document0  a   tag:Document ;\n" +
    //            "        tag:layer  tag:layer_ ;\n" +
    //            "        tag:root   tag:markup2 .",
    //        "tag:layer_  a           tag:LayerNode ;\n" +
    //            "        tag:layer_name  \"\" .",
    //        "tag:markup2  a           tag:MarkupNode ;\n" +
    //            "        tag:elements     ( tag:text3 ) ;\n" +
    //            "        tag:layer        tag:layer_ ;\n" +
    //            "        tag:markup_name  \"line\" .",
    //        "tag:text3  a         tag:TextNode ;\n" +
    //            "        tag:content  \"The rain in Spain falls mainly on the plain.\" .");
  }

  @Test
  public void testLayers() {
    String tagml = DUMMY_HEADER + "[line|+A>Cookie Monster likes cookies.<line|A]";
    testRDFConversionContains(tagml, "tag:content  \"Cookie Monster likes cookies.\"");
    //        "tag:document0  a   tag:Document ;\n" +
    //            "        tag:layer  tag:layer_A ;\n" +
    //            "        tag:root   tag:markup2 .",
    //        "tag:layer_A  a          tag:LayerNode ;\n" +
    //            "        tag:layer_name  \"A\" .",
    //        "tag:markup2  a           tag:MarkupNode ;\n" +
    //            "        tag:elements     ( tag:text3 ) ;\n" +
    //            "        tag:layer        tag:layer_A ;\n" +
    //            "        tag:markup_name  \"line\" .",
    //        "tag:text3  a         tag:TextNode ;\n" +
    //            "        tag:content  \"Cookie Monster likes cookies.\" .");
  }

  @Test
  public void testAnnotations() {
    String tagml =
        DUMMY_HEADER + "[line month_1='November' month_2=11>In the eleventh month...<line]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testBooleanAnnotation() {
    String tagml =
        DUMMY_HEADER
            + "[line rhyme=false>There once was a vicar in [place real=true>Slough<place]<line]";
    testRDFConversionContains(tagml, "tag:value            true", "tag:value            false");
  }

  @Test
  public void testListAnnotation() {
    String tagml =
        DUMMY_HEADER
            + "[line>Donald and [group names=['Huey','Louie','Dewey']>his nephews<group] went for a ride.<line]";
    testRDFConversionContains(
        tagml, "tag:value  \"Huey\"", "tag:value  \"Louie\"", "tag:value  \"Dewey\"");
  }

  @Test
  public void testMapAnnotation() {
    String tagml =
        DUMMY_HEADER
            + "[quote>Van de maan af gezien zijn we allen even groot.\n[by>[alias person={first_name='Eduard' middle_name='Douwes' last_name='Dekker'}>Multatuli<alias]<by]<quote]";
    testRDFConversionContains(
        tagml, "tag:content  \"Van de maan af gezien zijn we allen even groot.\\n\"");
  }

  @Test
  public void testRichTextAnnotation() {
    String tagml =
        DUMMY_HEADER
            + "[text>Hello, my name is [gloss addition=[>[p>that’s [qualifier>Mrs.<qualifier] to you<p]<]>Doubtfire. How do you do?<gloss]<text]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testReferenceAnnotation() {
    String tagml =
        DUMMY_HEADER
            + "[text meta={persons=[{:id=huyg0001 name='Constantijn Huygens'}]}>[title>De Zee-Straet<title] door [author pers->huyg0001>Constantijn Huygens<author] ....... <text]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testOverlap() {
    String tagml = DUMMY_HEADER + "[line>[a|+A>Cookie Monster [b|+B>likes<a|A] cookies.<b|B]<line]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testHierarchical() {
    String tagml = DUMMY_HEADER + "[line>[a>Cookie Monster [b>likes<b] cookies.<a]<line]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testDiscontinuity() {
    String tagml =
        DUMMY_HEADER
            + "[ex>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<ex]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testNonLinearity() {
    String tagml = DUMMY_HEADER + "[q>To be, or <|to be not|not to be|>.<q]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void loadOntology() {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
    model.read(TAG.NS);
    System.out.println(DotFactory.fromModel(model));
  }

  private void testRDFConversionContains(String tagml, final String... turtleStatements) {
    runInStore(
        store -> {
          TAGDocument document =
              store.runInTransaction(() -> new TAGMLImporter(store).importTAGML(tagml.trim()));
          Model model = store.runInTransaction(() -> RDFFactory.fromDocument(document));

          //      String queryString = "prefix tag: <" + TAG.getURI() + "> "
          //          + "select ?s ?o "
          //          + "where { ?s <" + TAG.markupName + "> ?o . }";
          //      System.out.println(queryString);
          //      List<Map<String, String>> resultList = getResults(model, queryString);
          //      System.out.println(resultList);

          String dot = DotFactory.fromModel(model);

          System.out.println(
              "\n------------TTL------------------------------------------------------------------------------------\n");
          model.write(System.out, "TURTLE");
          System.out.println(
              "\n------------TTL------------------------------------------------------------------------------------\n");

          System.out.println(
              "\n------------8<------------------------------------------------------------------------------------\n");
          System.out.println(dot);
          System.out.println(
              "\n------------8<------------------------------------------------------------------------------------\n");

          final OutputStream baos = new ByteArrayOutputStream();
          model.write(baos, "TURTLE");
          String ttl = baos.toString();
          assertThat(ttl).contains(turtleStatements);
        });
  }

  private List<Map<String, String>> getResults(final Model model, final String queryString) {
    Query query = QueryFactory.create(queryString);
    PrefixMapping prefixMapping = query.getPrefixMapping();
    QueryExecution qe = QueryExecutionFactory.create(query, model);
    //    if (query.isSelectType()){
    ResultSet results = qe.execSelect();
    //    boolean b = qe.execAsk();
    //    Model model1 = qe.execConstruct();
    //    Model model2 = qe.execDescribe();
    System.out.println("----------------------------------------------------------------");
    ResultSetFormatter.out(System.out, results, query);
    System.out.println("----------------------------------------------------------------");
    ResultSetFormatter.outputAsJSON(QueryExecutionFactory.create(query, model).execSelect());
    System.out.println("----------------------------------------------------------------");
    ResultSetFormatter.outputAsCSV(QueryExecutionFactory.create(query, model).execSelect());
    System.out.println("----------------------------------------------------------------");
    ResultSetFormatter.outputAsTSV(QueryExecutionFactory.create(query, model).execSelect());
    System.out.println("----------------------------------------------------------------");
    ResultSetFormatter.outputAsXML(QueryExecutionFactory.create(query, model).execSelect());
    System.out.println("----------------------------------------------------------------");
    List<Map<String, String>> resultList = new ArrayList<>();
    while (results.hasNext()) {
      QuerySolution querySolution = results.next();
      Map<String, String> result = new HashMap<>();
      querySolution
          .varNames()
          .forEachRemaining(
              (String varName) -> {
                RDFNode node = querySolution.get(varName);
                String val =
                    node.isResource()
                        ? "<" + prefixMapping.shortForm(node.asResource().getURI()) + ">"
                        : node.toString();
                result.put(varName, val);
              });
      resultList.add(result);
    }
    qe.close();
    return resultList;
  }
}
