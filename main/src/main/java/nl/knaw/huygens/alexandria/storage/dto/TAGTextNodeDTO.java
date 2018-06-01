package nl.knaw.huygens.alexandria.storage.dto;

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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import nl.knaw.huygens.alexandria.storage.TAGTextNodeType;

import java.util.ArrayList;
import java.util.List;

import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.*;

@Entity(version = 2)
public class TAGTextNodeDTO implements TAGDTO {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private Long dbId;

  private TAGTextNodeType type;

  private String text;

  private List<Long> prevTextNodeIds = new ArrayList<>();
  private List<Long> nextTextNodeIds = new ArrayList<>();

  private TAGTextNodeDTO() {
  }

  public TAGTextNodeDTO(String text) {
    this.text = text;
    this.type = plaintext;
  }

  public TAGTextNodeDTO(TAGTextNodeType type) {
    this.text = "";
    this.type = type;
  }

  public Long getDbId() {
    return dbId;
  }

  public void setType(final TAGTextNodeType type) {
    this.type = type;
  }

  public TAGTextNodeType getType() {
    return type;
  }

  public TAGTextNodeDTO setText(String text) {
    this.text = text;
    return this;
  }

  public String getText() {
    return text;
  }

  // link to previous nodes
  public void setPrevTextNodeIds(final List<Long> prevTextNodeIds) {
    this.prevTextNodeIds = prevTextNodeIds;
  }

  public void addPrevTextNodeId(final Long prevTextNodeId) {
    if (!convergence.equals(type) && !prevTextNodeIds.isEmpty()) {
      throw new RuntimeException(type + " nodes may have at most 1 prevTextNode.");
    }
    this.prevTextNodeIds.add(prevTextNodeId);
  }

  public List<Long> getPrevTextNodeIds() {
    return prevTextNodeIds;
  }

  // link to next nodes
  public void setNextTextNodeIds(final List<Long> nextTextNodeIds) {
    this.nextTextNodeIds = nextTextNodeIds;
  }

  public void addNextTextNodeId(final Long nextTextNodeId) {
    if (!divergence.equals(type) && !this.nextTextNodeIds.isEmpty()) {
      throw new RuntimeException(type + " nodes may have at most 1 nextTextNode.");
    }
    this.nextTextNodeIds.add(nextTextNodeId);
  }

  public List<Long> getNextTextNodeIds() {
    return nextTextNodeIds;
  }
}
