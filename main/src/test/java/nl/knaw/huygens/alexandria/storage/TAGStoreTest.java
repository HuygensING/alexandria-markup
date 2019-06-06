package nl.knaw.huygens.alexandria.storage;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

public class TAGStoreTest extends AlexandriaBaseStoreTest {

  @Test
  public void testTAGStore() {
    AtomicLong documentId = new AtomicLong();
    TAGTextNode textNode = new TAGTextNode("something");

    runInStore(store ->
        store.runInTransaction(() -> {
          Long textNodeId = store.update(textNode);

          TAGDocument document = new TAGDocument();
          document.getTextNodeIds().add(textNode.getDbId());
          documentId.set(store.update(document));
        }));

    runInStore(store ->
        store.runInTransaction(() -> {
          TAGDocument document = store.getDocumentDTO(documentId.get());
          assertThat(document.getTextNodeIds()).contains(textNode.getDbId());
        }));
  }

}
