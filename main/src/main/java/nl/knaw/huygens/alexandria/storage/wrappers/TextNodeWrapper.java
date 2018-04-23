package nl.knaw.huygens.alexandria.storage.wrappers;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class TextNodeWrapper {
  private final TAGStore store;
  private final TAGTextNode textNode;

  public TextNodeWrapper(TAGStore store, TAGTextNode textNode) {
    this.store = store;
    this.textNode = textNode;
    update();
  }

  public Long getId() {
    return textNode.getDbId();
  }

  public String getText() {
    return textNode.getText();
  }

  public TAGTextNode getTextNode() {
    return textNode;
  }

  public TextNodeWrapper setPreviousTextNode(TextNodeWrapper previousTextNode) {
    textNode.setPrevTextNodeId(previousTextNode.getId());
    if (previousTextNode.getNextTextNode() == null) {
      previousTextNode.setNextTextNode(this);
    }
    update();
    return this;
  }

  public void setNextTextNode(TextNodeWrapper nextTextNode) {
    textNode.setNextTextNodeId(nextTextNode.getId());
    update();
  }

  public TextNodeWrapper getNextTextNode() {
    Long nextTextNodeId = textNode.getNextTextNodeId();
    if (nextTextNodeId == null) {
      return null;
    }
    return store.getTextNodeWrapper(nextTextNodeId);
  }

  public List<TextNodeWrapper> getNextTextNodes() {
    return textNode.getNextTextNodeIds()
        .stream()
        .map(store::getTextNodeWrapper)
        .collect(toList());
  }

  private void update() {
    store.persist(textNode);
  }

  @Override
  public int hashCode() {
    return textNode.getDbId().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TextNodeWrapper//
        && ((TextNodeWrapper) other).getId().equals(getId());
  }

}
