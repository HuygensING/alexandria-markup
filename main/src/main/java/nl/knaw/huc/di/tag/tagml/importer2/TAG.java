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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class TAG {
  public static final String NS = "https://brambg.github.io/tag-rdf/tag.ttl#";

  public static final Resource Document = resource("Document");
  public static final Resource MarkupElement = resource("MarkupElement");
  public static final Resource TextNode = resource("TextNode");
  public static final Resource Annotation = resource("Annotation");

  public static final Property hasAnnotations = property("hasAnnotations");
  public static final Property hasElements = property("hasElements");
  public static final Property hasRootMarkup = property("hasRootMarkup");
  public static final Property next = property("next");
  public static final Property name = property("name");
  public static final Property value = property("value");
  public static final Property marksUp = property("marksUp");
  public static final Property firstTextNode = property("firstTextNode");

  private TAG() {
    throw new UnsupportedOperationException();
  }

  public static String getURI() {
    return NS;
  }

  private static Resource resource(String local) {
    return ResourceFactory.createResource(NS + local);
  }

  private static Property property(String local) {
    return ResourceFactory.createProperty(NS, local);
  }

}
