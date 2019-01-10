package nl.knaw.huc.di.tag.tagml.xml.exporter;

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

import nl.knaw.huc.di.tag.TAGExporter;
import nl.knaw.huc.di.tag.TAGTraverser;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XMLExporter extends TAGExporter {
  private static final Logger LOG = LoggerFactory.getLogger(XMLExporter.class);

  public XMLExporter(TAGStore store) {
    super(store);
  }

  public XMLExporter(TAGStore store, TAGView view) {
    super(store, view);
  }

  public String asXML(TAGDocument document) {
    XMLBuilder xmlBuilder = new XMLBuilder();
    new TAGTraverser(store, view, document).accept(xmlBuilder);
    return xmlBuilder.getResult();
  }

}
