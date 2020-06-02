package nl.knaw.huygens.alexandria;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.storage.BDBTAGStore;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Function;

public class AlexandriaBaseStoreTest extends AlexandriaLMNLBaseTest {
  public static final String DUMMY_HEADER = "[{}]\n";

  private static final Logger LOG = LoggerFactory.getLogger(AlexandriaBaseStoreTest.class);
  private static Path tmpDir;

  @BeforeClass
  public static void beforeClass() throws IOException {
    //    LOG.info("System.getenv()={}", System.getenv().toString().replace(",", ",\n"));
    tmpDir = Files.createTempDirectory("tmpDir");
    LOG.info("Created tempDirectory {}", tmpDir.toAbsolutePath());
    tmpDir.toFile().deleteOnExit();
  }

  @AfterClass
  public static void afterClass() {
    LOG.info("Deleting tempDirectory {}", tmpDir.toAbsolutePath());
    try {
      FileUtils.forceDelete(tmpDir.toFile());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void runInStore(Consumer<TAGStore> storeConsumer) {
    try (TAGStore store = getStore()) {
      storeConsumer.accept(store);
    }
  }

  public void runInStoreTransaction(Consumer<TAGStore> storeConsumer) {
    try (TAGStore store = getStore()) {
      store.runInTransaction(() -> storeConsumer.accept(store));
    }
  }

  public <T> T runInStoreTransaction(Function<TAGStore, T> storeFunction) {
    try (TAGStore store = getStore()) {
      return store.runInTransaction(() -> storeFunction.apply(store));
    }
  }

  private TAGStore getStore() {
    return new BDBTAGStore(tmpDir.toString(), false);
  }
}
