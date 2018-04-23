package nl.knaw.huygens.alexandria.storage;

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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity(version = 2)
public class TAGTextNode implements TAGObject {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private Long dbId;

  private TAGTextNodeType type;

  private String text;

  private List<Long> prevTextNodeIds = new ArrayList<>();
  private List<Long> nextTextNodeIds = new ArrayList<>();

  private TAGTextNode() {
  }

  public TAGTextNode(String text) {
    this.text = text;
    this.type = TAGTextNodeType.plaintext;
  }

  public TAGTextNode(TAGTextNodeType type) {
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

  public TAGTextNode setText(String text) {
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

  public void setPrevTextNodeId(final Long prevTextNodeId) {
    if (type.equals(TAGTextNodeType.convergence)) {
      throw new RuntimeException("Use addPrevTextNodeId(prevTextNodeId) for convergence nodes.");
    }
    this.prevTextNodeIds.clear();
    this.prevTextNodeIds.add(prevTextNodeId);
  }

  public void addPrevTextNodeId(final Long prevTextNodeId) {
    if (!type.equals(TAGTextNodeType.convergence)) {
      throw new RuntimeException("Use setPrevTextNodeId(prevTextNodeId) for " + type + " nodes.");
    }
    this.prevTextNodeIds.add(prevTextNodeId);
  }

  public Long getPrevTextNodeId() {
    if (type.equals(TAGTextNodeType.convergence)) {
      throw new RuntimeException("Use getPrevTextNodeIds() for convergence nodes.");
    }
    return prevTextNodeIds.isEmpty() ? null : prevTextNodeIds.get(0);
  }

  public List<Long> getPrevTextNodeIds() {
    return prevTextNodeIds;
  }

  // link to next nodes
  public void setNextTextNodeIds(final List<Long> nextTextNodeIds) {
    this.nextTextNodeIds = nextTextNodeIds;
  }

  public void setNextTextNodeId(final Long nextTextNodeId) {
    if (type.equals(TAGTextNodeType.divergence)) {
      throw new RuntimeException("Use addNextTextNodeId(nextTextNodeId) for divergence nodes.");
    }
    this.nextTextNodeIds.clear();
    this.nextTextNodeIds.add(nextTextNodeId);
  }

  public void addNextTextNodeId(final Long prevTextNodeId) {
    if (!type.equals(TAGTextNodeType.divergence)) {
      throw new RuntimeException("Use setNextTextNodeId(nextTextNodeId) for " + type + " nodes.");
    }
    this.nextTextNodeIds.add(prevTextNodeId);
  }

  public Long getNextTextNodeId() {
    if (type.equals(TAGTextNodeType.convergence)) {
      throw new RuntimeException("Use getNextTextNodeIds() for convergence nodes.");
    }
    return prevTextNodeIds.isEmpty() ? null : prevTextNodeIds.get(0);
  }

  public List<Long> getNextTextNodeIds() {
    return nextTextNodeIds;
  }
}
