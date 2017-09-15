package nl.knaw.huygens.alexandria.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity(version = 1)
public class TAGTextNode implements TAGObject {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private Long id;

  private String text;

  private Long prevTextNodeId;
  private Long nextTextNodeId;

  private TAGTextNode() {
  }

  public TAGTextNode(String text) {
    this.text = text;
  }

  public Long getId() {
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
