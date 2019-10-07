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
import static nl.knaw.huygens.alexandria.creole.Constructors.choice;
import static nl.knaw.huygens.alexandria.creole.Constructors.group;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class Group extends PatternWithTwoPatternParameters {
  public Group(Pattern pattern1, Pattern pattern2) {
    super(pattern1, pattern2);
  }

  @Override
  void init() {
    nullable = pattern1.isNullable() && pattern2.isNullable();
    allowsText = pattern1.isNullable()//
        ? (pattern1.allowsText() || pattern2.allowsText())//
        : pattern1.allowsText();
    allowsAnnotations = pattern1.allowsAnnotations() || pattern2.allowsAnnotations();
    onlyAnnotations = pattern1.onlyAnnotations() && pattern2.onlyAnnotations();
  }

  @Override
  public Pattern textDeriv(Basics.Context cx, String s) {
    //textDeriv cx (Group p1 p2) s =
    //  let p = group (textDeriv cx p1 s) p2
    //  in if nullable p1 then choice p (textDeriv cx p2 s)
    //                    else p
    Pattern p = group(pattern1.textDeriv(cx, s), pattern2);
    return pattern1.isNullable()//
        ? choice(p, pattern2.textDeriv(cx, s))//
        : p;
  }

  @Override
  public Pattern startTagDeriv(Basics.QName qn, Basics.Id id) {
    // startTagDeriv (Group p1 p2) qn id =
    //   let d = group (startTagDeriv p1 qn id) p2
    //   in if nullable p1 then choice d (startTagDeriv p2 qn id)
    //                     else d
    Pattern d = group(pattern1.startTagDeriv(qn, id), pattern2);
    return pattern1.isNullable()//
        ? choice(d, pattern2.startTagDeriv(qn, id))//
        : d;
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id) {
    // endTagDeriv (Group p1 p2) qn id =
    //   let p = group (endTagDeriv p1 qn id) p2
    //   if nullable p1 then choice p
    //                             (endTagDeriv p2 qn id)
    //                  else p
    Pattern p = group(pattern1.endTagDeriv(qn, id), pattern2);
    return pattern1.isNullable()//
        ? choice(p, pattern2.endTagDeriv(qn, id))//
        : p;
  }

  @Override
  public Pattern startAnnotationDeriv(Basics.QName qn) {
    Pattern p = group(pattern1, pattern2.startAnnotationDeriv(qn));
    return pattern1.allowsAnnotations()//
        ? null//
        : p;
  }

  @Override
  public Pattern endAnnotationDeriv(Basics.QName qn) {
    Pattern p = group(pattern1.endAnnotationDeriv(qn), pattern2);
    return pattern1.isNullable()//
        ? choice(p, pattern2.endAnnotationDeriv(qn))//
        : p;
  }


}
