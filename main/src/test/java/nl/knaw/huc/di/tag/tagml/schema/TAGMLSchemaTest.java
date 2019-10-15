package nl.knaw.huc.di.tag.tagml.schema;

/*-
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
import org.junit.Test;

import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMLSchemaTest {
  @Test
  public void test() {
    String schemaYAML =
        "---\n"
            + "L1:\n"
            + "   root:\n"
            + "     - a\n"
            + "     - b\n"
            + "     - c\n"
            + "     - d:\n"
            + "         - d1\n"
            + "         - d2\n"
            + "L2:\n"
            + "  root:\n"
            + "    - x\n"
            + "    - y\n"
            + "    - z:\n"
            + "        - z1\n"
            + "        - z2\n";
    TAGMLSchema schema = TAGMLSchemaFactory.fromYAML(schemaYAML);
    assertThat(schema.getLayers()).containsExactly("L1", "L2");
  }
}
