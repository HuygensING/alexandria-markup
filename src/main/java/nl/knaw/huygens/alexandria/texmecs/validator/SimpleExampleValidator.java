package nl.knaw.huygens.alexandria.texmecs.validator;

import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;

import java.util.Arrays;
import java.util.List;

public class SimpleExampleValidator extends AbstractTexMECSValidator {

  List<String> expectedEvents = Arrays.asList("<text|","some text","|text>");
  @Override
  public void enterText(TexMECSParser.TextContext ctx) {

  }
}
