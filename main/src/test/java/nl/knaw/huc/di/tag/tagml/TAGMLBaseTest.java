package nl.knaw.huc.di.tag.tagml;

/*
 * #%L
 * alexandria-markup-core
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

import nl.knaw.AntlrUtils;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class TAGMLBaseTest {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLBaseTest.class);

  protected void printTokens(String input) {
    LOG.info("\nTAGML:\n{}\n", input);
    printTokens(CharStreams.fromString(input));
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    TAGMLLexer lexer = new TAGMLLexer(inputStream);
    String table = AntlrUtils.makeTokenTable(lexer);
    LOG.info("\nTokens:\n{}\n", table);
  }

}
