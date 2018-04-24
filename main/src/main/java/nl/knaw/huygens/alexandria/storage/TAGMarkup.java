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
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity(version = 1)
public class TAGMarkup implements TAGObject {
  @PrimaryKey(sequence = "textrange_pk_sequence")
  private Long id;

  private String tag;
  private String markupId;
  private List<Long> annotationIds = new ArrayList<>();
  private List<Long> textNodeIds = new ArrayList<>();
  private long documentId;
  private boolean isAnonymous = true;
  private boolean isContinuous = true;
  private String suffix;
  private Long dominatedMarkupId;
  private Long dominatingMarkupId;
  private boolean optional = false;

  private TAGMarkup() {
  }

  public TAGMarkup(long documentId, String tagName) {
    this.documentId = documentId;
    this.tag = tagName;
  }

  public TAGMarkup(TAGDocument document, String tagName) {
    this.documentId = document.getDbId();
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

  public List<Long> getAnnotationIds() {
    return annotationIds;
  }

  public void setAnnotationIds(List<Long> annotationIds) {
    this.annotationIds = annotationIds;
  }

  public List<Long> getTextNodeIds() {
    return textNodeIds;
  }

  public void setTextNodeIds(List<Long> textNodeIds) {
    this.textNodeIds = textNodeIds;
  }

  public String getExtendedTag() {
    // TODO: this is output language dependent move to language
    if (StringUtils.isNotEmpty(suffix)) {
      return tag + "~" + suffix;
    }
//    if (StringUtils.isNotEmpty(markupId)) {
//      return tag + "=" + markupId;
//    }
    return tag;
  }

  public void addTextNode(TAGTextNode textNode) {
    textNodeIds.add(textNode.getDbId());
  }

  public TAGMarkup addAnnotation(TAGAnnotation annotation) {
    annotationIds.add(annotation.getDbId());
    return this;
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

  public TAGMarkup setMarkupId(String markupId) {
    this.markupId = markupId;
    return this;
  }

  public TAGMarkup setOptional(boolean optional) {
    this.optional = optional;
    return this;
  }

  public boolean isOptional() {
    return optional;
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
    return other instanceof TAGMarkup//
        && getDbId().equals(((TAGMarkup) other).getDbId());
  }
}
