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

@Entity(version = 1)
public class TAGTextNode implements TAGObject {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private Long id;

  private String text;

  private Long prevTextNodeId;
  private Long nextTextNodeId;

  private TAGTextNode() {
  }

  public TAGTextNode(String text) {
    this.text = text;
  }

  public Long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public TAGTextNode setText(String text) {
    this.text = text;
    return this;
  }

  public Long getPrevTextNodeId() {
    return prevTextNodeId;
  }

  public TAGTextNode setPrevTextNodeId(long prevId) {
    this.prevTextNodeId = prevId;
    return this;
  }

  public Long getNextTextNodeId() {
    return nextTextNodeId;
  }

  public TAGTextNode setNextTextNodeId(long nextId) {
    this.nextTextNodeId = nextId;
    return this;
  }

}
