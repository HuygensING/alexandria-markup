package nl.knaw.huygens.alexandria.storage;

/*
 * #%L
 * alexandria-markup-core
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

import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class TAGTextNode {
  private final TAGStore store;
  private final TAGTextNodeDTO textNode;

  public TAGTextNode(TAGStore store, TAGTextNodeDTO textNode) {
    this.store = store;
    this.textNode = textNode;
    update();
  }

  public Long getDbId() {
    return textNode.getDbId();
  }

  public TAGTextNodeDTO getDTO() {
    return textNode;
  }

  public String getText() {
    return textNode.getText();
  }

  public List<TAGTextNode> getNextTextNodes() {
    // TODO: implement here or in TAGDocument
    return new ArrayList<>();
  }

  @Override
  public int hashCode() {
    return textNode.getDbId().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TAGTextNode//
        && ((TAGTextNode) other).getDbId().equals(getDbId());
  }

  @Override
  public String toString() {
    return format("%d:%s", getDbId(), getText());
  }

  private void update() {
    store.persist(textNode);
  }

}
