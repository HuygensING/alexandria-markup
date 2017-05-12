package nl.knaw.huygens.alexandria.lmnl.grammar;

import org.antlr.runtime.RecognitionException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.AlexandriaLMNLBaseTest;

/**
 * Created by Ronald Haentjens Dekker on 28/12/16.
 */
public class TestTheLMNLLexer extends AlexandriaLMNLBaseTest {

  static final Logger LOG = LoggerFactory.getLogger(TestTheLMNLLexer.class);

  @Test
  public void testLexerTextWithOneRange() throws RecognitionException {
    String input = "[l}He manages to keep the upper hand{l]";
    printTokens(input);
  }

  @Test
  public void testLexerComplexExample() throws RecognitionException {
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
