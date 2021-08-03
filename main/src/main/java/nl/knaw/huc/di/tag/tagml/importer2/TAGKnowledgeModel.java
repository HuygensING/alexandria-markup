package nl.knaw.huc.di.tag.tagml.importer2;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class TAGKnowledgeModel {
  private final Model model;
  final AtomicLong resourceCounter = new AtomicLong();
  private Multimap<MarkupResource, String> layersForMarkup = ArrayListMultimap.create();
  private Map<String, String> parentLayers = new HashMap<>();
  private List<String> layerNames = new ArrayList<>();
  private Map<Long, Resource> resourceMap = new HashMap<>();
  private final Map<String, Deque<MarkupResource>> openMarkupStackForLayer = new HashMap<>();

  TAGKnowledgeModel() {
    model =
        ModelFactory.createDefaultModel()
            .setNsPrefix("rdf", RDF.getURI())
            .setNsPrefix("rdfs", RDFS.getURI())
            //        .setNsPrefix("dc", DC.getURI())
            .setNsPrefix("tag", TAG.getURI());
  }

  public DocumentResource createDocument() {
    String documentURI = resourceURI(resourceCounter.getAndIncrement(), "document");
    Resource resource = model.createResource(documentURI).addProperty(RDF.type, TAG.Document);
    return new DocumentResource(resource);
  }

  public TextResource createTextResource(final String text) {
    String textURI = resourceURI(resourceCounter.getAndIncrement(), "text");
    Resource textResource = model.createResource(textURI).addProperty(RDF.type, TAG.TextNode);
    Literal content = model.createLiteral(text);
    textResource.addProperty(RDF.value, content);
    return new TextResource(textResource);
  }

  public MarkupResource createMarkupResource(final String tag) {
    Long resourceId = resourceCounter.getAndIncrement();
    String markupURI = resourceURI(resourceId, "markup");
    Resource resource = model.createResource(markupURI).addProperty(RDF.type, TAG.MarkupNode);
    MarkupResource markupResource = new MarkupResource(resource);
    markupResource.setResourceId(resourceId);
    markupResource.setTag(tag);
    markupResource.setExtendedTag(tag);
    markupResource.addProperty(TAG.markupName, tag);
    return markupResource;
  }

  private String resourceURI(final Long resourceId, final String type) {
    return URI.create(TAG.getURI() + type + resourceId).toASCIIString();
  }

  public void connectTextNodeAndMarkup(final Resource textResource, final Resource markupResource) {
    model.add(markupResource, TAG.elements, textResource);
  }

  public Set<String> getLayers(final MarkupResource m) {
    return new HashSet(layersForMarkup.get(m));
  }

  public Map<String, String> getParentLayerMap() {
    return parentLayers;
  }

  public List<String> getLayerNames() {
    return layerNames;
  }

  public void addLayer(
      final String layerName, final MarkupResource rootMarkup, final String parentLayer) {
    layerNames.add(layerName);
    openMarkupStackForLayer.put(layerName, new ArrayDeque<>());
    openMarkupStackForLayer.get(layerName).push(rootMarkup);
    if (parentLayer != null) {
      Deque<MarkupResource> openMarkupStack = openMarkupStackForLayer.get(parentLayer);
      linkToParentMarkup(rootMarkup, parentLayer, openMarkupStack);
    }
  }

  private void linkToParentMarkup(
      final MarkupResource rootMarkup,
      final String parentLayer,
      final Deque<MarkupResource> openMarkupStack) {
    if (openMarkupStack != null && !openMarkupStack.isEmpty()) {
      Long parentMarkupId = openMarkupStack.peek().getResourceId();
      Long childMarkupId = rootMarkup.getResourceId();
      if (!Objects.equals(parentMarkupId, childMarkupId)) {
        //        boolean edgeExists = textGraph.getOutgoingEdges(parentMarkupId)
        //            .stream()
        //            .filter(LayerEdge.class::isInstance)
        //            .map(LayerEdge.class::cast)
        //            .filter(e -> e.hasLayer(parentLayer))
        //            .anyMatch(e -> {
        //              Collection<Long> targets = textGraph.getTargets(e);
        //              return targets.size() == 1 && targets.contains(childMarkupId);
        //            });
        //        if (!edgeExists) {
        //          textGraph.addChildMarkup(parentMarkupId, parentLayer, childMarkupId);
        //        }
      }
    }
  }

  public void openMarkupInLayer(final MarkupResource markup, final String layerId) {}

  public void addAllLayers(final MarkupResource markup, final Set<String> layers) {
    layersForMarkup.putAll(markup, layers);
  }

  public MarkupResource getMarkup(final Long resourceId) {
    return (MarkupResource) resourceMap.get(resourceId);
  }

  public void closeMarkupInLayer(final MarkupResource markup, final String layerName) {}

  public Long createStringAnnotationValue(final String value) {
    return null;
  }

  public Long createBooleanAnnotationValue(final Boolean value) {
    return null;
  }

  public Long createNumberAnnotationValue(final Double value) {
    return null;
  }

  public Long createReferenceValue(final String value) {
    return null;
  }

  public ListAnnotationValueResource createListAnnotationValue() {
    return null;
  }
}
