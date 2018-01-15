package nl.knaw.huygens.alexandria.lmnl.grammar;

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


import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;
import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Ronald Haentjens Dekker on 28/12/16.
 */
public class TestTheLMNLLexer extends AlexandriaLMNLBaseTest {

  static final Logger LOG = LoggerFactory.getLogger(TestTheLMNLLexer.class);

  @Test
  public void testLexerTextWithOneRange() {
    String input = "[l}He manages to keep the upper hand{l]";
    printTokens(input);
  }

  @Test
  public void testLexerComplexExample() {
    String input = "[excerpt\n"//
        + "  [source [date}1915{][title}The Housekeeper{]]\n"//
        + "  [author\n"//
        + "    [name}Robert Frost{]\n"//
        + "    [dates}1874-1963{]] }\n"//
        + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
        + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
        + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n" + "{excerpt]";
    printTokens(input);
  }
}
