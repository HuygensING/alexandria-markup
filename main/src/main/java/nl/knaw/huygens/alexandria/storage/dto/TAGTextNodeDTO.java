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

import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.plaintext;

@Entity(version = 2)
public class TAGTextNodeDTO implements TAGDTO {
  @PrimaryKey(sequence = "tgnode_pk_sequence")
  private Long dbId;

  private TAGTextNodeType type;

  private String text;

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

  @Deprecated
  public Object getPrevTextNodeIds() {
    return null;
  }

  @Deprecated
  public Object getNextTextNodeIds() {
    return null;
  }
}
