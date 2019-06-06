package nl.knaw.huygens.alexandria.storage;

/*
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

import nl.knaw.huc.di.tag.model.graph.edges.AnnotationEdge;
import nl.knaw.huc.di.tag.model.graph.edges.ContinuationEdge;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.tagml.TAGML.DEFAULT_LAYER;

public class TAGMarkupDAO {
  private final TAGStore store;
  private final TAGMarkup markupDTO;
  private final TAGDocumentDAO document;

  public TAGMarkupDAO(TAGStore store, TAGMarkup markupDTO) {
    checkNotNull(store);
    checkNotNull(markupDTO);
    this.store = store;
    this.markupDTO = markupDTO;
    this.document = store.getDocument(markupDTO.getDocumentId());
    update();
  }

  public Long getDbId() {
    return markupDTO.getDbId();
  }

  public String getTag() {
    return markupDTO.getTag();
  }

//  @Deprecated
//  public TAGMarkup addTextNode(TAGTextNode tagTextNode) {
//    markupDTO.getTextNodeIds().add(tagTextNode.getDbId());
//    Long ownerId = markupDTO.getDocumentId();
//    new TAGDocument(store, store.getDocumentDTO(ownerId))//
//        .associateTextNodeWithMarkupForLayer(tagTextNode, this, "");
//    update();
//    return this;
//  }

//  public TAGMarkup setOnlyTextNode(TAGTextNode tagTextNode) {
//    markupDTO.getTextNodeIds().clear();
//    addTextNode(tagTextNode);
//    return this;
//  }

//  public TAGMarkup setFirstAndLastTextNode(TAGTextNode first, TAGTextNode last) {
//    markupDTO.getTextNodeIds().clear();
//    addTextNode(first);
//    if (!first.getDbId().equals(last.getDbId())) {
//      TAGTextNode next = first.getNextTextNodes().get(0); // TODO: handle divergence
//      while (!next.getDbId().equals(last.getDbId())) {
//        addTextNode(next);
//        next = next.getNextTextNodes().get(0);// TODO: handle divergence
//      }
//      addTextNode(next);
//    }
//    update();
//    return this;
//  }

  public TAGMarkupDAO addAnnotation(AnnotationInfo annotation) {
//    markupDTO.getAnnotationIds().add(annotation.getDbId());
//    update();
    return this;
  }

  public Stream<AnnotationInfo> getAnnotationStream() {
    Long markupNode = getDbId();
    return document.getDTO().textGraph
        .getOutgoingEdges(markupNode).stream()
        .filter(AnnotationEdge.class::isInstance)
        .map(AnnotationEdge.class::cast)
        .map(this::toAnnotationInfo);
  }

  private AnnotationInfo toAnnotationInfo(final AnnotationEdge annotationEdge) {
    Collection<Long> targets = document.getDTO().textGraph.getTargets(annotationEdge);
    final Long valueNode = targets.iterator().next();
    return new AnnotationInfo(valueNode, annotationEdge.getAnnotationType(), annotationEdge.getField());
  }

  public Stream<TAGTextNodeDAO> getTextNodeStream() {
    return document.getTextNodeStreamForMarkup(this);
  }

  public Stream<TAGTextNodeDAO> getTextNodeStreamForLayers(Set<String> layers) {
    return document.getTextNodeStreamForMarkupInLayers(this, layers);
  }

  public TAGMarkup getDTO() {
    return markupDTO;
  }

  public void setIsDiscontinuous(final boolean b) {
    markupDTO.setDiscontinuous(b);
  }

  public boolean isDiscontinuous() {
    return markupDTO.isDiscontinuous();
  }

  public boolean isContinuous() {
    return !markupDTO.isDiscontinuous();
  }

  public String getExtendedTag() {
    return getExtendedTag(Collections.emptySet());
  }

  public String getExtendedTag(final Set<String> newLayers) {
    String layerSuffix = layerSuffix(newLayers);
    String tag = getTag();
    if (isOptional()) {
      return TAGML.OPTIONAL_PREFIX + tag + layerSuffix;
    }
    String suffix = getSuffix();
    if (StringUtils.isNotEmpty(suffix)) {
      return tag + "~" + suffix + layerSuffix;
    }
    return tag + layerSuffix;
  }

  public boolean hasN() {
    return getAnnotationStream()//
        .map(AnnotationInfo::getName) //
        .anyMatch("n"::equals);
  }

  public String getSuffix() {
    return markupDTO.getSuffix();
  }

  public Optional<TAGMarkupDAO> getDominatedMarkup() {
    return markupDTO.getDominatedMarkupId()
        .map(store::getMarkupDTO)
        .map(m -> new TAGMarkupDAO(store, m));
  }

  public void setDominatedMarkup(TAGMarkupDAO dominatedMarkup) {
    markupDTO.setDominatedMarkupId(dominatedMarkup.getDbId());
    if (!dominatedMarkup.getDTO().getDominatingMarkupId().isPresent()) {
      dominatedMarkup.setDominatingMarkup(this);
    }
    update();
  }

  public Optional<TAGMarkupDAO> getDominatingMarkup() {
    return markupDTO.getDominatingMarkupId()
        .map(store::getMarkupDTO)
        .map(m -> new TAGMarkupDAO(store, m));
  }

  public boolean hasMarkupId() {
    return markupDTO.getMarkupId() != null;
  }

  public String getMarkupId() {
    return markupDTO.getMarkupId();
  }

  public boolean isOptional() {
    return markupDTO.isOptional();
  }

  public TAGMarkupDAO setOptional(boolean optional) {
    markupDTO.setOptional(optional);
    return this;
  }

  public TAGMarkupDAO setMarkupId(String id) {
    markupDTO.setMarkupId(id);
    return this;
  }

  public boolean hasTag(String tag) {
    return tag.equals(markupDTO.getTag());
  }

  public void setSuffix(String suffix) {
    markupDTO.setSuffix(suffix);
  }

  public TAGMarkupDAO addAllLayers(final Set<String> layers) {
    markupDTO.addAllLayers(layers);
    return this;
  }

  public Set<String> getLayers() {
    return markupDTO.getLayers();
  }

  public boolean isAnonymous() {
    List<TAGTextNodeDAO> textNodesForMarkup = document.getTextNodeStreamForMarkup(this)
        .collect(toList());
    return textNodesForMarkup.size() == 1 // markup has just 1 textnode
        && textNodesForMarkup.get(0).getText().isEmpty();  // and it's empty
  }

  public boolean isSuspended() {
    return document.getDTO().textGraph
        .getOutgoingEdges(getDbId())
        .stream()
        .anyMatch(ContinuationEdge.class::isInstance);
  }

  public boolean isResumed() {
    return document.getDTO().textGraph
        .getIncomingEdges(getDbId())
        .stream()
        .anyMatch(ContinuationEdge.class::isInstance);
  }

  public boolean matches(TAGMarkupDAO other) {
    if (!other.getExtendedTag().equals(getExtendedTag())) {
      return false;
    }

//    int thisAnnotationCount = markupDTO.getAnnotationIds().size();
//    int otherAnnotationCount = other.getDTO().getAnnotationIds().size();
//    if (thisAnnotationCount != otherAnnotationCount) {
//      return false;
//    }

    String thisAnnotationString = annotationsString();
    String otherAnnotationString = other.annotationsString();
    return thisAnnotationString.equals(otherAnnotationString);
  }

  public boolean hasAnnotation(String key) {
    return getAnnotationStream()
        .map(AnnotationInfo::getName)
        .anyMatch(key::equals);
  }

  public AnnotationInfo getAnnotation(String key) {
    return getAnnotationStream()
        .filter(a -> a.hasName(key))
        .findAny().get();
  }

  @Override
  public String toString() {
    return markupDTO.toString();
  }

  @Override
  public int hashCode() {
    return markupDTO.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TAGMarkupDAO //
        && markupDTO.equals(((TAGMarkupDAO) other).getDTO());
  }

  private void update() {
    store.persist(markupDTO);
  }

  private void setDominatingMarkup(TAGMarkupDAO dominatingMarkup) {
    markupDTO.setDominatingMarkupId(dominatingMarkup.getDbId());
    if (!dominatingMarkup.getDTO().getDominatedMarkupId().isPresent()) {
      dominatingMarkup.setDominatedMarkup(this);
    }
    update();
  }

  private String annotationsString() {
    StringBuilder annotationsString = new StringBuilder();
    getAnnotationStream().forEach(a ->
        annotationsString.append(" ").append(a.toString())
    );
    return annotationsString.toString();
  }

  String layerSuffix(final Set<String> newLayers) {
    String layerSuffix = getLayers().stream()
        .filter(l -> !DEFAULT_LAYER.equals(l))
        .map(l -> newLayers.contains(l) ? "+" + l : l)
        .collect(joining(","));
    return layerSuffix.isEmpty() ? "" : TAGML.DIVIDER + layerSuffix;
  }

  // TODO: refactor links to these dummy methods
  @Deprecated
  public int getTextNodeCount() {
    return 0;
  }

  @Deprecated
  public TAGMarkupDAO setOnlyTextNode(final TAGTextNodeDAO t1) {
    return this;
  }

  @Deprecated
  public TAGMarkupDAO setFirstAndLastTextNode(final TAGTextNodeDAO tn00, final TAGTextNodeDAO tn10) {
    return this;
  }

}
