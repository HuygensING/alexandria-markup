package nl.knaw.huygens.alexandria.storage;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import static org.assertj.core.api.Assertions.assertThat;

import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

public class TAGStoreTest extends AlexandriaBaseStoreTest{

  @Test
  public void testTAGStore() {
    store.open();

    AtomicLong documentId = new AtomicLong();
    TAGTextNodeDTO textNode = new TAGTextNodeDTO("something");
    store.runInTransaction(() -> {
      Long textNodeId = store.persist(textNode);

      TAGDocumentDTO document = new TAGDocumentDTO();
      document.getTextNodeIds().add(textNode.getDbId());
      documentId.set(store.persist(document));
    });

    store.close();

    store.open();

    store.runInTransaction(() -> {
      TAGDocumentDTO document = store.getDocumentDTO(documentId.get());
      assertThat(document.getTextNodeIds()).contains(textNode.getDbId());
    });

    store.close();
  }

}
