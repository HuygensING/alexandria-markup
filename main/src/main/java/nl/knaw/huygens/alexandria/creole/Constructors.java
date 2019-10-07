package nl.knaw.huygens.alexandria.creole;

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

import java.util.ArrayList;

import static nl.knaw.huygens.alexandria.creole.NameClasses.anyName;
import static nl.knaw.huygens.alexandria.creole.NameClasses.name;
import nl.knaw.huygens.alexandria.creole.patterns.*;
import static nl.knaw.huygens.alexandria.creole.patterns.Patterns.EMPTY;
import static nl.knaw.huygens.alexandria.creole.patterns.Patterns.NOT_ALLOWED;
import static nl.knaw.huygens.alexandria.creole.patterns.Patterns.TEXT;

public class Constructors {

  /*
  Constructors

  When we create a derivative, we often need to create a new pattern.
  These constructors take into account special handling of NotAllowed, Empty and After patterns.
   */

  //  choice :: Pattern -> Pattern -> Pattern
  public static Pattern choice(Pattern pattern1, Pattern pattern2) {
    //  choice p NotAllowed = p
    if (pattern1 instanceof NotAllowed) {
      return pattern2;
    }
    //  choice NotAllowed p = p
    if (pattern2 instanceof NotAllowed) {
      return pattern1;
    }
    //  choice Empty Empty = Empty
    if (pattern1 instanceof Empty && pattern2 instanceof Empty) {
      return pattern1;
    }
    //  choice p1 p2 = Choice p1 p2
    return new Choice(pattern1, pattern2);
  }

  //  group :: Pattern -> Pattern -> Pattern
  public static Pattern group(Pattern pattern1, Pattern pattern2) {
    //  group p NotAllowed = NotAllowed
    //  group NotAllowed p = NotAllowed
    if (pattern1 instanceof NotAllowed || pattern2 instanceof NotAllowed) {
      return notAllowed();
    }
    //  group p Empty = p
    if (pattern2 instanceof Empty) {
      return pattern1;
    }
    //  group Empty p = p
    if (pattern1 instanceof Empty) {
      return pattern2;
    }
    //  group (After p1 p2) p3 = after p1 (group p2 p3)
    if (pattern1 instanceof After) {
      After after = (After) pattern1;
      return after(after.getPattern1(), group(after.getPattern2(), pattern2));
    }
    //  group p1 (After p2 p3) = after p2 (group p1 p3)
    if (pattern2 instanceof After) {
      After after = (After) pattern2;
      return after(after.getPattern1(), group(pattern1, after.getPattern2()));
    }
    //  group p1 p2 = Group p1 p2
    return new Group(pattern1, pattern2);
  }

  //  interleave :: Pattern -> Pattern -> Pattern
  public static Pattern interleave(Pattern pattern1, Pattern pattern2) {
    //  interleave p NotAllowed = NotAllowed
    //  interleave NotAllowed p = NotAllowed
    if (pattern1 instanceof NotAllowed || pattern2 instanceof NotAllowed) {
      return notAllowed();
    }
    //  interleave p Empty = p
    if (pattern2 instanceof Empty) {
      return pattern1;
    }
    //  group Empty p = p
    if (pattern1 instanceof Empty) {
      return pattern2;
    }
    //  interleave (After p1 p2) p3 = after p1 (interleave p2 p3)
    if (pattern1 instanceof After) {
      After after = (After) pattern1;
      return after(after.getPattern1(), interleave(after.getPattern2(), pattern2));
    }
    //  interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
    if (pattern2 instanceof After) {
      After after = (After) pattern2;
      return after(after.getPattern1(), interleave(pattern1, after.getPattern2()));
    }
    //  interleave p1 p2 = Interleave p1 p2
    return new Interleave(pattern1, pattern2);
  }

  //  concur :: Pattern -> Pattern -> Pattern
  public static Pattern concur(Pattern pattern1, Pattern pattern2) {
    //  concur p NotAllowed = NotAllowed
    //  concur NotAllowed p = NotAllowed
    if (pattern1 instanceof NotAllowed || pattern2 instanceof NotAllowed) {
      return notAllowed();
    }

    //  concur p Text = p
    if (pattern2 instanceof Text) {
      return pattern1;
    }

    //  concur Text p = p
    if (pattern1 instanceof Text) {
      return pattern2;
    }

    //  concur (After p1 p2) (After p3 p4) = after (all p1 p3) (concur p2 p4)
    if (pattern1 instanceof After && pattern2 instanceof After) {
      After after1 = (After) pattern1;
      Pattern p1 = after1.getPattern1();
      Pattern p2 = after1.getPattern2();
      After after2 = (After) pattern2;
      Pattern p3 = after2.getPattern1();
      Pattern p4 = after2.getPattern2();
      return after(all(p1, p3), concur(p2, p4));
    }

    //  concur (After p1 p2) p3 = after p1 (concur p2 p3)
    if (pattern1 instanceof After) {
      After after = (After) pattern1;
      Pattern p1 = after.getPattern1();
      Pattern p2 = after.getPattern2();
      return after(p1, concur(p2, pattern2));
    }

    //  concur p1 (After p2 p3) = after p2 (concur p1 p3)
    if (pattern2 instanceof After) {
      After after = (After) pattern2;
      Pattern p2 = after.getPattern1();
      Pattern p3 = after.getPattern2();
      return after(p2, concur(pattern1, p3));
    }

    //  concur p1 p2 = Concur p1 p2
    return new Concur(pattern1, pattern2);
  }

