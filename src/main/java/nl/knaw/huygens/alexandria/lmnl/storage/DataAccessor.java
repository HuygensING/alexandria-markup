package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGMarkup;

public class DataAccessor {
  PrimaryIndex<Long, TAGDocument> limenById;
  PrimaryIndex<Long, TAGTextNode> textNodeById;
  PrimaryIndex<Long, TAGMarkup> markupById;
  PrimaryIndex<Long, TAGAnnotation> annotationById;

  public DataAccessor(EntityStore store) throws DatabaseException {
    limenById = store.getPrimaryIndex(Long.class, TAGDocument.class);
    textNodeById = store.getPrimaryIndex(Long.class, TAGTextNode.class);
    markupById = store.getPrimaryIndex(Long.class, TAGMarkup.class);
    annotationById = store.getPrimaryIndex(Long.class, TAGAnnotation.class);
  }

}
