package nl.knaw.huygens.alexandria.lmnl.storage;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredLimen;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredTextNode;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class TAGStoreTest {

  @Test
  public void testTAGStore() {
    TAGStore store = new TAGStore("out", false);
    store.open();

    AtomicLong limenId = new AtomicLong();
    StoredTextNode textNode = new StoredTextNode();
    textNode.setText("something");
    store.runInTransaction(() -> {
      Long textNodeId = store.putTextNode(textNode);

      StoredLimen limen = new StoredLimen();
      limen.getTextNodeIds().add(textNode.getId());
      limenId.set(store.putLimen(limen));
    });

    store.close();

    store.open();

    store.runInTransaction(() -> {
      StoredLimen storedLimen = store.getLimen(limenId.get());
      assertThat(storedLimen.getTextNodeIds()).contains(textNode.getId());
    });

    store.close();
  }

}
