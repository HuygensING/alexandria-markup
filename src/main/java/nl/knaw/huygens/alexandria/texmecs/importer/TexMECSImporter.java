package nl.knaw.huygens.alexandria.texmecs.importer;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAAGQLParser;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

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
    ParseTree parseTree = new TAAGQLParser(tokens).query();
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();

    Document document = new Document();
    Limen limen = document.value();
    return document;
  }

}
