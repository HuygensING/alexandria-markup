package nl.knaw.huc.di.tag.tagml;

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

import de.vandermeer.asciitable.AsciiTable;
import de.vandermeer.asciitable.CWC_LongestLine;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class TAGMLBaseTest {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLBaseTest.class);

  protected void printTokens(String input) {
    // This gets all the tokens at once, it does not stop for errors
    // List<? extends Token> allTokens = grammar.getAllTokens();
    // System.out.println(allTokens);
    System.out.println("TAGML:");
    System.out.println(input);
    System.out.println("Tokens:");
    printTokens(CharStreams.fromString(input));
  }

  protected void printTokens(InputStream input) throws IOException {
    printTokens(CharStreams.fromStream(input));
  }

  private void printTokens(CharStream inputStream) {
    TAGMLLexer lexer = new TAGMLLexer(inputStream);
    AsciiTable table = new AsciiTable()//
        .setTextAlignment(TextAlignment.LEFT);
    CWC_LongestLine cwc = new CWC_LongestLine();
    table.getRenderer().setCWC(cwc);
    table.addRule();
    table.addRow("Text", "Rule", "Mode", "Token");
    table.addRule();

    Token token;
    do {
      token = lexer.nextToken();
      if (token.getType() != Token.EOF) {
        String ruleName = lexer.getRuleNames()[token.getType() - 1];
        String modeName = lexer.getModeNames()[lexer._mode];
        table.addRow(
            "'" + token.getText() + "'",
            ruleName,
            modeName,
            token
        );
//        System.out.println(token + "\t| " + lexer.getRuleNames()[token.getType() - 1] + "\t| " + lexer.getModeNames()[lexer._mode]);
      }
    } while (token.getType() != Token.EOF);
    table.addRule();
    LOG.info("\nTokens:\n{}\n", table.render());
  }

}
