package nl.knaw.huc.di.tag.tagml.rdf;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class RDFFactoryTest extends TAGBaseStoreTest {

  @Test
  public void testBasic() {
    String tagml = "[line>The rain in Spain falls mainly on the plain.<line]";
    testRDFConversionContains(tagml,
        "");
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
    String tagml = "[line|+A>Cookie Monster likes cookies.<line|A]";
    testRDFConversionContains(tagml,
        "tag:content  \"Cookie Monster likes cookies.\"");
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
    String tagml = "[line month_1='November' month_2=11>In the eleventh month...<line]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testBooleanAnnotation() {
    String tagml = "[line rhyme=false>There once was a vicar in [place real=true>Slough<place]<line]";
    testRDFConversionContains(tagml, "tag:value            true",
        "tag:value            false");
  }

  @Test
  public void testListAnnotation() {
    String tagml = "[line>Donald and [group names=['Huey','Louie','Dewey']>his nephews<group] went for a ride.<line]";
    testRDFConversionContains(tagml, "tag:value  \"Huey\"",
        "tag:value  \"Louie\"",
        "tag:value  \"Dewey\"");
  }

  @Test
  public void testOverlap() {
    String tagml = "[line>[a|+A>Cookie Monster [b|+B>likes<a|A] cookies.<b|B]<line]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testHierarchical() {
    String tagml = "[line>[a>Cookie Monster [b>likes<b] cookies.<a]<line]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testDiscontinuity() {
    String tagml = "[ex>[q>and what is the use of a book,<-q] thought Alice[+q>without pictures or conversation?<q]<ex]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void testNonLinearity() {
    String tagml = "[q>To be, or <|to be not|not to be|>.<q]";
    testRDFConversionContains(tagml, "");
  }

  @Test
  public void loadOntology() {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
    model.read(TAG.NS);
    System.out.println(DotFactory.fromModel(model));
  }

  private void testRDFConversionContains(String tagml, final String... turtleStatements) {
    runInStore(store -> {
      TAGDocument document = store.runInTransaction(() -> new TAGMLImporter(store).importTAGML(tagml.trim()));
      Model model = store.runInTransaction(() -> RDFFactory.fromDocument(document));
      String dot = DotFactory.fromModel(model);

      System.out.println("\n------------TTL------------------------------------------------------------------------------------\n");
      model.write(System.out, "TURTLE");
      System.out.println("\n------------TTL------------------------------------------------------------------------------------\n");

      System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
      System.out.println(dot);
      System.out.println("\n------------8<------------------------------------------------------------------------------------\n");

      final OutputStream baos = new ByteArrayOutputStream();
      model.write(baos, "TURTLE");
      String ttl = baos.toString();
      assertThat(ttl).contains(turtleStatements);
    });

  }

}
