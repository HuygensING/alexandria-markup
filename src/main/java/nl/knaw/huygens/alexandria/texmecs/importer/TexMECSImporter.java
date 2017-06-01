package nl.knaw.huygens.alexandria.texmecs.importer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;

public class TexMECSImporter {
  final Logger LOG = LoggerFactory.getLogger(getClass());

  public Document importTexMECS(String input) {
    CharStream antlrInputStream = CharStreams.fromString(input);
    return importTexMECS(antlrInputStream);
  }

  public Document importTexMECS(InputStream input) {
    try {
      CharStream antlrInputStream = CharStreams.fromStream(input);
      return importTexMECS(antlrInputStream);
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  private Document importTexMECS(CharStream antlrInputStream) {
    TexMECSLexer lexer = new TexMECSLexer(antlrInputStream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TexMECSParser parser = new TexMECSParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    if (numberOfSyntaxErrors > 0) {
      throw new RuntimeException(numberOfSyntaxErrors + " Syntax errors");
    }
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    TexMECSListener listener = new TexMECSListener();
    parseTreeWalker.walk(listener, parseTree);
    Document document = listener.getDocument();
    handleMarkupDominance(document.value());
    return document;
  }

  private void handleMarkupDominance(Limen limen) {
    List<Markup> markupList = limen.markupList;
    for (int i = 0; i < markupList.size() - 1; i++) {
      Markup first = markupList.get(i);
      Markup second = markupList.get(i + 1);
      if (first.textNodes.equals(second.textNodes)) {
        LOG.info("dominance found: {} dominates {}", first.getExtendedTag(), second.getExtendedTag());
        first.setDominatedMarkup(second);
      }
    }
  }

}
