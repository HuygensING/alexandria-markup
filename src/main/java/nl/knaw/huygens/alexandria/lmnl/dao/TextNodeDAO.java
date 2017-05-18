package nl.knaw.huygens.alexandria.lmnl.dao;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;

@Entity
public class TextNodeDAO {
  @PrimaryKey(sequence = "textnode_pk_sequence")
  private long id;

  private long prevId;
  private long nextId;

  String text;
}
