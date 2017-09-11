package nl.knaw.huygens.alexandria.lmnl.storage.dao;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class TAGTextNode {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private long id;

  private String text;

  private Long prevTextNodeId;
  private Long nextTextNodeId;

  public TAGTextNode(String text) {
    this.text = text;
  }

  public long getId() {
    return id;
  }

  public String getText() {
    return text;
  }

  public TAGTextNode setText(String text) {
    this.text = text;
    return this;
  }

  public Long getPrevTextNodeId() {
    return prevTextNodeId;
  }

  public TAGTextNode setPrevTextNodeId(long prevId) {
    this.prevTextNodeId = prevId;
    return this;
  }

  public Long getNextTextNodeId() {
    return nextTextNodeId;
  }

  public TAGTextNode setNextTextNodeId(long nextId) {
    this.nextTextNodeId = nextId;
    return this;
  }

}
