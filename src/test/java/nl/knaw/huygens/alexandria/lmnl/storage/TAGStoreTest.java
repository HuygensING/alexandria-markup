package nl.knaw.huygens.alexandria.lmnl.storage;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredLimen;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredTextNode;

public class TAGStoreTest {

//  @Test
  public void testTAGStore() {
    TAGStore store = new TAGStore("out", false);
    store.open();
    StoredLimen limen = new StoredLimen();
    StoredTextNode textNode = new StoredTextNode();
    textNode.setText("something");
    Long limenId = store.putLimen(limen);
    store.close();

    store.open();
    StoredLimen storedLimen = store.getLimen(limenId);
    assertThat(storedLimen.getTextNodeIds()).contains(textNode.getId());
    store.close();
  }

}
