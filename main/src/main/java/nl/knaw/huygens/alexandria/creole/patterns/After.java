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
import static nl.knaw.huygens.alexandria.creole.Constructors.after;
import nl.knaw.huygens.alexandria.creole.Pattern;

import java.util.function.Function;

public class After extends PatternWithTwoPatternParameters {
  public After(Pattern pattern1, Pattern pattern2) {
    super(pattern1, pattern2);
  }

  @Override
  void init() {
    nullable = false;
    allowsText = pattern1.isNullable()//
        ? (pattern1.allowsText() || pattern2.allowsText())//
        : pattern1.allowsText();
    allowsAnnotations = pattern1.isNullable()//
        ? (pattern1.allowsAnnotations() || pattern2.allowsAnnotations())//
        : pattern1.allowsAnnotations();
    onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations();
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    //textDeriv cx (After p1 p2) s =
    //  after (textDeriv cx p1 s) p2
    return after(//
        pattern1.textDeriv(cx, s),//
        pattern2//
    );
  }

  @Override
  public Pattern startTagDeriv(Basics.QName qn, Basics.Id id) {
    // startTagDeriv (After p1 p2) qn id =
    //   after (startTagDeriv p1 qn id)
    //         p2
    return after(//
        pattern1.startTagDeriv(qn, id),//
        pattern2//
    );
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id) {
    // endTagDeriv (After p1 p2) qn id =
    //   after (endTagDeriv p1 qn id) p2
    return after(//
        pattern1.endTagDeriv(qn, id),//
        pattern2//
    );
  }

  @Override
  public Pattern applyAfter(Function<Pattern, Pattern> f) {
    return after(//
        pattern1,//
        f.apply(pattern2)//
    );
  }
}
