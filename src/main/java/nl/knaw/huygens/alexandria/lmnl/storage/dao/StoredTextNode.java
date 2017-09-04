package nl.knaw.huygens.alexandria.lmnl.storage.dao;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class StoredTextNode {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private long id;

  private String text;

  private long prevTextNodeId;
  private long nextTextNodeId;

  public long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public long getPrevTextNodeId() {
    return prevTextNodeId;
  }

  public void setPrevTextNodeId(long prevId) {
    this.prevTextNodeId = prevId;
  }

  public long getNextTextNodeId() {
    return nextTextNodeId;
  }

  public void setNextTextNodeId(long nextId) {
    this.nextTextNodeId = nextId;
  }

}
