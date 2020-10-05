package nl.knaw.huygens.alexandria.texmecs.importer;

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

import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.data_model.Limen;
import nl.knaw.huygens.alexandria.data_model.Markup;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParser;
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
import java.util.List;

class TexMECSImporterInMemory {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  public Document importTexMECS(String input) throws TexMECSSyntaxError {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importTexMECS(antlrInputStream);
  }

  public Document importTexMECS(InputStream input) throws TexMECSSyntaxError {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importTexMECS(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new UncheckedIOException(e);
    }
  }

  private Document importTexMECS(CharStream antlrInputStream) {
    TexMECSLexer lexer = new TexMECSLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    TexMECSParser parser = new TexMECSParser(tokens);
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.debug("parsed with {} syntax errors", numberOfSyntaxErrors);
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    TexMECSListenerInMemory listener = new TexMECSListenerInMemory();
    parseTreeWalker.walk(listener, parseTree);
    Document document = listener.getDocument();
    handleMarkupDominance(document.value());

    String errorMsg = "";
    if (listener.hasErrors()) {
      String errors = String.join("\n", listener.getErrors());
      errorMsg = "Parsing errors:\n" + errors;
    }
    if (numberOfSyntaxErrors > 0) {
      errorMsg += "\n\nTokenizing errors:\n" + errorListener.getPrefixedErrorMessagesAsString();
    }
    if (!errorMsg.isEmpty()) {
      throw new TexMECSSyntaxError(errorMsg);
    }
    return document;
  }

  private void handleMarkupDominance(Limen limen) {
    List<Markup> markupList = limen.markupList;
    for (int i = 0; i < markupList.size() - 1; i++) {
      Markup first = markupList.get(i);
      Markup second = markupList.get(i + 1);
      if (first.textNodes.equals(second.textNodes)) {
        // LOG.info("dominance found: {} dominates {}", first.getExtendedTag(),
        // second.getExtendedTag());
        first.setDominatedMarkup(second);
      }
    }
  }
}
