package nl.knaw.huygens.alexandria.lmnl.query;

import nl.knaw.huygens.alexandria.lmnl.grammar.LQLLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.LQLParser;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

public class LQLQueryHandler {

  public LQLResult execute(String statement){
    CharStream stream = new ANTLRInputStream(statement);
    LQLLexer lex = new LQLLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(lex);
    LQLParser parser = new LQLParser(tokens);
    // TODO

    return new LQLResult();
  }
}
