package nl.knaw;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import de.vandermeer.asciithemes.a7.A7_Grids;
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

public class AntlrUtils {

  public static String makeTokenTable(Lexer lexer) {
    AsciiTable table = new AsciiTable()
        .setTextAlignment(TextAlignment.LEFT);
    CWC_LongestLine cwc = new CWC_LongestLine();
    table.getRenderer().setCWC(cwc);
    table.addRule();
    table.addRow("Pos", "Text", "Rule", "Next mode", "Token");
    table.addRule();

    Token token;
    do {
      token = lexer.nextToken();
//      LOG.info(token.toString());
      if (token.getType() != Token.EOF) {
        String pos = token.getLine() + ":" + token.getCharPositionInLine();
        String text = "'" + token.getText() + "'";
        String rule = lexer.getRuleNames()[token.getType() - 1];
        String mode = lexer.getModeNames()[lexer._mode];
        table.addRow(pos, text, rule, mode, token);
      }
    } while (token.getType() != Token.EOF);
    table.addRule();
    table.getContext().setGrid(A7_Grids.minusBarPlusEquals());
    return table.render();
  }

}
