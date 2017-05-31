package nl.knaw.huygens.alexandria.lmnl.query;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAAGQLLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAAGQLParser;
import nl.knaw.huygens.alexandria.lmnl.taagql.TAAGQLStatement;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

public class TAAGQLQueryHandler {

  private Document document;

  public TAAGQLQueryHandler(Document document) {
    this.document = document;
  }

  public TAAGQLResult execute(String statement) {
    CharStream stream = CharStreams.fromString(statement);
    TAAGQLLexer lexer = new TAAGQLLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ParseTree parseTree = new TAAGQLParser(tokens).query();
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    TAAGQLQueryListener listener = new TAAGQLQueryListener();
    parseTreeWalker.walk(listener, parseTree);
    List<TAAGQLStatement> statements = listener.getStatements();

    TAAGQLResult result = new TAAGQLResult();
    statements.stream()//
        .map(this::execute)//
        .forEach(result::addResult);

    return result;
  }

  TAAGQLResult execute(TAAGQLStatement statement) {
    return statement.getLimenProcessor()//
        .apply(document.value());
  }

}
