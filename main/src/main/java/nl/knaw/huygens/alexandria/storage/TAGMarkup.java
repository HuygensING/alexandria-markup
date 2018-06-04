package nl.knaw.huygens.alexandria.storage;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkNotNull;

public class TAGMarkup {
  private final TAGStore store;
  private final TAGMarkupDTO markup;

  public TAGMarkup(TAGStore store, TAGMarkupDTO markup) {
    checkNotNull(store);
    checkNotNull(markup);
    this.store = store;
    this.markup = markup;
    update();
  }

  public Long getDbId() {
    return markup.getDbId();
  }

  public String getTag() {
    return markup.getTag();
  }

  public TAGMarkup addTextNode(TAGTextNode tagTextNode) {
    markup.getTextNodeIds().add(tagTextNode.getDbId());
    Long ownerId = markup.getDocumentId();
    new TAGDocument(store, store.getDocumentDTO(ownerId))//
        .associateTextNodeWithMarkup(tagTextNode, this);
    update();
    return this;
  }

  public TAGMarkup setOnlyTextNode(TAGTextNode tagTextNode) {
    markup.getTextNodeIds().clear();
    addTextNode(tagTextNode);
    return this;
  }

  public TAGMarkup setFirstAndLastTextNode(TAGTextNode first, TAGTextNode last) {
    markup.getTextNodeIds().clear();
    addTextNode(first);
    if (!first.getDbId().equals(last.getDbId())) {
      TAGTextNode next = first.getNextTextNodes().get(0); // TODO: handle divergence
      while (!next.getDbId().equals(last.getDbId())) {
        addTextNode(next);
        next = next.getNextTextNodes().get(0);// TODO: handle divergence
      }
      addTextNode(next);
    }
    update();
    return this;
  }

  public TAGMarkup addAnnotation(TAGAnnotation annotation) {
    markup.getAnnotationIds().add(annotation.getDbId());
    update();
    return this;
  }

  public Stream<TAGAnnotation> getAnnotationStream() {
    return markup.getAnnotationIds().stream()//
        .map(store::getAnnotationDTO)//
        .map(annotation -> new TAGAnnotation(store, annotation));
  }

  public Stream<TAGTextNode> getTextNodeStream() {
    return markup.getTextNodeIds().stream()//
        .map(store::getTextNodeDTO)//
        .map(textNode -> new TAGTextNode(store, textNode));
  }

  private void update() {
    store.persist(markup);
  }

  public boolean isAnonymous() {
    return markup.getTextNodeIds().size() == 1//
        && "".equals(getTextNodeStream().findFirst().map(TAGTextNode::getText).get());
  }

  public TAGMarkupDTO getDTO() {
    return markup;
  }

  public void setIsDiscontinuous(final boolean b) {
    markup.setDiscontinuous(b);
  }

  public boolean isDiscontinuous() {
    return markup.isDiscontinuous();
  }

  public boolean isContinuous() {
    return !markup.isDiscontinuous();
  }

  public String getExtendedTag() {
    return markup.getExtendedTag();
  }

  public boolean hasN() {
    return getAnnotationStream()//
        .map(TAGAnnotation::getTag) //
        .anyMatch("n"::equals);
  }

  public String getSuffix() {
    return markup.getSuffix();
  }

  public Optional<TAGMarkup> getDominatedMarkup() {
    return markup.getDominatedMarkupId()
        .map(store::getMarkupDTO)
        .map(m -> new TAGMarkup(store, m));
  }

  public void setDominatedMarkup(TAGMarkup dominatedMarkup) {
    markup.setDominatedMarkupId(dominatedMarkup.getDbId());
    if (!dominatedMarkup.getDTO().getDominatingMarkupId().isPresent()) {
      dominatedMarkup.setDominatingMarkup(this);
    }
    update();
  }

  public Optional<TAGMarkup> getDominatingMarkup() {
    return markup.getDominatingMarkupId()
        .map(store::getMarkupDTO)
        .map(m -> new TAGMarkup(store, m));

  }

  private void setDominatingMarkup(TAGMarkup dominatingMarkup) {
    markup.setDominatingMarkupId(dominatingMarkup.getDbId());
    if (!dominatingMarkup.getDTO().getDominatedMarkupId().isPresent()) {
      dominatingMarkup.setDominatedMarkup(this);
    }
    update();
  }

  public boolean hasMarkupId() {
    return markup.getMarkupId() != null;
  }

  public String getMarkupId() {
    return markup.getMarkupId();
  }

  public boolean isOptional() {
    return markup.isOptional();
  }

  public TAGMarkup setOptional(boolean optional) {
    markup.setOptional(optional);
    return this;
  }

  public TAGMarkup setMarkupId(String id) {
    markup.setMarkupId(id);
    return this;
  }

  public boolean hasTag(String tag) {
    return tag.equals(markup.getTag());
  }

  public void setSuffix(String suffix) {
    markup.setSuffix(suffix);
  }

  public int getTextNodeCount() {
    return markup.getTextNodeIds().size();
  }

  public TAGMarkup addAllLayers(final Set<String> layers) {
    markup.addAllLayers(layers);
    return this;
  }

  public Set<String> getLayers() {
    return markup.getLayers();
  }

  @Override
  public String toString() {
    return markup.toString();
  }

  @Override
  public int hashCode() {
    return markup.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TAGMarkup //
        && markup.equals(((TAGMarkup) other).getDTO());
  }

  public boolean matches(TAGMarkup other) {
    if (!other.getExtendedTag().equals(getExtendedTag())) {
      return false;
    }

    int thisAnnotationCount = markup.getAnnotationIds().size();
    int otherAnnotationCount = other.getDTO().getAnnotationIds().size();
    if (thisAnnotationCount != otherAnnotationCount) {
      return false;
    }

    String thisAnnotationString = annotationsString();
    String otherAnnotationString = other.annotationsString();
    return thisAnnotationString.equals(otherAnnotationString);
  }

  private String annotationsString() {
    StringBuilder annotationsString = new StringBuilder();
    getAnnotationStream().forEach(a ->
        annotationsString.append(" ").append(a.toString())
    );
    return annotationsString.toString();
  }

}
