package nl.knaw.huygens.alexandria.lmnl.storage.dao;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextNode;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Entity
public class TAGMarkup {
  @PrimaryKey(sequence = "textrange_pk_sequence")
  private long id;
  private String lmnlId = ""; // LMNL, should be unique
  private String suffix = ""; // TexMECS, doesn't need to be unique

  private String tag;
  private List<Long> annotationIds = new ArrayList<>();
  private List<Long> textNodeIds = new ArrayList<>();
  private long documentId;
  private boolean isAnonymous = true;
  private boolean isContinuous = true;

  public TAGMarkup(long documentId, String tagName) {
    this.documentId = documentId;
    this.tag = tagName;
  }

  public TAGMarkup(TAGDocument document, String tagName) {
    this.documentId = document.getId();
    this.tag = tagName;
  }

  public long getId() {
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
    if (StringUtils.isNotEmpty(suffix)) {
      return tag + "~" + suffix;
    }
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

  public boolean hasN() {
    // TODO
    return true;
//    return annotations.parallelStream()//
//        .map(Annotation::getTag) //
//        .anyMatch(t -> t.equals("n"));
  }

  public void joinWith(TAGMarkup markup) {
    // TODO
  }

  public boolean isAnonymous() {
    // TODO
    return isAnonymous;
  }

  public boolean isContinuous() {
    // TODO
    return isContinuous;
  }

  public Long getDocumentId() {
    return documentId;
  }
}
