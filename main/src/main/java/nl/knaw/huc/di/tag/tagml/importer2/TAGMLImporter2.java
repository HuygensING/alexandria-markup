package nl.knaw.huc.di.tag.tagml.importer2;

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

import nl.knaw.huc.di.tag.model.graph.DotFactory;
import nl.knaw.huc.di.tag.tagml.TAGMLBreakingError;
import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class TAGMLImporter2 {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporter2.class);

  public TAGMLImporter2() {
  }

  public TAGKnowledgeModel importTAGML(final String input) throws TAGMLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importTAGML(antlrInputStream);
  }

  public TAGKnowledgeModel importTAGML(InputStream input) throws TAGMLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importTAGML(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private TAGKnowledgeModel importTAGML(CharStream antlrInputStream) throws TAGMLSyntaxError {
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    TAGMLParser parser = new TAGMLParser(tokens);
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
//    LOG.debug("parsetree: {}", parseTree.toStringTree(parser));

    TAGMLKnowledgeModelListener listener = new TAGMLKnowledgeModelListener(errorListener);
    try {
      ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    } catch (TAGMLBreakingError ignored) {
        LOG.error("TAGMLBreakingError:{}", ignored);
    }

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} parser syntax errors", numberOfSyntaxErrors);

    String errorMsg = "";
    if (errorListener.hasErrors()) {
//      logDocumentGraph(document,"");
      String errors = String.join("\n", errorListener.getErrors());
      errorMsg = "Parsing errors:\n" + errors;
      throw new TAGMLSyntaxError(errorMsg);
    }
    return listener.getKnowledgeModel();
  }

  protected static void logDocumentGraph(final TAGKnowledgeModel knowledgeModel, final String input) {
    System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
    System.out.println(new DotFactory().toDot(knowledgeModel, input));
    System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
  }

}
