package nl.knaw.huygens.alexandria.storage;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import nl.knaw.huygens.alexandria.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dao.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dao.TAGMarkup;

public class DataAccessor {
  PrimaryIndex<Long, TAGDocument> documentById;
  PrimaryIndex<Long, TAGTextNode> textNodeById;
  PrimaryIndex<Long, TAGMarkup> markupById;
  PrimaryIndex<Long, TAGAnnotation> annotationById;

  public DataAccessor(EntityStore store) throws DatabaseException {
    documentById = store.getPrimaryIndex(Long.class, TAGDocument.class);
    textNodeById = store.getPrimaryIndex(Long.class, TAGTextNode.class);
    markupById = store.getPrimaryIndex(Long.class, TAGMarkup.class);
    annotationById = store.getPrimaryIndex(Long.class, TAGAnnotation.class);
  }

}
