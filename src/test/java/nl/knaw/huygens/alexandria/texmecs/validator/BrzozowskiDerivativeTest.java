package nl.knaw.huygens.alexandria.texmecs.validator;

/*
 * #%L
 * alexandria-markup
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

import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSLexer;
import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;
import nl.knaw.huygens.alexandria.texmecs.importer.TexMECSSyntaxError;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class BrzozowskiDerivativeTest {
  private final Logger LOG = LoggerFactory.getLogger(getClass());

  @Test
  public void testTexMECSValidation1() {
    // from http://www.princexml.com/howcome/2007/xtech/papers/output/0077-30/index.xhtml
    String texMECS = "<book|<page no=\"1\"|\n" +//
        "  <title|Genesis|title>\n" +//
        "  <section|\n" +//
        "    <heading|The flood and the tower of Babel|heading>\n" +//
        "    <chapter no=\"7\"|\n" +//
        "      <para|...<s|<verse no=\"23\"|God wiped out every living thing\n" +//
        "      that existed on earth, <index~1 ref=\"i0037\"|man and\n" +//
        "      <index~2 ref=\"i0038\"|beast|index~1>, reptile|page> \n" +//
        "      <page no=\"74\"|and bird|index~2>; they were all wiped out\n" +//
        "      over the whole earth, and only Noah and his company in the\n" +//
        "      ark survived.|s>|verse>|para>\n" +//
        "      <para|<verse no=\"24\"|<s|When the waters had increased over \n" +//
        "      the earth for a hundred and fifty days, |verse>|chapter> \n" +//
        "      <chapter no=\"8\"|<verse no=\"1\"|God thought of Noah and all\n" +//
        "      the wild animals and the cattle with him in the ark, and\n" +//
        "      he made a wind pass over the earth, and the waters began to\n" +//
        "      subside.|verse>|s>...|para>\n" +//
        "    |chapter>\n" +//
        "  |section>\n" +//
        "|page>|book>";

    String creole = "start = book\n" +//
        "book = element book { page ~ \n" +//
        "                      ( title, ( chapter+ ~ section+ ) ) }\n" +//
        "page = range page { attribute no { text }, text }\n" +//
        "title = element title { text }\n" +//
        "chapter = range chapter { attribute no { text }, verse+ }\n" +//
        "verse = range verse { attribute no { text }, text }\n" +//
        "section = range section { heading, para+ }\n" +//
        "heading = element heading { indexedText }\n" +//
        "para = range para { verse+ ~ s+ }\n" +//
        "s = range s { indexedText }\n" +//
        "indexedText = concurOneOrMore { mixed { index* } }\n" +//
        "index = range index { attribute ref { text }, text }";
    TexMECSSchema schema = new TexMECSSchema(creole);
    TexMECSValidator validator = ValidatorFactory.createTexMECSValidator(schema);
    ValidationReport report = validate(validator, texMECS);
//    assertThat(report.isValidated()).isTrue();
  }

  @Test
  public void testTexMECSValidation2() {
    String validTexMECS = "<text|bla|text>";
    String invalidTexMECS = "<text|<p|some characters|p>|text>";

    String creole = "start = text\n" +//
        "text = element text { text }";
    TexMECSSchema schema = new TexMECSSchema(creole);
    TexMECSValidator validator = ValidatorFactory.createTexMECSValidator(schema);
    ValidationReport report = validate(validator, validTexMECS);
    assertThat(report.isValidated()).isTrue();

    ValidationReport report2 = validate(validator, invalidTexMECS);
    assertThat(report2.isValidated()).isFalse();
  }

  private ValidationReport validate(TexMECSValidator validator, String texMECS) {
    CharStream antlrInputStream = CharStreams.fromString(texMECS);
    TexMECSLexer lexer = new TexMECSLexer(antlrInputStream);
    ErrorListener errorListener = new ErrorListener();
    lexer.addErrorListener(errorListener);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    TexMECSParser parser = new TexMECSParser(tokens);
    parser.addErrorListener(errorListener);
    parser.setBuildParseTree(true);
    ParseTree parseTree = parser.document();
    int numberOfSyntaxErrors = parser.getNumberOfSyntaxErrors();
    LOG.info("parsed with {} syntax errors", numberOfSyntaxErrors);
    ParseTreeWalker parseTreeWalker = new ParseTreeWalker();
    ValidationReport report;
    try {
      parseTreeWalker.walk(validator, parseTree);
      report = validator.getValidationReport();
    } catch (ValidationException e) {
      report = new ValidationReport();
      report.setValidated(false);
    }

    String errorMsg = "";
    if (validator.hasErrors()) {
      String errors = validator.getErrors().stream().collect(Collectors.joining("\n"));
      errorMsg = "Parsing errors:\n" + errors;
    }
    if (numberOfSyntaxErrors > 0) {
      String errors = errorListener.getErrors().stream().collect(Collectors.joining("\n"));
      errorMsg += "\n\nTokenizing errors:\n" + errors;
    }
    if (!errorMsg.isEmpty()) {
      throw new TexMECSSyntaxError(errorMsg);
    }

    return report;

  }
}
