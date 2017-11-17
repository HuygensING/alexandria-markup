package nl.knaw.huygens.alexandria.texmecs.validator;

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

import nl.knaw.huygens.alexandria.lmnl.grammar.TexMECSParser;
import static nl.knaw.huygens.alexandria.texmecs.validator.ValidationState.StateValue.valid;
import nl.knaw.huygens.alexandria.texmecs.validator.events.CloseTagEvent;
import nl.knaw.huygens.alexandria.texmecs.validator.events.OpenTagEvent;
import nl.knaw.huygens.alexandria.texmecs.validator.events.TextEvent;
import nl.knaw.huygens.alexandria.texmecs.validator.events.ValidationEvent;

public class SimpleExampleValidator extends TexMECSBaseValidator {
  private ValidationState validationState;

  SimpleExampleValidator(ValidationState initiaLState) {
    validationState = initiaLState;
  }

  @Override
  public void exitStartTag(TexMECSParser.StartTagContext ctx) {
    ValidationEvent event = new OpenTagEvent(ctx.eid().getText());
    validationState.process(event);
  }

  @Override
  public void exitEndTag(TexMECSParser.EndTagContext ctx) {
    ValidationEvent event = new CloseTagEvent(ctx.gi().getText());
    validationState.process(event);
  }

  @Override
  public void exitText(TexMECSParser.TextContext ctx) {
    ValidationEvent event = new TextEvent(ctx.getText());
    validationState.process(event);
  }

  @Override
  public void exitDocument(TexMECSParser.DocumentContext ctx) {
    super.exitDocument(ctx);
    getValidationReport().setValidated(validationState.getValue().equals(valid));
  }
}
