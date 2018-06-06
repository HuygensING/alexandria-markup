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
import com.sleepycat.persist.model.SecondaryKey;
import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

import static com.sleepycat.persist.model.Relationship.ONE_TO_MANY;

@Entity(version = 3)
public class TAGDocumentDTO implements TAGDTO {
  // previously: Limen
  @PrimaryKey(sequence = "document_pk_sequence")
  private Long id;

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGTextNodeDTO.class)
  private List<Long> textNodeIds = new ArrayList<>();

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGMarkupDTO.class)
  private List<Long> markupIds = new ArrayList<>();

  private Date creationDate = new Date();
  private Date modificationDate = new Date();
  public TextGraph textGraph = new TextGraph();

  public TAGDocumentDTO() {
  }

  public Long getDbId() {
    return id;
  }

  public void setTextNodeIds(List<Long> textNodeIds) {
    this.textNodeIds = textNodeIds;
  }

  public boolean hasTextNodes() {
    return !getTextNodeIds().isEmpty();
  }

  public List<Long> getTextNodeIds() {
    return textNodeIds;
  }

  public void setMarkupIds(List<Long> markupIds) {
    this.markupIds = markupIds;
  }

  public List<Long> getMarkupIds() {
    return markupIds;
  }

  public void setCreationDate(final Date creationDate) {
    this.creationDate = creationDate;
  }

  public Date getCreationDate() {
    return creationDate;
  }

  public void setModificationDate(final Date modificationDate) {
    this.modificationDate = modificationDate;
  }

  public void updateModificationDate() {
    modificationDate = new Date();
  }

  public Date getModificationDate() {
    return modificationDate;
  }

  public void setFirstTextNodeId(final Long firstTextNodeId) {
    textGraph.setFirstTextNodeId(firstTextNodeId);
  }

  public Long getFirstTextNodeId() {
    return textGraph.getFirstTextNodeId();
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TAGMarkupDTO markup) {
    int textNodeSize = textNodeIds.size();
    return textNodeSize > 2 //
        && getTextNodeIdStreamForMarkupId(markup.getDbId(), "").count() >= textNodeSize / 2d;
  }

  private Stream<Long> getTextNodeIdStreamForMarkupId(final Long markupId, final String layerName) {
    return textGraph.getTextNodeIdStreamForMarkupIdInLayer(markupId, layerName);
  }

  public Stream<Long> getMarkupIdsForTextNodeId(Long textNodeId) {
    return textGraph.getMarkupIdStreamForTextNodeId(textNodeId);
  }

  public void addTextNode(TAGTextNodeDTO textNode) {
    textNodeIds.add(textNode.getDbId());
  }

  public void associateTextWithMarkupForLayer(TAGTextNodeDTO textNode, TAGMarkupDTO markup, final String layerName) {
    textGraph.linkMarkupToTextNodeForLayer(markup.getDbId(), textNode.getDbId(), layerName);
  }

  public boolean markupHasTextNodes(final TAGMarkup markup) {
    // TODO: layers
    return getTextNodeIdStreamForMarkupId(markup.getDbId(), "").findAny().isPresent();
  }
}
