package nl.knaw.huygens.alexandria.creole;

/*
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

public class Constructors {

  /*
  Constructors

  When we create a derivative, we often need to create a new pattern.
  These constructors take into account special handling of NotAllowed, Empty and After patterns.
   */

  //  choice :: Pattern -> Pattern -> Pattern
  public static Pattern choice(Pattern pattern1, Pattern pattern2) {
    //  choice p NotAllowed = p
    if (pattern1 instanceof Patterns.NotAllowed) {
      return pattern2;
    }
    //  choice NotAllowed p = p
    if (pattern2 instanceof Patterns.NotAllowed) {
      return pattern1;
    }
    //  choice Empty Empty = Empty
    if (pattern1 instanceof Patterns.Empty && pattern2 instanceof Patterns.Empty) {
      return pattern1;
    }
    //  choice p1 p2 = Choice p1 p2
    return Patterns.choice(pattern1, pattern2);
  }

  //  group :: Pattern -> Pattern -> Pattern
  public static Pattern group(Pattern pattern1, Pattern pattern2) {
    //  group p NotAllowed = NotAllowed
    //  group NotAllowed p = NotAllowed
    if (pattern1 instanceof Patterns.NotAllowed || pattern2 instanceof Patterns.NotAllowed) {
      return Patterns.notAllowed();
    }
    //  group p Empty = p
    if (pattern2 instanceof Patterns.Empty) {
      return pattern1;
    }
    //  group Empty p = p
    if (pattern1 instanceof Patterns.Empty) {
      return pattern2;
    }
    //  group (After p1 p2) p3 = after p1 (group p2 p3)
    if (pattern1 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern1;
      return after(after.getPattern1(), group(after.getPattern2(), pattern2));
    }
    //  group p1 (After p2 p3) = after p2 (group p1 p3)
    if (pattern2 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern2;
      return after(after.getPattern1(), group(pattern1, after.getPattern2()));
    }
    //  group p1 p2 = Group p1 p2
    return Patterns.group(pattern1, pattern2);
  }

  //  interleave :: Pattern -> Pattern -> Pattern
  public static Pattern interleave(Pattern pattern1, Pattern pattern2) {
    //  interleave p NotAllowed = NotAllowed
    //  interleave NotAllowed p = NotAllowed
    if (pattern1 instanceof Patterns.NotAllowed || pattern2 instanceof Patterns.NotAllowed) {
      return Patterns.notAllowed();
    }
    //  interleave p Empty = p
    if (pattern2 instanceof Patterns.Empty) {
      return pattern1;
    }
    //  group Empty p = p
    if (pattern1 instanceof Patterns.Empty) {
      return pattern2;
    }
    //  interleave (After p1 p2) p3 = after p1 (interleave p2 p3)
    if (pattern1 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern1;
      return after(after.getPattern1(), interleave(after.getPattern2(), pattern2));
    }
    //  interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
    if (pattern2 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern2;
      return after(after.getPattern1(), interleave(pattern1, after.getPattern2()));
    }
    //  interleave p1 p2 = Interleave p1 p2
    return Patterns.interleave(pattern1, pattern2);
  }

  //  concur :: Pattern -> Pattern -> Pattern
  public static Pattern concur(Pattern pattern1, Pattern pattern2) {
    //  concur p NotAllowed = NotAllowed
    //  concur NotAllowed p = NotAllowed
    if (pattern1 instanceof Patterns.NotAllowed || pattern2 instanceof Patterns.NotAllowed) {
      return Patterns.notAllowed();
    }

    //  concur p Text = p
    if (pattern2 instanceof Patterns.Text) {
      return pattern1;
    }

    //  concur Text p = p
    if (pattern1 instanceof Patterns.Text) {
      return pattern2;
    }

    //  concur (After p1 p2) (After p3 p4) = after (all p1 p3) (concur p2 p4)
    if (pattern1 instanceof Patterns.After && pattern2 instanceof Patterns.After) {
      Patterns.After after1 = (Patterns.After) pattern1;
      Pattern p1 = after1.getPattern1();
      Pattern p2 = after1.getPattern2();
      Patterns.After after2 = (Patterns.After) pattern2;
      Pattern p3 = after2.getPattern1();
      Pattern p4 = after2.getPattern2();
      return after(all(p1, p3), concur(p2, p4));
    }

    //  concur (After p1 p2) p3 = after p1 (concur p2 p3)
    if (pattern1 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern1;
      Pattern p1 = after.getPattern1();
      Pattern p2 = after.getPattern2();
      return after(p1, concur(p2, pattern2));
    }

    //  concur p1 (After p2 p3) = after p2 (concur p1 p3)
    if (pattern2 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern2;
      Pattern p2 = after.getPattern1();
      Pattern p3 = after.getPattern2();
      return after(p2, concur(pattern1, p3));
    }

    //  concur p1 p2 = Concur p1 p2
    return Patterns.concur(pattern1, pattern2);
  }

  //  partition :: Pattern -> Pattern
  public static Pattern partition(Pattern pattern) {
    //  partition NotAllowed = NotAllowed
    if (pattern instanceof Patterns.NotAllowed) {
      return pattern;
    }
    //  partition Empty = Empty
    if (pattern instanceof Patterns.Empty) {
      return pattern;
    }
    //  partition p = Partition p
    return Patterns.partition(pattern);
  }

  //  oneOrMore :: Pattern -> Pattern
  public static Pattern oneOrMore(Pattern pattern) {
    //  oneOrMore NotAllowed = NotAllowed
    if (pattern instanceof Patterns.NotAllowed) {
      return pattern;
    }
    //  oneOrMore Empty = Empty
    if (pattern instanceof Patterns.Empty) {
      return pattern;
    }
    //  oneOrMore p = OneOrMore p
    return Patterns.oneOrMore(pattern);
  }

  //  concurOneOrMore :: Pattern -> Pattern
  public static Pattern concurOneOrMore(Pattern pattern) {
    //  concurOneOrMore NotAllowed = NotAllowed
    if (pattern instanceof Patterns.NotAllowed) {
      return pattern;
    }
    //  concurOneOrMore Empty = Empty
    if (pattern instanceof Patterns.Empty) {
      return pattern;
    }
    //  concurOneOrMore p = ConcurOneOrMore p
    return Patterns.concurOneOrMore(pattern);
  }

  //  after :: Pattern -> Pattern -> Pattern
  public static Pattern after(Pattern pattern1, Pattern pattern2) {
    //  after p NotAllowed = NotAllowed
    //  after NotAllowed p = NotAllowed
    if (pattern1 instanceof Patterns.NotAllowed || pattern2 instanceof Patterns.NotAllowed) {
      return Patterns.notAllowed();
    }

    //  after Empty p = p
    if (pattern1 instanceof Patterns.Empty) {
      return pattern2;
    }

    //  after (After p1 p2) p3 = after p1 (after p2 p3)
    if (pattern1 instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern1;
      Pattern p1 = after.getPattern1();
      Pattern p2 = after.getPattern2();
      return after(p1, after(p2, pattern2));
    }

    //  after p1 p2 = After p1 p2
    return Patterns.after(pattern1, pattern2);
  }

  //  all :: Pattern -> Pattern -> Pattern
  public static Pattern all(Pattern pattern1, Pattern pattern2) {
    //  all p NotAllowed = NotAllowed
    //  all NotAllowed p = NotAllowed
    if (pattern1 instanceof Patterns.NotAllowed || pattern2 instanceof Patterns.NotAllowed) {
      return Patterns.notAllowed();
    }

    //  all p Empty = if nullable p then Empty else NotAllowed
    if (pattern2 instanceof Patterns.Empty) {
      return Utilities.nullable(pattern1) ? Patterns.empty() : Patterns.notAllowed();
    }

    //  all Empty p = if nullable p then Empty else NotAllowed
    if (pattern2 instanceof Patterns.Empty) {
      return Utilities.nullable(pattern2) ? Patterns.empty() : Patterns.notAllowed();
    }

    //  all (After p1 p2) (After p3 p4) = after (all p1 p3) (all p2 p4)
    if (pattern1 instanceof Patterns.After && pattern2 instanceof Patterns.After) {
      Patterns.After after1 = (Patterns.After) pattern1;
      Pattern p1 = after1.getPattern1();
      Pattern p2 = after1.getPattern2();
      Patterns.After after2 = (Patterns.After) pattern2;
      Pattern p3 = after2.getPattern1();
      Pattern p4 = after2.getPattern2();
      return after(all(p1, p3), all(p2, p4));
    }

    //  all p1 p2 = All p1 p2
    return Patterns.all(pattern1, pattern2);
  }
}
