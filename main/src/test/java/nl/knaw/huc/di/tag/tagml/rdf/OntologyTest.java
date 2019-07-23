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

import nl.knaw.huc.di.tag.tagml.importer2.TAG;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class OntologyTest {

  @Test
  public void testTAGMLOntology() throws FileNotFoundException {
    String namespace = TAG.NS;
    OntModel model = ModelFactory.createOntologyModel(OntModelSpec.RDFS_MEM);
    model.setNsPrefix("tag", namespace);

    OntClass document = createLabeledResourceClass(model, TAG.Document);

    OntClass markupNode = createLabeledClass(model, TAG.MarkupNode);
    OntClass branchesNode = createLabeledClass(model, TAG.BranchesNode);
    OntClass branchNode = createLabeledClass(model, TAG.BranchNode);

    OntClass textNode = createLabeledResourceClass(model, TAG.TextNode);

    OntClass annotationNode = createLabeledResourceClass(model, TAG.Annotation);

    createAnnotationSubClass(model, annotationNode, TAG.StringAnnotation);

    createAnnotationSubClass(model, annotationNode, TAG.NumberAnnotation);

    createAnnotationSubClass(model, annotationNode, TAG.BooleanAnnotation);

    createAnnotationSubClass(model, annotationNode, TAG.ReferenceAnnotation);

    createAnnotationSubClass(model, annotationNode, TAG.ListAnnotation);

    createAnnotationSubClass(model, annotationNode, TAG.MapAnnotation);

    createAnnotationSubClass(model, annotationNode, TAG.RichTextAnnotation);

    OntClass layerNode = createLabeledResourceClass(model, TAG.Layer);

    OntClass elementList = createLabeledClass(model, TAG.ElementList);
    elementList.addSuperClass(RDF.List);

    OntClass branchList = createLabeledClass(model, TAG.BranchList);
    branchList.addSuperClass(RDF.List);

    OntClass nil = model.createClass(namespace + "nil");
    nil.addSuperClass(elementList);

    OntProperty first = createLabeledProperty(model, TAG.first);
    first.addDomain(elementList);
    first.addRange(markupNode);
    first.addRange(textNode);

    OntProperty rest = createLabeledProperty(model, TAG.rest);
    rest.addDomain(elementList);
    rest.addRange(elementList);

    OntProperty root = createLabeledProperty(model, TAG.root);
    root.addDomain(document);
    root.addRange(markupNode);

    OntProperty markupName = createLabeledProperty(model, TAG.markupName);
    markupName.addDomain(markupNode);
    markupName.addRange(RDFS.Literal);

    OntProperty layer = createLabeledProperty(model, TAG.layer);
    layer.addDomain(markupNode);
    layer.addRange(layerNode);

    OntProperty layerName = createLabeledProperty(model, TAG.layerName);
    layerName.addDomain(layerNode);
    layerName.addRange(RDFS.Literal);

    OntProperty annotation = createLabeledProperty(model, TAG.annotation);
    annotation.addDomain(markupNode);
    annotation.addRange(annotationNode);

    OntProperty elements = createLabeledProperty(model, TAG.elements);
    elements.addDomain(markupNode);
    elements.addRange(elementList);
//    elements.addRange(markupNode);
//    elements.addRange(textNode);

    OntProperty annotationName = createLabeledProperty(model, TAG.annotationName);
    annotationName.addDomain(annotationNode);
    annotationName.addRange(RDFS.Literal);

    OntProperty value = createLabeledProperty(model, TAG.value);
    value.addDomain(annotationNode);
    value.addRange(RDFS.Literal);
    value.addRange(RDFS.Container);
    value.addRange(document);

    OntProperty content = createLabeledProperty(model, TAG.content);
    content.addDomain(textNode);
    content.addRange(RDFS.Literal);

    OntProperty hasbranches = createLabeledProperty(model, TAG.branches);
    hasbranches.addDomain(branchesNode);
    hasbranches.addRange(branchList);

//    OntProperty next = model.createOntProperty(namespace + "next");
//    addLabel(next, "next");
//    next.addDomain(textNode);
//    next.addRange(textNode);

    System.out.println(DotFactory.fromModel(model));
    model.write(System.out, "TURTLE");
    model.write(new FileOutputStream(new File("out/tagml.rdf")));
    model.write(new FileOutputStream(new File("out/tagml.jsonld")), "JSONLD");
    model.write(new FileOutputStream(new File("out/tagml.ttl")), "TURTLE");
  }

  private OntProperty createLabeledProperty(final OntModel model, final Property property) {
    String label = property.getLocalName();
    OntProperty first = model.createOntProperty(TAG.NS + label);
    addLabel(first, label);
    return first;
  }

  private OntClass createLabeledResourceClass(final OntModel model, final Resource resource) {
    OntClass ontClass = createLabeledClass(model, resource);
    ontClass.addSuperClass(RDFS.Resource);
    return ontClass;
  }

  private OntClass createLabeledClass(final OntModel model, final Resource resource) {
    final String label = resource.getLocalName();
    OntClass ontClass = model.createClass(TAG.NS + label);
    addLabel(ontClass, label);
    return ontClass;
  }

  private OntClass createAnnotationSubClass(final OntModel model, final OntClass annotationNode, final Resource resource) {
    OntClass subAnnotationClass = createLabeledClass(model, resource);
    subAnnotationClass.addSuperClass(annotationNode);
    return subAnnotationClass;
  }

  private void addLabel(final OntResource resource, final String label) {
    resource.addLabel(label, "en");
  }

}
