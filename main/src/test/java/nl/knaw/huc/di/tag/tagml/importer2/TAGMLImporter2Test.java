package nl.knaw.huc.di.tag.tagml.importer2;

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
import nl.knaw.huc.di.tag.tagml.TAGMLBaseTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMLImporter2Test extends TAGMLBaseTest {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporter2Test.class);

//  @Test
  public void test() {
    String tagML = "[root>" +
        "[s><|[del>Dit kwam van een<del]|[del>[add>Gevolg van een<add]<del]|[add>De<add]|>" +
        " te streng doorgedreven rationalisatie van zijne " +
        "<|[del>opvoeding<del]|[del>[add>prinselijke jeugd<add]<del]|[add>prinsenjeugd [?del>bracht<?del] had dit met zich meegebracht<add]|><s]" +
        "<root]";

    TAGKnowledgeModel knowledgeModel = parseTAGML(tagML);
    assertThat(knowledgeModel).isNotNull();

  }

  private TAGKnowledgeModel parseTAGML(final String tagML) {
    LOG.info("TAGML=\n{}\n", tagML);
    String trimmedTagML = tagML.trim();
    printTokens(trimmedTagML);
    TAGKnowledgeModel knowledgeModel = new TAGMLImporter2().importTAGML(trimmedTagML);
    TAGMLImporter2.logDocumentGraph(knowledgeModel, trimmedTagML);
    return knowledgeModel;
  }

}
