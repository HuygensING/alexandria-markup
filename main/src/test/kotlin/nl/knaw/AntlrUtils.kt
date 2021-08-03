package nl.knaw

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import de.vandermeer.asciitable.AsciiTable
import de.vandermeer.asciitable.CWC_LongestLine
import de.vandermeer.asciithemes.a7.A7_Grids
import de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token

object AntlrUtils {

    @JvmStatic
    fun makeTokenTable(lexer: Lexer): String {
        val table = AsciiTable()
            .setTextAlignment(TextAlignment.LEFT)
        val cwc = CWC_LongestLine()
        table.renderer.cwc = cwc
        table.addRule()
        table.addRow("Pos", "Text", "Rule", "Next mode", "Token")
        table.addRule()
        var token: Token
        do {
            token = lexer.nextToken()
            //      LOG.info(token.toString());
            if (token.type != Token.EOF) {
                val pos = token.line.toString() + ":" + token.charPositionInLine
                val text = "'" + token.text + "'"
                val rule = lexer.ruleNames[token.type - 1]
                val mode = lexer.modeNames[lexer._mode]
                table.addRow(pos, text, rule, mode, token)
            }
        } while (token.type != Token.EOF)
        table.addRule()
        table.context.grid = A7_Grids.minusBarPlusEquals()
        return table.render()
    }
}
