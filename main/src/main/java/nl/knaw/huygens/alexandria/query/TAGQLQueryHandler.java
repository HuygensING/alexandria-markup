package nl.knaw.huygens.alexandria.query;

/*
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

import nl.knaw.huc.di.tag.tagql.TAGQLStatement;
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLLexer;
import nl.knaw.huc.di.tag.tagql.grammar.TAGQLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.List;

public class TAGQLQueryHandler {

  private final DocumentWrapper document;

  public TAGQLQueryHandler(DocumentWrapper document) {
    this.document = document;
  }

  public TAGQLResult execute(String statement) {
    CharStream stream = CharStreams.fromString(statement);
    ErrorListener errorListener = new ErrorListener();
    TAGQLLexer lexer = new TAGQLLexer(stream);
    lexer.addErrorListener(errorListener);

    CommonTokenStream tokens = new CommonTokenStream(lexer);
    TAGQLParser tagqlParser = new TAGQLParser(tokens);
    tagqlParser.addErrorListener(errorListener);
    ParseTree parseTree = tagqlParser.query();
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    TAGQLQueryListener listener = new TAGQLQueryListener();
    parseTreeWalker.walk(listener, parseTree);
    List<TAGQLStatement> statements = listener.getStatements();

    TAGQLResult result = new TAGQLResult(statement);
    statements.stream()//
        .map(this::execute)//
        .forEach(result::addResult);
    result.getErrors().addAll(errorListener.getErrors());
    return result;
  }

  private TAGQLResult execute(TAGQLStatement statement) {
    return statement.getLimenProcessor()//
        .apply(document);
  }

}
