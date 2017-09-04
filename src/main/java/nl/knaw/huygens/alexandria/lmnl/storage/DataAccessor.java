package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredAnnotation;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredLimen;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredTextNode;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredMarkup;

public class DataAccessor {
  PrimaryIndex<Long, StoredLimen> limenById;
  PrimaryIndex<Long, StoredTextNode> textNodeById;
  PrimaryIndex<Long, StoredMarkup> textRangeById;
  PrimaryIndex<Long, StoredAnnotation> annotationById;

  public DataAccessor(EntityStore store) throws DatabaseException {
    limenById = store.getPrimaryIndex(Long.class, StoredLimen.class);
    textNodeById = store.getPrimaryIndex(Long.class, StoredTextNode.class);
    textRangeById = store.getPrimaryIndex(Long.class, StoredMarkup.class);
    annotationById = store.getPrimaryIndex(Long.class, StoredAnnotation.class);
  }

}
