package nl.knaw.huygens.alexandria.lmnl.storage;

import org.junit.Test;

import nl.knaw.huygens.alexandria.lmnl.storage.LMNLStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dto.LimenDTO;
import nl.knaw.huygens.alexandria.lmnl.storage.dto.TextNodeDTO;

public class LMNLStoreTest {

  @Test
  public void testLMNLStore() {
    LMNLStore store = new LMNLStore("out", false);
    store.open();
    LimenDTO limen = new LimenDTO();
    TextNodeDTO textNode = new TextNodeDTO();
    textNode.setText("something");
    Long limenId = store.putLimen(limen);
    store.close();

    store.open();
    LimenDTO storedLimen = store.getLimen(limenId);
    store.close();
  }

}
