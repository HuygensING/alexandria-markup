package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.persist.EntityCursor;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredLimen;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredTextNode;
import org.junit.Test;

import java.io.File;
import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;

public class DataAccessorTest {

  @Test
  public void testDataAccessor() {
    DbEnv dbEnv = new DbEnv();
    File file = new File("out/bdb");
    file.mkdirs();
    dbEnv.setup(file, false);
    DataAccessor da = new DataAccessor(dbEnv.getEntityStore());
    StoredLimen limen = new StoredLimen();
    StoredTextNode textNode = new StoredTextNode();
    textNode.setText("Alea acta est");
    try {
      da.textNodeById.put(textNode);
      limen.getTextNodeIds().add(textNode.getId());
      da.limenById.put(limen);
      StoredLimen limen2 = da.limenById.get(limen.getId());
      assertThat(limen).isEqualToComparingFieldByField(limen);
      StoredTextNode tn2 = da.textNodeById.get(limen2.getTextNodeIds().get(0));
      assertThat(tn2.getText()).isEqualTo("Alea acta est");
      System.out.println("limen.id=" + limen2.getId());

      try (EntityCursor<Long> cursor = da.textNodeById.keys()) {
        Iterator<Long> iterator = cursor.iterator();
        while (iterator.hasNext()) {
          Long id = iterator.next();
          System.out.println("textNode.id=" + id);
        }
      }
    } finally {
      dbEnv.close();
    }

  }
}
