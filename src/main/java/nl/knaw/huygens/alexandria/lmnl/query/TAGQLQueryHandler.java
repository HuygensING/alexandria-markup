package nl.knaw.huygens.alexandria.lmnl.query;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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
