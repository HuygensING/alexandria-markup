package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.persist.EntityCursor;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.LimenDAO;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TextNodeDAO;
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
    LimenDAO limen = new LimenDAO();
    TextNodeDAO textNode = new TextNodeDAO();
    textNode.setText("Alea acta est");
    try {
      da.textNodeById.put(textNode);
      limen.getTextNodeIds().add(textNode.getId());
      da.limenById.put(limen);
      LimenDAO limen2 = da.limenById.get(limen.getId());
      assertThat(limen).isEqualToComparingFieldByField(limen);
      TextNodeDAO tn2 = da.textNodeById.get(limen2.getTextNodeIds().get(0));
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
