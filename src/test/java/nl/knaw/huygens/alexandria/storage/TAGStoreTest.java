package nl.knaw.huygens.alexandria.storage;

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class TAGStoreTest extends AlexandriaBaseStoreTest{

  @Test
  public void testTAGStore() {
    store.open();

    AtomicLong documentId = new AtomicLong();
    TAGTextNode textNode = new TAGTextNode("something");
    store.runInTransaction(() -> {
      Long textNodeId = store.persist(textNode);

      TAGDocument document = new TAGDocument();
      document.getTextNodeIds().add(textNode.getId());
      documentId.set(store.persist(document));
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
