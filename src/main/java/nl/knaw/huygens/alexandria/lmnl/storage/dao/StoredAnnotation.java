package nl.knaw.huygens.alexandria.lmnl.storage.dao;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class StoredAnnotation {
  @PrimaryKey(sequence = "annotation_pk_sequence")
  private long id;

  private String tag;
  private final List<Long> annotationIds = new ArrayList<>();

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
}
