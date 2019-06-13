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

import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huc.di.tag.tagml.importer2.TAG;
import nl.knaw.huygens.alexandria.storage.AnnotationType;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class RDFFactory {

  static Logger LOG = LoggerFactory.getLogger(RDFFactory.class);

  public static Model fromDocument(TAGDocument document) {
    AtomicLong resourceCounter = new AtomicLong();
    Model model = ModelFactory.createDefaultModel()
        .setNsPrefix("rdf", RDF.getURI())
//        .setNsPrefix("rdfs", RDFS.getURI())
//        .setNsPrefix("dc", DC.getURI())
        .setNsPrefix("tag", TAG.getURI());

    String documentURI = resourceURI("document", resourceCounter.getAndIncrement());
    Resource documentResource = model.createResource(documentURI).addProperty(RDF.type, TAG.Document);
    final TextGraph textGraph = document.getDTO().textGraph;

    Map<String, Resource> layerResources = new HashMap<>();
    textGraph.getLayerNames().forEach(l -> {
      final Resource layerResource = createLayerResource(model, l);
      layerResources.put(l, layerResource);
      documentResource.addProperty(TAG.layer, layerResource);
    });

    Map<Long, Resource> textResources = new HashMap<>();
    AtomicReference<Resource> lastTextResource = new AtomicReference<>();
    textGraph.getTextNodeIdStream()
        .map(document.store::getTextNode)
        .forEach(textNode -> {
          Long id = textNode.getDbId();
          Resource textResource = createTextResource(model, textNode.getText(), id);
          textResources.put(id, textResource);
          lastTextResource.set(textResource);
        });

    Map<Long, Resource> markupResources = new HashMap<>();
    document.getMarkupStream().forEach(markup -> {
      Resource markupResource = createMarkupResource(model, markup);
      if (markupResources.isEmpty()) {
        model.add(documentResource, TAG.root, markupResource);
      }
      markupResources.put(markup.getDbId(), markupResource);
      for (String layer : markup.getLayers()) {
        markupResource.addProperty(TAG.layer, layerResources.get(layer));
      }
      markup.getAnnotationStream()
          .map(ai -> toAnnotationResource(model, ai, document.store))
          .forEach(ar -> markupResource.addProperty(TAG.annotation, ar));
    });

    markupResources.keySet().forEach(markupId -> {
      Resource markupResource = markupResources.get(markupId);
      List<Resource> subElements = new ArrayList<>();
      textGraph.getOutgoingEdges(markupId).stream()
          .filter(LayerEdge.class::isInstance)
          .map(LayerEdge.class::cast)
          .forEach(le -> {
            if (le.hasType(EdgeType.hasText)) {
              textGraph.getTargets(le).forEach(t -> {
                subElements.add(textResources.get(t.longValue()));
              });
            } else if (le.hasType(EdgeType.hasMarkup)) {
              textGraph.getTargets(le).forEach(t -> {
                subElements.add(markupResources.get(t.longValue()));
              });
            }
          });
      RDFList list = model.createList(subElements.iterator());
      markupResource.addProperty(TAG.elements, list);
      // TODO: remove markup-text links for parent layers that cover textnodes that are linked by markup in child layer.
    });

    return model;
  }

  private static Resource createLayerResource(final Model model, final String layerName) {
    String uri = TAG.NS + "layer_" + layerName;
    Resource resource = model.createResource(uri)
        .addProperty(RDF.type, TAG.Layer)
        .addProperty(TAG.layerName, layerName);
    return resource;
  }

  private static Resource createMarkupResource(final Model model, final TAGMarkup markup) {
    String textURI = resourceURI("markup", markup.getDbId());
    Resource resource = model.createResource(textURI)
        .addProperty(RDF.type, TAG.MarkupElement)
        .addProperty(TAG.markupName, markup.getTag());
    return resource;
  }

  public static Resource createTextResource(final Model model, final String text, final Long resourceId) {
    String textURI = resourceURI("text", resourceId);
    Resource textResource = model.createResource(textURI)
        .addProperty(RDF.type, TAG.TextNode);
    Literal content = model.createLiteral(text);
    textResource.addProperty(TAG.content, content);
    return textResource;
  }

  static final Map<AnnotationType, Resource> annotationTypeResources = new HashMap<>();

  static {
    annotationTypeResources.put(AnnotationType.String, TAG.StringAnnotation);
    annotationTypeResources.put(AnnotationType.Boolean, TAG.BooleanAnnotation);
    annotationTypeResources.put(AnnotationType.Number, TAG.NumberAnnotation);
    annotationTypeResources.put(AnnotationType.List, TAG.ListAnnotation);
    annotationTypeResources.put(AnnotationType.Map, TAG.MapAnnotation);
    annotationTypeResources.put(AnnotationType.Reference, TAG.ReferenceAnnotation);
    annotationTypeResources.put(AnnotationType.RichText, TAG.RichTextAnnotation);
  }

  private static Resource toAnnotationResource(Model model, AnnotationInfo annotationInfo, final TAGStore store) {
    String annotationURI = resourceURI("annotation", annotationInfo.getNodeId());
    Resource resource = model.createResource(annotationURI)
        .addProperty(RDF.type, annotationTypeResources.get(annotationInfo.getType()))
        .addProperty(TAG.annotationName, annotationInfo.getName());
    if (annotationInfo.getType().equals(AnnotationType.String)) {
      String value = store.getStringAnnotationValue(annotationInfo.getNodeId()).getValue();
      Literal literal = model.createLiteral(value);
      resource.addProperty(TAG.value, literal);
    } else if (annotationInfo.getType().equals(AnnotationType.Number)) {
      Double value = store.getNumberAnnotationValue(annotationInfo.getNodeId()).getValue();
      Literal literal = model.createTypedLiteral(value);
      resource.addProperty(TAG.value, literal);
    }
    return resource;
  }

  private static Resource toLayerResource(Model model, String layerName) {
    String layerURI = resourceURI("layer", new Random().nextLong());
    Resource resource = model.createResource(layerURI)
        .addProperty(RDF.type, TAG.Layer)
        .addProperty(TAG.layerName, layerName);
    return resource;
  }

  private static String resourceURI(final String type, final Long resourceId) {
    return URI.create(TAG.getURI() + type + resourceId).toASCIIString();
  }
}
