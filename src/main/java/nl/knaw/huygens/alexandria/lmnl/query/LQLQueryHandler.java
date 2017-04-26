package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import nl.knaw.huygens.alexandria.lmnl.data_model.Document;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLSelectStatement;
import nl.knaw.huygens.alexandria.lmnl.lql.LQLStatement;

public class LQLQueryHandler {

  private Document document;

  public LQLQueryHandler(Document document) {
    this.document = document;
  }

  public LQLResult execute(String statement) {
    CharStream stream = CharStreams.fromString(statement);
    LQLLexer lex = new LQLLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(lex);
    ParseTree tree = new LQLParser(tokens).lql_script();
    ParseTreeWalker ptw = new ParseTreeWalker();
    LQLQueryListener l = new LQLQueryListener();
    ptw.walk(l, tree);
    List<LQLStatement> statements = l.getStatements();

    LQLResult result = new LQLResult();
    statements.stream()//
        .map(this::execute)//
        .forEach(result::addResult);

    return result;
  }

  LQLResult execute(LQLStatement statement) {
    if (statement instanceof LQLSelectStatement) {
      return executeSelect((LQLSelectStatement) statement);
    }
    LQLResult result = new LQLResult();
    return result;
  }

  LQLResult executeSelect(LQLSelectStatement select) {
    LQLResult result = new LQLResult();
    document.value().textRangeList.stream()//
        .filter(select.getTextRangeFilter())//
        .map(select.getTextRangeMapper())//
    ;
    return result;
  }
}
