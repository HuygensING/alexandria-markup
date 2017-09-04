package nl.knaw.huygens.alexandria.lmnl.grammar;

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


import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TAGQLTest {
  Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testCorrectTAGQLStatement() {
    String statement = "select m.text from markup m where m.name='q' and m.id='a'";
    CharStream stream = CharStreams.fromString(statement);
    TAGQLLexer lex = new TAGQLLexer(stream);
    List<? extends Token> allTokens = lex.getAllTokens();
    for (Token token : allTokens) {
      LOG.info("token: [{}] <<{}>>", lex.getRuleNames()[token.getType() - 1], token.getText());
    }
    lex.reset();

    CommonTokenStream tokens = new CommonTokenStream(lex);
    TAGQLParser parser = new TAGQLParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree tree = parser.query();
    LOG.info("tree={}", tree.toStringTree(parser));
    assertThat(tree.getChildCount()).isEqualTo(2);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    assertThat(numberOfSyntaxErrors).isEqualTo(0); // no syntax errors
    LOG.info("numberOfSyntaxErrors={}", numberOfSyntaxErrors);

    for (int i = 0; i < tree.getChildCount(); i++) {
      LOG.info("root.child={}", tree.getChild(i).getText());
    }
    assertThat(allTokens).hasSize(19);

    // MyListener listener = new MyListener();
    // parser.TAGQL_script().enterRule(listener);
    //
    // MyVisitor visitor = new MyVisitor();
    // Object result = visitor.visit(tree);
    // LOG.info("result={}",result);

  }

}
