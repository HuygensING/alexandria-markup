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

public class RDFFactoryTest extends TAGBaseStoreTest {

  @Test
  public void test1() {
    String tagml = "[line>The rain in Spain falls mainly on the plain.<line]";
    testRDFConversion(tagml);
  }

  @Test
  public void test2() {
    String tagml = "[line|+A>Cookie Monster likes cookies.<line|A]";
    testRDFConversion(tagml);
  }

  @Test
  public void test3() {
    String tagml = "[line month_1='November' month_2=11>In the eleventh month...<line]";
    testRDFConversion(tagml);
  }

  @Test
  public void test4() {
    String tagml = "[line>[a|+A>Cookie Monster [b|+B>likes<a|A] cookies.<b|B]<line]";
    testRDFConversion(tagml);
  }

  @Test
  public void test5() {
    String tagml = "[line>[a>Cookie Monster [b>likes<b] cookies.<a]<line]";
    testRDFConversion(tagml);
  }

  @Test
  public void loadOntology() {
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
    model.read(TAG.NS);
    System.out.println(DotFactory.fromModel(model));
  }

  private void testRDFConversion(String tagml) {
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
    });
  }

}
