package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

import nl.knaw.huygens.alexandria.lmnl.storage.dto.AnnotationDTO;
import nl.knaw.huygens.alexandria.lmnl.storage.dto.LimenDTO;
import nl.knaw.huygens.alexandria.lmnl.storage.dto.TextNodeDTO;
import nl.knaw.huygens.alexandria.lmnl.storage.dto.TextRangeDTO;

public class DataAccessor {
  PrimaryIndex<Long, LimenDTO> limenById;
  PrimaryIndex<Long, TextNodeDTO> textNodeById;
  PrimaryIndex<Long, TextRangeDTO> textRangeById;
  PrimaryIndex<Long, AnnotationDTO> annotationById;

  public DataAccessor(EntityStore store) throws DatabaseException {
    limenById = store.getPrimaryIndex(Long.class, LimenDTO.class);
    textNodeById = store.getPrimaryIndex(Long.class, TextNodeDTO.class);
    textRangeById = store.getPrimaryIndex(Long.class, TextRangeDTO.class);
    annotationById = store.getPrimaryIndex(Long.class, AnnotationDTO.class);
  }

}
