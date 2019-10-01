package nl.knaw.huygens.alexandria.creole.patterns;

    /*
     * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
     */

import nl.knaw.huygens.alexandria.creole.Pattern;

public class Patterns {
  public static final Pattern EMPTY = new Empty();
  public static final Pattern NOT_ALLOWED = new NotAllowed();
  public static final Pattern TEXT = new Text();

  /*
  A Pattern represents a pattern after simplification.

  data Pattern = Empty
               | NotAllowed
               | Text
               | Choice Pattern Pattern
               | Interleave Pattern Pattern
               | Group Pattern Pattern
               | Concur Pattern Pattern
               | Partition Pattern
               | OneOrMore Pattern
               | ConcurOneOrMore Pattern
               | Range NameClass Pattern
               | EndRange QName Id
               | After Pattern Pattern
               | All Pattern Pattern
               | Atom Pattern
               | EndAtom
               | Annotation NameClass Pattern
               | EndAnnotation NameClass
   */
}
