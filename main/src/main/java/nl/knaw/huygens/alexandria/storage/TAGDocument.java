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

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

import java.util.*;

import static com.sleepycat.persist.model.Relationship.ONE_TO_MANY;

@Entity(version = 2)
public class TAGDocument implements TAGObject {
  // previously: Limen
  @PrimaryKey(sequence = "document_pk_sequence")
  private Long id;

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGTextNode.class)
  private List<Long> textNodeIds = new ArrayList<>();

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGMarkup.class)
  private List<Long> markupIds = new ArrayList<>();

  private final Map<Long, Set<Long>> textNodeIdToMarkupIds = new LinkedHashMap<>();

  private Date creationDate = new Date();
  private Date modificationDate = new Date();

  TAGDocument() {
  }

  public Long getId() {
    return id;
  }

  public List<Long> getTextNodeIds() {
    return textNodeIds;
  }

  public void setTextNodeIds(List<Long> textNodeIds) {
    this.textNodeIds = textNodeIds;
  }

  public List<Long> getMarkupIds() {
    return markupIds;
  }

  public void setMarkupIds(List<Long> markupIds) {
    this.markupIds = markupIds;
  }

  public boolean hasTextNodes() {
    return !getTextNodeIds().isEmpty();
  }

  public Map<Long, Set<Long>> getTextNodeIdToMarkupIds() {
    return textNodeIdToMarkupIds;
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TAGMarkup markup) {
    int textNodeSize = textNodeIds.size();
    return textNodeSize > 2 //
        && markup.getTextNodeIds().size() >= textNodeSize / 2d;
  }

  public Set<Long> getMarkupIdsForTextNodeIds(Long textNodeId) {
    Set<Long> markups = textNodeIdToMarkupIds.get(textNodeId);
    return markups == null ? new LinkedHashSet<>() : markups;
  }

  public void addTextNode(TAGTextNode textNode) {
    textNodeIds.add(textNode.getId());
  }

  public void associateTextWithMarkup(TAGTextNode textNode, TAGMarkup markup) {
    textNodeIdToMarkupIds.computeIfAbsent(textNode.getId(), f -> new LinkedHashSet<>()).add(markup.getId());
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(final Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getModificationDate() {
    return modificationDate;
  }

  public void setModificationDate(final Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  public void updateModificationDate() {
    modificationDate = new Date();
  }
}