  //  partition :: Pattern -> Pattern
  static Pattern partition(Pattern pattern) {
    //  partition NotAllowed = NotAllowed
    //  partition Empty = Empty
    if (pattern instanceof NotAllowed //
        || pattern instanceof Empty) {
      return pattern;
    }
    //  partition p = Partition p
    return new Partition(pattern);
  }

  //  oneOrMore :: Pattern -> Pattern
  static Pattern oneOrMore(Pattern pattern) {
    //  oneOrMore NotAllowed = NotAllowed
    //  oneOrMore Empty = Empty
    if (pattern instanceof NotAllowed //
        || pattern instanceof Empty) {
      return pattern;
    }
    //  oneOrMore p = OneOrMore p
    return new OneOrMore(pattern);
  }

  //  concurOneOrMore :: Pattern -> Pattern
  static Pattern concurOneOrMore(Pattern pattern) {
    //  concurOneOrMore NotAllowed = NotAllowed
    //  concurOneOrMore Empty = Empty
    if (pattern instanceof NotAllowed //
        || pattern instanceof Empty) {
      return pattern;
    }
    //  concurOneOrMore p = ConcurOneOrMore p
    return new ConcurOneOrMore(pattern);
  }

  //  after :: Pattern -> Pattern -> Pattern
  public static Pattern after(Pattern pattern1, Pattern pattern2) {
    //  after p NotAllowed = NotAllowed
    //  after NotAllowed p = NotAllowed
    if (pattern1 instanceof NotAllowed //
        || pattern2 instanceof NotAllowed) {
      return notAllowed();
    }

    //  after Empty p = p
    if (pattern1 instanceof Empty) {
      return pattern2;
    }

    //  after (After p1 p2) p3 = after p1 (after p2 p3)
    if (pattern1 instanceof After) {
      After after = (After) pattern1;
      Pattern p1 = after.getPattern1();
      Pattern p2 = after.getPattern2();
      return after(p1, after(p2, pattern2));
    }

    //  after p1 p2 = After p1 p2
    return new After(pattern1, pattern2);
  }

  //  all :: Pattern -> Pattern -> Pattern
  static Pattern all(Pattern pattern1, Pattern pattern2) {
    //  all p NotAllowed = NotAllowed
    //  all NotAllowed p = NotAllowed
    if (pattern1 instanceof NotAllowed || pattern2 instanceof NotAllowed) {
      return notAllowed();
    }

    //  all p Empty = if nullable p then Empty else NotAllowed
    if (pattern2 instanceof Empty) {
      return pattern1.isNullable() ? empty() : notAllowed();
    }

    //  all Empty p = if nullable p then Empty else NotAllowed
    if (pattern1 instanceof Empty) {
      return pattern2.isNullable() ? empty() : notAllowed();
    }

    //  all (After p1 p2) (After p3 p4) = after (all p1 p3) (all p2 p4)
    if (pattern1 instanceof After && pattern2 instanceof After) {
      After after1 = (After) pattern1;
      Pattern p1 = after1.getPattern1();
      Pattern p2 = after1.getPattern2();
      After after2 = (After) pattern2;
      Pattern p3 = after2.getPattern1();
      Pattern p4 = after2.getPattern2();
      return after(all(p1, p3), all(p2, p4));
    }

    //  all p1 p2 = All p1 p2
    return new All(pattern1, pattern2);
  }

  // convenience method: element is a range in a partition
  static Pattern element(String localName, Pattern pattern) {
    NameClass nameClass = name(localName);
    return partition(range(nameClass, pattern));
  }

  public static Pattern empty() {
    return EMPTY;
  }

  public static Pattern notAllowed() {
    return NOT_ALLOWED;
  }

  public static Pattern text() {
    return TEXT;
  }

  static Pattern range(NameClass nameClass, Pattern pattern) {
    return new Range(nameClass, pattern);
  }

  public static Pattern endRange(Basics.QName qName, Basics.Id id) {
    return new EndRange(qName, id);
  }

  static Pattern endRange(Basics.QName qName, String id) {
    return new EndRange(qName, Basics.id(id));
  }

  static Pattern zeroOrMore(Pattern pattern) {
    return choice(oneOrMore(pattern), empty());
  }

  static Pattern concurZeroOrMore(Pattern pattern) {
    return choice(concurOneOrMore(pattern), empty());
  }

  static Pattern optional(Pattern pattern) {
    return choice(pattern, empty());
  }

  static Pattern mixed(Pattern pattern) {
    return interleave(text(), pattern);
  }

  public static Pattern anyContent() {
    return interleave(text(), anyAtoms());
  }

  static Pattern atom(String name) {
    return new Atom(name(name), new ArrayList<>());
  }

  static Pattern attribute(String name) {
    return annotation(name, text());
  }

  static Pattern annotation(String name, Pattern pattern) {
    return new Annotation(name(name), pattern);
  }

  static Pattern endAnnotation(String name) {
    return new EndAnnotation(name(name));
  }

  static Pattern annotation(NameClass nameClass, Pattern pattern) {
    return new Annotation(nameClass, pattern);
  }

  private static Pattern anyAtoms() {
    return choice(oneOrMoreAtoms(), empty());
  }

  private static Pattern oneOrMoreAtoms() {
    return oneOrMore(anyAtom());
  }

  private static Pattern anyAtom() {
//    return atom(anyName(), anyAnnotations());
    return new Atom(anyName(), null);
  }

//  private static Pattern anyAnnotations() {
//    return null;
//  }

}
