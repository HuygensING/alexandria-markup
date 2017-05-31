package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TAGQLParser;
import nl.knaw.huygens.alexandria.lmnl.tagql.TAGQLStatement;

public class TAGQLQueryHandler {

  private Document document;

  public TAGQLQueryHandler(Document document) {
    this.document = document;
  }

  public TAGQLResult execute(String statement) {
    CharStream stream = CharStreams.fromString(statement);
    TAGQLLexer lexer = new TAGQLLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(lexer);
    ParseTree parseTree = new TAGQLParser(tokens).query();
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    TAGQLQueryListener listener = new TAGQLQueryListener();
    parseTreeWalker.walk(listener, parseTree);
    List<TAGQLStatement> statements = listener.getStatements();

    TAGQLResult result = new TAGQLResult();
    statements.stream()//
        .map(this::execute)//
        .forEach(result::addResult);

    return result;
  }

  TAGQLResult execute(TAGQLStatement statement) {
    return statement.getLimenProcessor()//
        .apply(document.value());
  }

}
