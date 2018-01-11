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
import nl.knaw.huygens.alexandria.creole.Pattern;

import java.util.function.Function;

public class Choice extends PatternWithTwoPatternParameters {
  public Choice(Pattern pattern1, Pattern pattern2) {
    super(pattern1, pattern2);
  }

  @Override
  void init() {
    nullable = pattern1.isNullable() || pattern2.isNullable();
    allowsText = pattern1.allowsText() || pattern2.allowsText();
    allowsAnnotations = pattern1.allowsAnnotations() || pattern2.allowsAnnotations();
    onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations();
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    // textDeriv cx (Choice p1 p2) s =
    //  choice (textDeriv cx p1 s) (textDeriv cx p2 s)
    return choice(//
        pattern1.textDeriv(cx, s),//
        pattern2.textDeriv(cx, s)//
    );
  }

  @Override
  public Pattern startTagDeriv(Basics.QName qn, Basics.Id id) {
    // startTagDeriv (Choice p1 p2) qn id =
    //   choice (startTagDeriv p1 qn id)
    //          (startTagDeriv p2 qn id)
    return choice(//
        pattern1.startTagDeriv(qn, id),//
        pattern2.startTagDeriv(qn, id)//
    );
  }

  public Pattern startTagOpenDeriv(Basics.QName qn, Basics.Id id) {
    return choice(//
        pattern1.startTagOpenDeriv(qn, id),//
        pattern2.startTagOpenDeriv(qn, id)//
    );
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id) {
    // endTagDeriv (Choice p1 p2) qn id =
    //   choice (endTagDeriv p1 qn id)
    //          (endTagDeriv p2 qn id)
    return choice(//
        pattern1.endTagDeriv(qn, id),//
        pattern2.endTagDeriv(qn, id)//
    );
  }

  @Override
  public Pattern startAnnotationDeriv(Basics.QName qn) {
    return choice(//
        pattern1.startAnnotationDeriv(qn),//
        pattern2.startAnnotationDeriv(qn)//
    );
  }

  @Override
  public Pattern endAnnotationDeriv(Basics.QName qn) {
    return choice(//
        pattern1.endAnnotationDeriv(qn),//
        pattern2.endAnnotationDeriv(qn)//
    );
  }

  @Override
  public Pattern applyAfter(Function<Pattern, Pattern> f) {
    return choice(//
        pattern1.applyAfter(f),//
        pattern2.applyAfter(f)//
    );
  }

}
