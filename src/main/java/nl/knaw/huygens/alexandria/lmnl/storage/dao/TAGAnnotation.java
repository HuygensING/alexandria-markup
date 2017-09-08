package nl.knaw.huygens.alexandria.lmnl.storage.dao;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity
public class TAGAnnotation {
  @PrimaryKey(sequence = "annotation_pk_sequence")
  private long id;

  private String tag;
  private final List<Long> annotationIds = new ArrayList<>();
  private long limenId;

  public TAGAnnotation(String tag) {
    this.tag = tag;
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

  public void addAnnotation(TAGAnnotation annotation) {
    annotationIds.add(annotation.getId());
  }

  public long value() {
    return limenId;
  }

  public void setLimenId(long limenId) {
    this.limenId = limenId;
  }
}
