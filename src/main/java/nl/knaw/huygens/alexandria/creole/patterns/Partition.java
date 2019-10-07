package nl.knaw.huygens.alexandria.creole.patterns;

/*-
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
import nl.knaw.huygens.alexandria.creole.Basics;
import static nl.knaw.huygens.alexandria.creole.Constructors.after;
import static nl.knaw.huygens.alexandria.creole.Constructors.empty;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class Partition extends PatternWithOnePatternParameter {
  public Partition(Pattern pattern) {
    super(pattern);
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    //For Partition, we create an After pattern that contains the derivative.
    //
    //textDeriv cx (Partition p) s =
    //  after (textDeriv cx p s) Empty
    return after(//
        pattern.textDeriv(cx, s),//
        empty()//
    );
  }

  @Override
  public Pattern startTagDeriv(Basics.QName qn, Basics.Id id) {
    // startTagDeriv (Partition p) qn id =
    //   after (startTagDeriv p qn id) Empty
    return after(//
        pattern.startTagDeriv(qn, id),//
        empty()//
    );
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id) {
    // endTagDeriv (Partition p) qn id =
    //   after (endTagDeriv p qn id)
    //         Empty
    return after(//
        pattern.endTagDeriv(qn, id),//
        empty()//
    );
  }
}
