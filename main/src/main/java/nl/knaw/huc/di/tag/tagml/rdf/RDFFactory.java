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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.EdgeType;
import nl.knaw.huc.di.tag.model.graph.edges.LayerEdge;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationFactory;
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
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
    AnnotationFactory annotationFactory = new AnnotationFactory(document.store, textGraph);

    Map<String, Resource> layerResources = new HashMap<>();
    textGraph.getLayerNames().forEach(l -> {
      final Resource layerResource = createLayerResource(model, l);
      layerResources.put(l, layerResource);
      documentResource.addProperty(TAG.layer, layerResource);
    });

    Map<Long, Resource> markupResources = new HashMap<>();
    Multimap<Long, String> layersForMarkup = ArrayListMultimap.create();
    Map<Long, Long> continuedMarkupId = new HashMap<>();
    document.getMarkupStream().forEach(markup -> {
      Long id = markup.getDbId();
      if (markup.isSuspended()) {
        continuedMarkupId.put(id, textGraph.getContinuedMarkupId(id).get());
      }
      Resource markupResource = createMarkupResource(model, markup);
      if (markupResources.isEmpty()) {
        model.add(documentResource, TAG.root, markupResource);
      }
      markupResources.put(id, markupResource);
      Set<String> layers = markup.getLayers();
      layersForMarkup.putAll(id, layers);
      for (String layer : layers) {
        markupResource.addProperty(TAG.layer, layerResources.get(layer));
      }
      markup.getAnnotationStream()
          .map(ai -> toAnnotationResource(model, ai, document.store, annotationFactory))
          .forEach(ar -> markupResource.addProperty(TAG.annotation, ar));
    });

    Map<Long, Resource> textResources = new HashMap<>();
    AtomicReference<Resource> lastTextResource = new AtomicReference<>();
    Multimap<Long, String> layersForTextNode = ArrayListMultimap.create();
    textGraph.getTextNodeIdStream()
        .map(document.store::getTextNode)
        .forEach(textNode -> {
          Long id = textNode.getDbId();
          Resource textResource = createTextResource(model, textNode.getText(), id);
          textResources.put(id, textResource);
          lastTextResource.set(textResource);
          List<String> relevantLayers = determineRelevantLayers(textGraph, id, layersForMarkup);
          layersForTextNode.putAll(id, relevantLayers);
        });

    markupResources.keySet().forEach(markupId -> {
      Resource markupResource = markupResources.get(markupId);
      List<Resource> subElements = new ArrayList<>();
      textGraph.getOutgoingEdges(markupId).stream()
          .filter(LayerEdge.class::isInstance)
          .map(LayerEdge.class::cast)
          .forEach(le -> {
            String layerName = le.getLayerName();
            if (le.hasType(EdgeType.hasText)) {
              textGraph.getTargets(le).stream()
                  .filter(tId -> layersForTextNode.get(tId).contains(layerName))
                  .forEach(tId -> subElements.add(textResources.get(tId)));
            } else if (le.hasType(EdgeType.hasMarkup)) {
              textGraph.getTargets(le).forEach(t -> subElements.add(markupResources.get(t)));
            }
          });
      RDFList list = model.createList(subElements.iterator());
      markupResource.addProperty(TAG.elements, list);
    });

    // connect discontinuous markup
    continuedMarkupId.forEach((suspend, resume) -> {
      Resource suspended = markupResources.get(suspend);
      Resource resumed = markupResources.get(resume);
      suspended.addProperty(TAG.continued, resumed);
    });

    return model;
  }

  private static List<String> determineRelevantLayers(TextGraph textGraph, Long id, Multimap<Long, String> layersForMarkup) {
    // returns all the layers to which this textnode belongs through its markup, leaving out parent layers if a child layer is included
    Set<String> rawLayers = textGraph.getIncomingEdges(id).stream()
        .filter(LayerEdge.class::isInstance)
        .map(LayerEdge.class::cast)
        .filter(e -> e.hasType(EdgeType.hasText))
        .map(textGraph::getSource)
        .map(layersForMarkup::get)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
    List<String> list = new ArrayList<>(rawLayers);
    // Now remove those layers that are parents of other layers in the list.
    Map<String, String> parentLayerMap = textGraph.getParentLayerMap();
    rawLayers.forEach(l -> {
      String parentLayer = parentLayerMap.get(l);
      list.remove(parentLayer);
    });
    return list;
  }

  private static Resource createLayerResource(final Model model, final String layerName) {
    String uri = TAG.NS + "layer_" + layerName;
    return model.createResource(uri)
        .addProperty(RDF.type, TAG.Layer)
        .addProperty(TAG.layerName, layerName);
  }

  private static Resource createMarkupResource(final Model model, final TAGMarkup markup) {
    String textURI = resourceURI("markup", markup.getDbId());
    return model.createResource(textURI)
        .addProperty(RDF.type, TAG.MarkupElement)
        .addProperty(TAG.markupName, markup.getTag());
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

  private static Resource toAnnotationResource(Model model, AnnotationInfo annotationInfo, final TAGStore store, AnnotationFactory annotationFactory) {
    String annotationURI = resourceURI("annotation", annotationInfo.getNodeId());
    Resource resource = model.createResource(annotationURI)
        .addProperty(RDF.type, annotationTypeResources.get(annotationInfo.getType()));
    String name = annotationInfo.getName();
    if (!name.isEmpty()) {
      resource.addProperty(TAG.annotationName, name);
    }
    if (annotationInfo.getType().equals(AnnotationType.String)) {
      String value = store.getStringAnnotationValue(annotationInfo.getNodeId()).getValue();
      Literal literal = model.createLiteral(value);
      resource.addProperty(TAG.value, literal);
    } else if (annotationInfo.getType().equals(AnnotationType.Number)) {
      Double value = store.getNumberAnnotationValue(annotationInfo.getNodeId()).getValue();
      Literal literal = model.createTypedLiteral(value);
      resource.addProperty(TAG.value, literal);
    } else if (annotationInfo.getType().equals(AnnotationType.Boolean)) {
      Boolean value = store.getBooleanAnnotationValue(annotationInfo.getNodeId()).getValue();
      Literal literal = model.createTypedLiteral(value);
      resource.addProperty(TAG.value, literal);
    } else if (annotationInfo.getType().equals(AnnotationType.List)) {
      Iterator<Resource> iterator = annotationFactory.getListValue(annotationInfo).stream()
          .map(ai -> toAnnotationResource(model, ai, store, annotationFactory))
          .collect(toList())
          .iterator();
      RDFList list = model.createList(iterator);
      resource.addProperty(TAG.value, list);

    } else {
      throw new RuntimeException("Unhandled AnnotationType: " + annotationInfo.getType());
    }
    return resource;
  }

  private static Resource toLayerResource(Model model, String layerName) {
    String layerURI = resourceURI("layer", new Random().nextLong());
    return model.createResource(layerURI)
        .addProperty(RDF.type, TAG.Layer)
        .addProperty(TAG.layerName, layerName);
  }

  private static String resourceURI(final String type, final Long resourceId) {
    return URI.create(TAG.getURI() + type + resourceId).toASCIIString();
  }
}
