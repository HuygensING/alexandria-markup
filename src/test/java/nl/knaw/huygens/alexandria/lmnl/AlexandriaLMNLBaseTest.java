package nl.knaw.huygens.alexandria.lmnl;

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


import nl.knaw.huygens.alexandria.lmnl.grammar.LMNLLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.InputStream;

public class AlexandriaLMNLBaseTest {

  protected void printTokens(String input) {
    // This gets all the tokens at once, it does not stop for errors
    // List<? extends Token> allTokens = grammar.getAllTokens();
    // System.out.println(allTokens);
    System.out.println("LMNL:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    LMNLLexer lexer = new LMNLLexer(inputStream);
    Token token;
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        System.out.println(token + ": " + lexer.getRuleNames()[token.getType() - 1] + ": " + lexer.getModeNames()[lexer._mode]);
      }
    } while (token.getType() != Token.EOF);
  }

}
