package nl.knaw.huygens.alexandria.creole.patterns;

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
import nl.knaw.huygens.alexandria.creole.Basics;
import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class ConcurOneOrMore extends PatternWithOnePatternParameter {
  public ConcurOneOrMore(Pattern pattern) {
    super(pattern);
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    //For ConcurOneOrMore, we partially expand the ConcurOneOrMore into a Concur. This mirrors the derivative for
    // OneOrMore, except that a new Concur pattern is constructed rather than a Group, and the second sub-pattern is a
    // choice between a ConcurOneOrMore and Text.
    //
    //textDeriv cx (ConcurOneOrMore p) s =
    //  concur (textDeriv cx p s)
    //         (choice (ConcurOneOrMore p) Text)
    return concur(//
        pattern.textDeriv(cx, s),//
        choice(new ConcurOneOrMore(pattern), text())//
    );
  }

  @Override
  public Pattern startTagDeriv(Basics.QName qn, Basics.Id id) {
    // startTagDeriv (ConcurOneOrMore p) qn id =
    //   concur (startTagDeriv p qn id)
    //          (choice (ConcurOneOrMore p) anyContent)
    return concur(//
        pattern.startTagDeriv(qn, id),//
        choice(new ConcurOneOrMore(pattern), anyContent())//
    );
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id) {
    // endTagDeriv (ConcurOneOrMore p) qn id =
    //   concur (endTagDeriv p qn id)
    //          (choice (ConcurOneOrMore p) anyContent)
    return concur(//
        pattern.endTagDeriv(qn, id),//
        choice(new ConcurOneOrMore(pattern), anyContent())//
    );
  }
}
