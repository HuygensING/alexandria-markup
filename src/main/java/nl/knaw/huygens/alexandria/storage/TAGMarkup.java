package nl.knaw.huygens.alexandria.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Entity(version = 1)
public class TAGMarkup implements TAGObject {
  @PrimaryKey(sequence = "textrange_pk_sequence")
  private Long id;

  private String tag;
  private List<Long> annotationIds = new ArrayList<>();
  private List<Long> textNodeIds = new ArrayList<>();
  private long documentId;
  private boolean isAnonymous = true;
  private boolean isContinuous = true;

  private TAGMarkup() {
  }

  public TAGMarkup(long documentId, String tagName) {
    this.documentId = documentId;
    this.tag = tagName;
  }

  public TAGMarkup(TAGDocument document, String tagName) {
    this.documentId = document.getId();
    this.tag = tagName;
  }

  public Long getId() {
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
    String suffix = "";
    if (StringUtils.isNotEmpty(suffix)) {
      return tag + "~" + suffix;
    }
    String lmnlId = "";
    if (StringUtils.isNotEmpty(lmnlId)) {
      return tag + "=" + lmnlId;
    }
    return tag;
  }

  public void addTextNode(TAGTextNode textNode) {
    textNodeIds.add(textNode.getId());
  }

  public TAGMarkup addAnnotation(TAGAnnotation annotation) {
    annotationIds.add(annotation.getId());
    return this;
  }

  public Long getDocumentId() {
    return documentId;
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TAGMarkup//
        && getId().equals(((TAGMarkup) other).getId());
  }
}
