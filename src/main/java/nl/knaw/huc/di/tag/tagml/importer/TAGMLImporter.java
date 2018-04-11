package nl.knaw.huc.di.tag.tagml.importer;

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

import nl.knaw.huc.di.tag.tagml.TAGMLSyntaxError;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGObject;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static java.util.stream.Collectors.joining;

public class TAGMLImporter {

  private static final Logger LOG = LoggerFactory.getLogger(TAGMLImporter.class);

  private final TAGStore tagStore;

  public TAGMLImporter(final TAGStore store) {
    tagStore = store;
  }

  public DocumentWrapper importTAGML(final String input) throws TAGMLSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importTAGML(antlrInputStream);
  }

  public DocumentWrapper importTAGML(InputStream input) throws TAGMLSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importTAGML(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private DocumentWrapper importTAGML(CharStream antlrInputStream) throws TAGMLSyntaxError {
    TAGMLLexer lexer = new TAGMLLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    TAGMLParser parser = new TAGMLParser(tokens);
    parser.addErrorListener(errorListener);

    DocumentWrapper documentWrapper = usingListener(parser, errorListener);
//    DocumentWrapper documentWrapper = usingVisitor(parser, errorListener);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);

    String errorMsg = "";
    if (errorListener.hasErrors()) {
      String errors = errorListener.getErrors().stream().collect(joining("\n"));
      errorMsg = "Parsing errors:\n" + errors;
    }
    if (!errorMsg.isEmpty()) {
      throw new TAGMLSyntaxError(errorMsg);
    }
    update(documentWrapper.getDocument());
    return documentWrapper;
  }

  private DocumentWrapper usingVisitor(final TAGMLParser parser, final ErrorListener errorListener) {
    TAGMLParser.DocumentContext documentContext = parser.document();
    TAGMLVisitor visitor = new TAGMLVisitor(tagStore, errorListener);
    visitor.visit(documentContext);
    return visitor.getDocumentWrapper();
  }

  private DocumentWrapper usingListener(final TAGMLParser parser, final ErrorListener errorListener) {
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    LOG.info("parsetree: {}", parseTree.toStringTree(parser));
    TAGMLListener listener = new TAGMLListener(tagStore, errorListener);
    ParseTreeWalker.DEFAULT.walk(listener, parseTree);
    return listener.getDocument();
  }

  private Long update(TAGObject tagObject) {
    return tagStore.persist(tagObject);
  }

}
