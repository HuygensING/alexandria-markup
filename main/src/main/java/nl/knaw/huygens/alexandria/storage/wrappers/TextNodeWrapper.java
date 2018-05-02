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

import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.convergence;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.divergence;

public class TextNodeWrapper {
  private final TAGStore store;
  private final TAGTextNode textNode;

  public TextNodeWrapper(TAGStore store, TAGTextNode textNode) {
    this.store = store;
    this.textNode = textNode;
    update();
  }

  public Long getDbId() {
    return textNode.getDbId();
  }

  public String getText() {
    return textNode.getText();
  }

  public TAGTextNode getTextNode() {
    return textNode;
  }

  public TextNodeWrapper addPreviousTextNode(TextNodeWrapper previousTextNode) {
    textNode.addPrevTextNodeId(previousTextNode.getDbId());
    if (previousTextNode.getNextTextNodes().isEmpty() || previousTextNode.isDivergence()) {
      previousTextNode.addNextTextNode(this);
    }
    update();
    return this;
  }

  public void addNextTextNode(final TextNodeWrapper nextTextNode) {
    Long id = nextTextNode.getDbId();
    textNode.addNextTextNodeId(id);
    update();
  }

  public List<TextNodeWrapper> getNextTextNodes() {
    return textNode.getNextTextNodeIds()
        .stream()
        .map(store::getTextNodeWrapper)
        .collect(toList());
  }

  public List<TextNodeWrapper> getPrevTextNodes() {
    return textNode.getPrevTextNodeIds()
        .stream()
        .map(store::getTextNodeWrapper)
        .collect(toList());
  }

  public boolean isDivergence() {
    return divergence.equals(textNode.getType());
  }

  public boolean isConvergence() {
    return convergence.equals(textNode.getType());
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
        && ((TextNodeWrapper) other).getDbId().equals(getDbId());
  }

  @Override
  public String toString() {
    String prefix = getDbId() + ":";
    if (isDivergence()) {
      return prefix + TAGML.DIVERGENCE;
    }
    if (isConvergence()) {
      return prefix + TAGML.CONVERGENCE;
    }
    return prefix + getText();
  }
}
