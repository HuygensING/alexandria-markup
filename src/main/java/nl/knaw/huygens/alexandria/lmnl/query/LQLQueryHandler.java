package nl.knaw.huygens.alexandria.lmnl.query;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLStatement;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

public class LQLQueryHandler {

  private Document document;

  public LQLQueryHandler(Document document) {
    this.document = document;
  }

  public LQLResult execute(String statement) {
    CharStream stream = CharStreams.fromString(statement);
    LQLLexer lqlLexer = new LQLLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(lqlLexer);
    ParseTree parseTree = new LQLParser(tokens).lqlScript();
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    LQLQueryListener lqlQueryListener = new LQLQueryListener();
    parseTreeWalker.walk(lqlQueryListener, parseTree);
    List<LQLStatement> statements = lqlQueryListener.getStatements();

    LQLResult result = new LQLResult();
    statements.stream()//
        .map(this::execute)//
        .forEach(result::addResult);

    return result;
  }

  LQLResult execute(LQLStatement statement) {
    return statement.getLimenProcessor()//
        .apply(document.value());
  }

}
