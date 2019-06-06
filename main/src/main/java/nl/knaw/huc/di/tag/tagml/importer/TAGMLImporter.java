package nl.knaw.huc.di.tag.tagml.importer;

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
import nl.knaw.huygens.alexandria.storage.TAGDocumentDAO;
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

public class TAGMLImporter {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporter.class);

  public TAGMLImporter() {
  }

  public TAGDocumentDAO importTAGML(final TAGModelBuilder tagModelBuilder, final String input) throws TAGMLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importTAGML(tagModelBuilder, antlrInputStream);
  }

  public TAGDocumentDAO importTAGML(final TAGModelBuilder tagModelBuilder, InputStream input) throws TAGMLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importTAGML(tagModelBuilder, antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private TAGDocumentDAO importTAGML(final TAGModelBuilder tagModelBuilder, CharStream antlrInputStream) throws TAGMLSyntaxError {
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    ErrorListener errorListener = tagModelBuilder.getErrorListener();
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    TAGMLParser parser = new TAGMLParser(tokens);
    parser.addErrorListener(errorListener);

    TAGDocumentDAO document = usingListener(parser, tagModelBuilder);
//    DocumentWrapper documentWrapper = usingVisitor(parser, errorListener);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
//    LOG.info("parsed with {} parser syntax errors", numberOfSyntaxErrors);

    String errorMsg = "";
    if (errorListener.hasErrors()) {
//      logDocumentGraph(document,"");
      String errors = String.join("\n", errorListener.getErrors());
      errorMsg = "Parsing errors:\n" + errors;
      throw new TAGMLSyntaxError(errorMsg);
    }
    tagModelBuilder.persist(document);
    return document;
  }

  private TAGDocumentDAO usingListener(final TAGMLParser parser, final TAGModelBuilder tagModelBuilder) {
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
//    LOG.debug("parsetree: {}", parseTree.toStringTree(parser));
    TAGMLListener listener = new TAGMLListener(tagModelBuilder);
    try {
      ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    } catch (TAGMLBreakingError ignored) {

    }
    return tagModelBuilder.getDocument();
  }

//  private TAGDocument usingVisitor(final TAGMLParser parser, final ErrorListener errorListener) {
//    TAGMLParser.DocumentContext documentContext = parser.document();
//    TAGMLVisitor visitor = new TAGMLVisitor(tagModelBuilder, errorListener);
//    visitor.visit(documentContext);
//    return visitor.getDocument();
//  }

  protected void logDocumentGraph(final TAGDocumentDAO document, final String input) {
    System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
    System.out.println(new DotFactory().toDot(document, input));
    System.out.println("\n------------8<------------------------------------------------------------------------------------\n");
  }

}
