package nl.knaw.huygens.alexandria.storage.dto;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

@Entity(version = 2)
public class TAGMarkupDTO implements TAGDTO {
  @PrimaryKey(sequence = "tgnode_pk_sequence")
  private Long id;

  private String tag;
  private String markupId;
  private long documentId;
  private boolean isAnonymous = true;
  private boolean isContinuous = true;
  private String suffix;
  private Long dominatedMarkupId;
  private Long dominatingMarkupId;
  private boolean optional = false;
  private boolean discontinuous = false;
  private Set<String> layers = new TreeSet<>();

  private TAGMarkupDTO() {
  }

  public TAGMarkupDTO(TAGDocumentDTO document, String tagName) {
    this(document.getDbId(), tagName);
  }

  public TAGMarkupDTO(Long documentId, String tagName) {
    this.documentId = documentId;
    this.tag = tagName;
  }

  public Long getDbId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public Long getDocumentId() {
    return documentId;
  }

  public String getSuffix() {
    return suffix;
  }

  public void setSuffix(String suffix) {
    this.suffix = suffix;
  }

  public Optional<Long> getDominatedMarkupId() {
    return Optional.ofNullable(dominatedMarkupId);
  }

  public void setDominatedMarkupId(Long dominatedMarkupId) {
    this.dominatedMarkupId = dominatedMarkupId;
  }

  public Optional<Long> getDominatingMarkupId() {
    return Optional.ofNullable(dominatingMarkupId);
  }

  public void setDominatingMarkupId(Long dominatingMarkupId) {
    this.dominatingMarkupId = dominatingMarkupId;
  }

  public String getMarkupId() {
    return markupId;
  }

  public TAGMarkupDTO setMarkupId(String markupId) {
    this.markupId = markupId;
    return this;
  }

  public TAGMarkupDTO addLayer(final String layer) {
    this.layers.add(layer);
    return this;
  }

  public TAGMarkupDTO addAllLayers(final Set<String> layers) {
    this.layers.addAll(layers);
    return this;
  }

  public Set<String> getLayers() {
    return layers;
  }

  public TAGMarkupDTO setOptional(boolean optional) {
    this.optional = optional;
    return this;
  }

  public boolean isOptional() {
    return optional;
  }

  public void setDiscontinuous(final boolean discontinuous) {
    this.discontinuous = discontinuous;
  }

  public boolean isDiscontinuous() {
    return discontinuous;
  }

  @Override
  public String toString() {
    return "[" + tag + "]";
  }

  @Override
  public int hashCode() {
    return id.intValue();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TAGMarkupDTO//
        && getDbId().equals(((TAGMarkupDTO) other).getDbId());
  }

  @Deprecated
  public List<Long> getTextNodeIds() {
    // TODO: remove dependency on this method
    return null;
  }

}
