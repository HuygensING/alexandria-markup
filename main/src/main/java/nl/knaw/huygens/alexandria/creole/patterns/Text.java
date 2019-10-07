package nl.knaw.huygens.alexandria.creole.patterns;

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
import nl.knaw.huygens.alexandria.creole.Basics;
import static nl.knaw.huygens.alexandria.creole.Constructors.text;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class Text extends PatternWithoutParameters {
  @Override
  void init() {
    nullable = true;
    allowsText = true;
    allowsAnnotations = false;
    onlyAnnotations = false;
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    //textDeriv cx Text _ = Text
    return text();
  }

  @Override
  public String toString() {
    return "Text()";
  }
}
