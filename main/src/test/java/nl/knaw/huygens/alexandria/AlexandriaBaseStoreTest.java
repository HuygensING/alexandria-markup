package nl.knaw.huygens.alexandria;


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

import nl.knaw.huc.di.tag.tagml.exporter.TAGMLExporter;
import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class AlexandriaBaseStoreTest extends AlexandriaLMNLBaseTest {

  private static final Logger LOG = LoggerFactory.getLogger(AlexandriaBaseStoreTest.class);
  protected static TAGStore store;
  protected static TAGMLExporter tagmlExporter;
  private static Path tmpDir;

  @BeforeClass
  public static void beforeClass() throws IOException {
    LOG.info("System.getenv()={}", System.getenv().toString().replace(",", ",\n"));
    tmpDir = Files.createTempDirectory("tmpDir");
    LOG.info("Created tempDirectory {}", tmpDir.toAbsolutePath());
    tmpDir.toFile().deleteOnExit();
    store = new TAGStore(tmpDir.toString(), false);
    store.open();
    tagmlExporter = new TAGMLExporter(store);
  }

  @AfterClass
  public static void afterClass() {
    LOG.info("afterClass() called!");
    LOG.info("Deleting tempDirectory {}", tmpDir.toAbsolutePath());
    boolean success = tmpDir.toFile().delete();
    if (!success) {
      LOG.warn("Could not delete tempDirectory {}", tmpDir.toAbsolutePath());
    }
    store.close();
    LOG.info("afterClass() finished!");
  }

}
