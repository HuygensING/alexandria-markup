package nl.knaw.huygens.alexandria.creole;

/*-
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
import static nl.knaw.huygens.alexandria.creole.Utilities.nullable;

import java.util.List;

public class Validator {

  private final Pattern schemaPattern;

  private Validator(Pattern schemaPattern){
    this.schemaPattern = schemaPattern;
  }

  public static Validator ofSchema(Pattern schemaPattern){
    return new Validator(schemaPattern);
  }

  public boolean validates(List<Event> events){
    Pattern pattern = Derivatives.eventsDeriv(schemaPattern, events);
    return (nullable(pattern));
  }
}
