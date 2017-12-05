package nl.knaw.huygens.alexandria.creole;

/*-
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
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;

@RunWith(Parameterized.class)
public class ImportCreoleSchemasTest extends CreoleTest {
  private static final String ROOTDIR = "src/test/resources/";
  private static final Logger LOG = LoggerFactory.getLogger(ImportCreoleSchemasTest.class);
  private final String basename;

  private static final IOFileFilter CREOLE_FILE_FILTER = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return isCreoleXML(file.getName());
    }

    @Override
    public boolean accept(File dir, String name) {
      return isCreoleXML(name);
    }

    private boolean isCreoleXML(String name) {
      return name.endsWith(".creole");
    }
  };

  @Parameterized.Parameters
  public static Collection<String[]> parameters() {
    return FileUtils.listFiles(new File(ROOTDIR), CREOLE_FILE_FILTER, null)//
        .stream()//
        .map(File::getName)//
        .map(n -> n.replace(".creole", ""))//
        .map(b -> new String[]{b})//
        .collect(Collectors.toList());
  }

  public ImportCreoleSchemasTest(String basename) {
    this.basename = basename;
  }

  @Test
  public void testCreoleFile() throws IOException, LMNLSyntaxError {
    LOG.info("testing {}.creole", basename);
    processCreoleFile(basename);
    LOG.info("done testing {}.creole", basename);
  }

  private void processCreoleFile(String basename) throws IOException {
    String xml = FileUtils.readFileToString(new File(ROOTDIR + basename + ".creole"), "UTF-8");
    assertThat(xml).isNotEmpty();
    LOG.info("testing {}.creole", basename);
    LOG.info("{}", xml);
    Pattern schema = SchemaImporter.fromXML(xml);
    assertThat(schema).isNotNull();
  }


}
