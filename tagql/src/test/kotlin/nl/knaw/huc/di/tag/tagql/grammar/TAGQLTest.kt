package nl.knaw.huc.di.tag.tagql.grammar

/*
 * #%L
 * tagql
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

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class TAGQLTest {
    private val LOG = LoggerFactory.getLogger(javaClass)

    @Test
    fun testCorrectTAGQLStatement() {
        val statement = "select m.text from markup m where m.name='q' and m.id='a'"
        val stream: CharStream = CharStreams.fromString(statement)
        val lex = TAGQLLexer(stream)
        val allTokens = lex.allTokens
        for (token in allTokens) {
            LOG.info("token: [{}] <<{}>>", lex.ruleNames[token.type - 1], token.text)
        }
        lex.reset()
        val tokens = CommonTokenStream(lex)
        val parser = TAGQLParser(tokens)
        parser.buildParseTree = true
        val tree: ParseTree = parser.query()
        LOG.info("tree={}", tree.toStringTree(parser))
        assertThat(tree.childCount).isEqualTo(2)

        val numberOfSyntaxErrors = parser.numberOfSyntaxErrors
        assertThat(numberOfSyntaxErrors).isEqualTo(0) // no syntax errors

        LOG.info("numberOfSyntaxErrors={}", numberOfSyntaxErrors)
        for (i in 0 until tree.childCount) {
            LOG.info("root.child={}", tree.getChild(i).text)
        }
        assertThat(allTokens).hasSize(19)

        // MyListener listener = new MyListener();
        // parser.TAGQL_script().enterRule(listener);

        // MyVisitor visitor = new MyVisitor();
        // Object result = visitor.visit(tree);
        // LOG.info("result={}",result);
    }
}
