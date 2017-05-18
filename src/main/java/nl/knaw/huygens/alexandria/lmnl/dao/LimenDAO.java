package nl.knaw.huygens.alexandria.lmnl.dao;

import static com.sleepycat.persist.model.Relationship.ONE_TO_MANY;

import java.util.ArrayList;
import java.util.List;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.SecondaryKey;

@Entity
public class LimenDAO {

  @PrimaryKey(sequence = "limen_pk_sequence")
  private long id;

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TextNodeDAO.class)
  List<Long> textNodeIds = new ArrayList<>();

  @SecondaryKey(relate = ONE_TO_MANY)
  List<Long> textRangeIds = new ArrayList<>();

  public long getId() {
    return id;
  }

}
