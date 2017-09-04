package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.AnnotationDAO;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.LimenDAO;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TextNodeDAO;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TextRangeDAO;

public class DataAccessor {
  PrimaryIndex<Long, LimenDAO> limenById;
  PrimaryIndex<Long, TextNodeDAO> textNodeById;
  PrimaryIndex<Long, TextRangeDAO> textRangeById;
  PrimaryIndex<Long, AnnotationDAO> annotationById;

  public DataAccessor(EntityStore store) throws DatabaseException {
    limenById = store.getPrimaryIndex(Long.class, LimenDAO.class);
    textNodeById = store.getPrimaryIndex(Long.class, TextNodeDAO.class);
    textRangeById = store.getPrimaryIndex(Long.class, TextRangeDAO.class);
    annotationById = store.getPrimaryIndex(Long.class, AnnotationDAO.class);
  }

}
