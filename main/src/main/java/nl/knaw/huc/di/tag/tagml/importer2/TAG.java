package nl.knaw.huc.di.tag.tagml.importer2;

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

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

public class TAG {
  public static final String NS = "https://huygensing.github.io/TAG/TAGML/ontology/tagml.ttl#";

  public static final Resource Document = resource("Document");
  public static final Resource MarkupNode = resource("MarkupNode");
  public static final Resource BranchesNode = resource("BranchesNode");
  public static final Resource BranchNode = resource("BranchNode");
  public static final Resource TextNode = resource("TextNode");
  public static final Resource Annotation = resource("AnnotationNode");
  public static final Resource StringAnnotation = resource("StringAnnotationNode");
  public static final Resource NumberAnnotation = resource("NumberAnnotationNode");
  public static final Resource BooleanAnnotation = resource("BooleanAnnotationNode");
  public static final Resource ReferenceAnnotation = resource("ReferenceAnnotationNode");
  public static final Resource ListAnnotation = resource("ListAnnotationNode");
  public static final Resource MapAnnotation = resource("MapAnnotationNode");
  public static final Resource RichTextAnnotation = resource("RichTextAnnotationNode");
  public static final Resource Layer = resource("LayerNode");
  public static final Resource ElementList = resource("ElementList");
  public static final Resource BranchList = resource("BranchList");

  public static final Property first = property("first");
  public static final Property rest = property("rest");
  public static final Property root = property("root");
  public static final Property markupName = property("markup_name");
  public static final Property layer = property("layer");
  public static final Property layerName = property("layer_name");
  public static final Property annotation = property("annotation");
  public static final Property annotationName = property("annotation_name");
  public static final Property elements = property("elements");
  public static final Property value = property("value");
  public static final Property content = property("content");
  public static final Property continued = property("continued");
  public static final Property branches = property("branches");

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
