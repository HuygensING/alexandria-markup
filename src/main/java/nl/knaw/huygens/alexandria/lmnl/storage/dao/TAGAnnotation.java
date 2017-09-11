package nl.knaw.huygens.alexandria.lmnl.storage.dao;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import static com.sleepycat.persist.model.Relationship.ONE_TO_MANY;
import static com.sleepycat.persist.model.Relationship.ONE_TO_ONE;
import com.sleepycat.persist.model.SecondaryKey;

import java.util.ArrayList;
import java.util.List;

@Entity
public class TAGAnnotation {
  @PrimaryKey(sequence = "annotation_pk_sequence")
  private long id;

  private String tag;

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGAnnotation.class)
  private final List<Long> annotationIds = new ArrayList<>();

  @SecondaryKey(relate = ONE_TO_ONE, relatedEntity = TAGDocument.class)
  private long documentId;

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

  public TAGAnnotation addAnnotation(TAGAnnotation annotation) {
    annotationIds.add(annotation.getId());
    return this;
  }

  public void setDocumentId(long documentId) {
    this.documentId = documentId;
  }

  public long getDocumentId() {
    return documentId;
  }

}
