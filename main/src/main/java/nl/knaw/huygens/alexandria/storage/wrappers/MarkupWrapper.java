package nl.knaw.huygens.alexandria.storage.wrappers;

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

import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MarkupWrapper {
  private final TAGStore store;
  private final TAGMarkup markup;

  public MarkupWrapper(TAGStore store, TAGMarkup markup) {
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

  public MarkupWrapper addTextNode(TextNodeWrapper textNodeWrapper) {
    markup.getTextNodeIds().add(textNodeWrapper.getDbId());
    Long ownerId = markup.getDocumentId();
    new DocumentWrapper(store, store.getDocument(ownerId))//
        .associateTextNodeWithMarkup(textNodeWrapper, this);
    update();
    return this;
  }

  public MarkupWrapper setOnlyTextNode(TextNodeWrapper textNodeWrapper) {
    markup.getTextNodeIds().clear();
    addTextNode(textNodeWrapper);
    return this;
  }

  public MarkupWrapper setFirstAndLastTextNode(TextNodeWrapper first, TextNodeWrapper last) {
    markup.getTextNodeIds().clear();
    addTextNode(first);
    if (!first.getDbId().equals(last.getDbId())) {
      TextNodeWrapper next = first.getNextTextNodes().get(0); // TODO: handle divergence
      while (!next.getDbId().equals(last.getDbId())) {
        addTextNode(next);
        next = next.getNextTextNodes().get(0);// TODO: handle divergence
      }
      addTextNode(next);
    }
    update();
    return this;
  }

  public MarkupWrapper addAnnotation(AnnotationWrapper annotation) {
    markup.getAnnotationIds().add(annotation.getDbId());
    update();
    return this;
  }

  public Stream<AnnotationWrapper> getAnnotationStream() {
    return markup.getAnnotationIds().stream()//
        .map(store::getAnnotation)//
        .map(annotation -> new AnnotationWrapper(store, annotation));
  }

  public Stream<TextNodeWrapper> getTextNodeStream() {
    return markup.getTextNodeIds().stream()//
        .map(store::getTextNode)//
        .map(textNode -> new TextNodeWrapper(store, textNode));
  }

  private void update() {
    store.persist(markup);
  }

  public boolean isAnonymous() {
    return markup.getTextNodeIds().size() == 1//
        && "".equals(getTextNodeStream().findFirst().map(TextNodeWrapper::getText).get());
  }

  public TAGMarkup getMarkup() {
    return markup;
  }

  public boolean isContinuous() {
    boolean isContinuous = true;
    List<TextNodeWrapper> textNodes = getTextNodeStream().collect(Collectors.toList());
    TextNodeWrapper textNode = textNodes.get(0);
    TextNodeWrapper expectedNext = textNode.getNextTextNodes().get(0); // TODO: handle divergence
    for (int i = 1; i < textNodes.size(); i++) {
      textNode = textNodes.get(i);
      if (!textNode.equals(expectedNext)) {
        isContinuous = false;
        break;
      }
      expectedNext = textNode.getNextTextNodes().get(0);// TODO: handle divergence
    }
    return isContinuous;
  }

  public String getExtendedTag() {
    return markup.getExtendedTag();
  }

  public boolean hasN() {
    return getAnnotationStream()//
        .map(AnnotationWrapper::getTag) //
        .anyMatch("n"::equals);
  }

  public String getSuffix() {
    return markup.getSuffix();
  }

  public Optional<MarkupWrapper> getDominatedMarkup() {
    return markup.getDominatedMarkupId()
        .map(store::getMarkup)
        .map(m -> new MarkupWrapper(store, m));
  }

  public void setDominatedMarkup(MarkupWrapper dominatedMarkup) {
    markup.setDominatedMarkupId(dominatedMarkup.getDbId());
    if (!dominatedMarkup.getMarkup().getDominatingMarkupId().isPresent()) {
      dominatedMarkup.setDominatingMarkup(this);
    }
    update();
  }

  public Optional<MarkupWrapper> getDominatingMarkup() {
    return markup.getDominatingMarkupId()
        .map(store::getMarkup)
        .map(m -> new MarkupWrapper(store, m));

  }

  private void setDominatingMarkup(MarkupWrapper dominatingMarkup) {
    markup.setDominatingMarkupId(dominatingMarkup.getDbId());
    if (!dominatingMarkup.getMarkup().getDominatedMarkupId().isPresent()) {
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

  public MarkupWrapper setOptional(boolean optional) {
    markup.setOptional(optional);
    return this;
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
    return other instanceof MarkupWrapper //
        && markup.equals(((MarkupWrapper) other).getMarkup());
  }

  public MarkupWrapper setMarkupId(String id) {
    markup.setMarkupId(id);
    return this;
  }

  public boolean hasTag(String tag) {
    return tag.equals(markup.getTag());
  }

  public void setSuffix(String suffix) {
    markup.setSuffix(suffix);
  }
}
