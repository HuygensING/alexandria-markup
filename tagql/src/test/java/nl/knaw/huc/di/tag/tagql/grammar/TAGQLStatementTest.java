package nl.knaw.huc.di.tag.tagql.grammar;

/*
 * #%L
 * tagql
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Stream.concat;
import static org.assertj.core.api.Assertions.assertThat;

public class TAGQLStatementTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @ParameterizedTest(name = "#{index} - TAGQL statement: \"{0}\" should be valid: {1}")
  @ArgumentsSource(CustomArgumentsProvider.class)
  public void testTAGQLStatementHasExpectedValidity(String statement, Boolean isValid) {
    LOG.info("TAGQL={}", statement);
    CharStream stream = CharStreams.fromString(statement);
    TAGQLLexer lex = new TAGQLLexer(stream);
    // List<? extends Token> allTokens = lex.getAllTokens();
    // for (Token token : allTokens) {
    // LOG.info("token: [{}] <<{}>>", lex.getRuleNames()[token.getType() - 1], token.getText());
    // }
    // lex.reset();

    CommonTokenStream tokens = new CommonTokenStream(lex);
    TAGQLParser parser = new TAGQLParser(tokens);
    parser.setBuildParseTree(true);
    ParseTree tree = parser.query();
    LOG.info("tree={}", tree.toStringTree(parser));
    // assertThat(tree.getChildCount()).isEqualTo(2);

    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("numberOfSyntaxErrors={}", numberOfSyntaxErrors);
    boolean isOk = numberOfSyntaxErrors == 0;
    assertThat(isOk).isEqualTo(isValid);
  }

  private static class CustomArgumentsProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      Stream<Arguments> correctStatements =
          loadStatements("correct_statements.tagql").stream().map(it -> Arguments.of(it, true));
      Stream<Arguments> incorrectStatements =
          loadStatements("incorrect_statements.tagql").stream().map(it -> Arguments.of(it, false));
      return concat(correctStatements, incorrectStatements);
    }

    private Collection<String> loadStatements(String name) {
      List<String> statements = new ArrayList<>();
      InputStream inputStream = TAGQLStatementTest.class.getResourceAsStream(name);
      BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
      String line;
      try {
        while ((line = reader.readLine()) != null) {
          statements.add(line);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
      return statements;
    }
  }
}
