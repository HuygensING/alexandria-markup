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

import nl.knaw.huygens.alexandria.texmecs.validator.events.OpenTagEvent;
import nl.knaw.huygens.alexandria.texmecs.validator.events.ValidationEvent;

public class TexMECSSchema {
  private String rootElement;

  public TexMECSSchema(String schemaText) {
    // TODO implement something
    rootElement = "text";
  }

  public ValidationState getStartState() {
    return new ValidationState(this);
  }

  public ValidationEvent getRootEvent() {
    return new OpenTagEvent(rootElement);
  }
}
