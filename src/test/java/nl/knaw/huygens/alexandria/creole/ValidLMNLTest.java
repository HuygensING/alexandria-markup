package nl.knaw.huygens.alexandria.creole;

    /*-
     * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter2;
import nl.knaw.huygens.alexandria.lmnl.importer.LMNLSyntaxError;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static nl.knaw.huygens.alexandria.AlexandriaAssertions.assertThat;

@RunWith(Parameterized.class)
public class ValidLMNLTest extends CreoleTest {
  private static final String ROOTDIR = "src/test/resources/";
  private static final String LMNL_DIR = ROOTDIR + "valid/";
  private static final Logger LOG = LoggerFactory.getLogger(ValidLMNLTest.class);

  private final String basename;

  private static final IOFileFilter LMNL_FILE_FILTER = new IOFileFilter() {
    @Override
    public boolean accept(File file) {
      return isLMNL(file.getName());
    }

    @Override
    public boolean accept(File dir, String name) {
      return isLMNL(name);
    }

    private boolean isLMNL(String name) {
      return name.endsWith(".lmnl");
    }
  };

  @Parameterized.Parameters
  public static Collection<String[]> parameters() {
    return FileUtils.listFiles(new File(LMNL_DIR), LMNL_FILE_FILTER, null)//
        .stream()//
        .map(File::getName)//
        .map(n -> n.replace(".lmnl", ""))//
        .map(b -> new String[]{b})//
        .collect(Collectors.toList());
  }

  public ValidLMNLTest(String basename) {
    this.basename = basename;
  }

  @Ignore
  @Test
  public void testCreoleFile() throws IOException, LMNLSyntaxError {
    LOG.info("validating {}.lmnl against {}.creole", basename, basename);
    validateLMNL(basename);
    LOG.info("done validating {}.lmnl against {}.creole", basename, basename);
  }

  private void validateLMNL(String basename) throws IOException {
    String xml = FileUtils.readFileToString(new File(ROOTDIR + basename + ".creole"), "UTF-8");
    assertThat(xml).isNotEmpty();
    LOG.info("testing {}.creole", basename);
    LOG.info("creole=\n{}", xml);
    Pattern schema = SchemaImporter.fromXML(xml);
    assertThat(schema).isNotNull();

    String lmnl = FileUtils.readFileToString(new File(LMNL_DIR + basename + ".lmnl"), "UTF-8");
    LOG.info("lmnl=\n{}", lmnl);
    List<Event> events = new LMNLImporter2().importLMNL(lmnl);
    Validator validator = Validator.ofPattern(schema);
    ValidationResult result = validator.validate(events);
    assertThat(result).isSuccess();
  }

}
