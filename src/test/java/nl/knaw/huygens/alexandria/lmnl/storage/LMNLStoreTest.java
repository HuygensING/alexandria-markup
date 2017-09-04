package nl.knaw.huygens.alexandria.lmnl.storage;

import org.junit.Test;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.LimenDAO;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TextNodeDAO;

public class LMNLStoreTest {

  @Test
  public void testLMNLStore() {
    LMNLStore store = new LMNLStore("out", false);
    store.open();
    LimenDAO limen = new LimenDAO();
    TextNodeDAO textNode = new TextNodeDAO();
    textNode.setText("something");
    Long limenId = store.putLimen(limen);
    store.close();

    store.open();
    LimenDAO storedLimen = store.getLimen(limenId);
    store.close();
  }

}
