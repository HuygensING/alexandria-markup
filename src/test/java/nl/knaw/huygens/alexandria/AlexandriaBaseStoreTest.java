package nl.knaw.huygens.alexandria;


/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import static java.util.stream.Collectors.toList;
import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import nl.knaw.huygens.alexandria.lmnl.data_model.IndexPoint;
import nl.knaw.huygens.alexandria.lmnl.data_model.NodeRangeIndex2;
import nl.knaw.huygens.alexandria.lmnl.exporter.LMNLExporter2;
import nl.knaw.huygens.alexandria.lmnl.exporter.LaTeXExporter2;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter2;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError;
import nl.knaw.huygens.alexandria.lmnl.storage.TAGStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGMarkup;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.lmnl.storage.wrappers.TextNodeWrapper;
import org.apache.commons.io.FileUtils;
import static org.assertj.core.api.Assertions.assertThat;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

public class AlexandriaBaseStoreTest extends AlexandriaLMNLBaseTest {

  private static Path tmpDir;
  final Logger LOG = LoggerFactory.getLogger(AlexandriaBaseStoreTest.class);
  public static TAGStore store;
  static LMNLExporter2 lmnlExporter;

  @BeforeClass
  public static void beforeClass() throws IOException {
    tmpDir = Files.createTempDirectory("tmpDir");
    tmpDir.toFile().deleteOnExit();
    store = new TAGStore(tmpDir.toString(), false);
    store.open();
    lmnlExporter = new LMNLExporter2(store).useShorthand();
  }

  @AfterClass
  public static void afterClass() {
    store.close();
    tmpDir.toFile().delete();
  }

}
