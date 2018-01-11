package nl.knaw.huygens.alexandria.creole.patterns;

/*-
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
import nl.knaw.huygens.alexandria.creole.Basics;
import static nl.knaw.huygens.alexandria.creole.Constructors.choice;
import static nl.knaw.huygens.alexandria.creole.Constructors.interleave;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class Interleave extends PatternWithTwoPatternParameters {
  public Interleave(Pattern pattern1, Pattern pattern2) {
    super(pattern1, pattern2);
  }

  @Override
  void init() {
    nullable = pattern1.isNullable() && pattern2.isNullable();
    allowsText = pattern1.allowsText() || pattern2.allowsText();
    allowsAnnotations = pattern1.allowsAnnotations() || pattern2.allowsAnnotations();
    onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations();
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    //textDeriv cx (Interleave p1 p2) s =
    //  choice (interleave (textDeriv cx p1 s) p2)
    //         (interleave p1 (textDeriv cx p2 s))
    return choice(//
        interleave(pattern1.textDeriv(cx, s), pattern2),//
        interleave(pattern1, pattern2.textDeriv(cx, s))//
    );
  }

  @Override
  public Pattern startTagDeriv(Basics.QName qn, Basics.Id id) {
    // startTagDeriv (Interleave p1 p2) qn id =
    //   choice (interleave (startTagDeriv p1 qn id) p2)
    //          (interleave p1 (startTagDeriv p2 qn id))
    return choice(//
        interleave(pattern1.startTagDeriv(qn, id), pattern2),//
        interleave(pattern1, pattern2.startTagDeriv(qn, id))//
    );
  }

  @Override
  public Pattern startTagOpenDeriv(Basics.QName qn, Basics.Id id) {
    return choice(//
        interleave(pattern1.startTagOpenDeriv(qn, id), pattern2),//
        interleave(pattern1, pattern2.startTagOpenDeriv(qn, id))//
    );
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id) {
    // endTagDeriv (Interleave p1 p2) qn id =
    //   choice (interleave (endTagDeriv p1 qn id) p2)
    //          (interleave p1 (endTagDeriv p2 qn id))
    return choice(//
        interleave(pattern1.endTagDeriv(qn, id), pattern2),//
        interleave(pattern1, pattern2.endTagDeriv(qn, id))//
    );
  }
}
