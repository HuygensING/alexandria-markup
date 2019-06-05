package nl.knaw.huc.di.tag.tagml.importer;

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
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

public class TAGModelBuilderImpl implements TAGModelBuilder {

  private final TAGStore store;
  private TAGDocument document;
  private ErrorListener errorListener;
  private final AnnotationFactory annotationFactory;
  private final Deque<TAGDocument> documentStack = new ArrayDeque<>(); // TODO: move to state

  public TAGModelBuilderImpl(final TAGStore store, ErrorListener errorListener) { // TODO: fix errorListener dependency
    this.store = store;
    this.document = store.createDocument();
    this.errorListener = errorListener;
    this.annotationFactory = new AnnotationFactory(store, document.getDTO().textGraph, errorListener);
  }

  public TAGDocument getDocument() {
    return document;
  }

  @Override
  public Long persist(final TAGDTO tagdto) {
    return store.persist(tagdto);
  }

  @Override
  public ErrorListener getErrorListener() {
    return errorListener;
  }

  @Override
  public void exitDocument(final Map<String, String> namespaces) {
    document.removeDefaultLayerIfUnused();
    document.linkParentlessLayerRootsToDocument();
    document.setNamespaces(namespaces);
    update(document.getDTO());
  }

  @Override
  public void exitText(final String text, final Deque<TAGMarkup> allOpenMarkup) {
    createConnectedTextNode(text, allOpenMarkup);
  }

  @Override
  public TAGTextNode createConnectedTextNode(final String text, final Deque<TAGMarkup> allOpenMarkup) {
    TAGTextNode tn = store.createTextNode(text);
    addAndConnectToMarkup(tn, allOpenMarkup);
    return tn;
  }

  @Override
  public boolean isFirstTag() {
    return !document.getLayerNames().contains(TAGML.DEFAULT_LAYER);
  }

  @Override
  public void addLayer(final String newLayerId, final TAGMarkup markup, final String parentLayer) {
    document.addLayer(newLayerId, markup, parentLayer);
  }

  @Override
  public void openMarkupInLayer(final TAGMarkup markup, final String layerId) {
    document.openMarkupInLayer(markup, layerId);
  }

  @Override
  public void closeMarkupInLayer(final TAGMarkup markup, final String layerName) {
    document.closeMarkupInLayer(markup, layerName);
  }

  @Override
  public TAGMarkup getMarkup(final Long rootMarkupId) {
    return store.getMarkup(rootMarkupId);
  }

  @Override
  public TAGTextNode getLastTextNode() {
    return document.getLastTextNode();
  }

  @Override
  public Stream<TAGMarkup> getMarkupStreamForTextNode(final TAGTextNode previousTextNode) {
    return document.getMarkupStreamForTextNode(previousTextNode);
  }

  @Override
  public TAGMarkup resumeMarkup(final TAGMarkup suspendedMarkup, final Set<String> layers) {
    TextGraph textGraph = document.getDTO().textGraph;
    TAGMarkup resumedMarkup = store.createMarkup(document, suspendedMarkup.getTag()).addAllLayers(layers);
    document.addMarkup(resumedMarkup);
    update(resumedMarkup.getDTO());
    textGraph.continueMarkup(suspendedMarkup, resumedMarkup);
    return resumedMarkup;

  }

  @Override
  public void associateTextNodeWithMarkupForLayer(final TAGTextNode tn, final TAGMarkup markup, final String layerName) {
    document.associateTextNodeWithMarkupForLayer(tn, markup, layerName);
  }

  @Override
  public void addRefAnnotation(final TAGMarkup markup, final String aName, final String refId) {
    AnnotationInfo annotationInfo = annotationFactory.makeReferenceAnnotation(aName, refId);
    Long markupNode = markup.getDbId();
    document.getDTO().textGraph.addAnnotationEdge(markupNode, annotationInfo);
  }

  @Override
  public void addBasicAnnotation(final TAGMarkup markup, final TAGMLParser.BasicAnnotationContext actx) {
    AnnotationInfo aInfo = annotationFactory.makeAnnotation(actx);
    Long markupNode = markup.getDbId();
    document.getDTO().textGraph.addAnnotationEdge(markupNode, aInfo);
  }

  @Override
  public TAGMarkup createMarkup(final String extendedTag) {
    return store.createMarkup(document, extendedTag);
  }

  @Override
  public void addMarkup(final TAGMarkup markup) {
    document.addMarkup(markup);
  }

  @Override
  public void persist(final TAGMarkup markup) {
    store.persist(markup.getDTO());
  }

  @Override
  public void enterRichTextValue() {
    documentStack.push(document);
    document = store.createDocument();

  }

  @Override
  public void exitRichTextValue() {
    document = documentStack.pop();
  }

  @Override
  public TAGMarkup addMarkup(final String tagName) {
    TAGMarkup markup = store.createMarkup(document, tagName);
    document.addMarkup(markup);
    return markup;
  }

  private Long update(TAGDTO tagdto) {
    return store.persist(tagdto);
  }

  private void addAndConnectToMarkup(final TAGTextNode tn, final Deque<TAGMarkup> allOpenMarkup) {
    List<TAGMarkup> relevantMarkup = getRelevantOpenMarkup(allOpenMarkup);
    document.addTextNode(tn, relevantMarkup);
  }

  @Override
  public List<TAGMarkup> getRelevantOpenMarkup(Deque<TAGMarkup> allOpenMarkup) {
    List<TAGMarkup> relevantMarkup = new ArrayList<>();
    if (!allOpenMarkup.isEmpty()) {
      Set<String> handledLayers = new HashSet<>();
      for (TAGMarkup m : allOpenMarkup) {
        Set<String> layers = m.getLayers();
        boolean markupHasNoHandledLayer = layers.stream().noneMatch(handledLayers::contains);
        if (markupHasNoHandledLayer) {
          relevantMarkup.add(m);
          handledLayers.addAll(layers);
          boolean goOn = true;
          while (goOn) {
            Set<String> newParentLayers = handledLayers.stream()
                .map(l -> document.getDTO().textGraph.getParentLayerMap().get(l))
                .filter(l -> !handledLayers.contains(l))
                .filter(l -> !TAGML.DEFAULT_LAYER.equals(l)) // Once again, the default layer is special! TODO: fix default layer usage
                .collect(toSet());
            handledLayers.addAll(newParentLayers);
            goOn = !newParentLayers.isEmpty();
          }
        }
      }
    }
    return relevantMarkup;
  }

}
