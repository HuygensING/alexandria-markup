package nl.knaw.huygens.alexandria.lmnl.storage;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class TAGStoreTest {

  @Test
  public void testTAGStore() {
    TAGStore store = new TAGStore("out", false);
    store.open();

    AtomicLong documentId = new AtomicLong();
    TAGTextNode textNode = new TAGTextNode("something");
    store.runInTransaction(() -> {
      Long textNodeId = store.putTextNode(textNode);

      TAGDocument limen = new TAGDocument();
      limen.getTextNodeIds().add(textNode.getId());
      documentId.set(store.putLimen(limen));
    });

    store.close();

    store.open();

    store.runInTransaction(() -> {
      TAGDocument document = store.getDocument(documentId.get());
      assertThat(document.getTextNodeIds()).contains(textNode.getId());
    });

    store.close();
  }

}
