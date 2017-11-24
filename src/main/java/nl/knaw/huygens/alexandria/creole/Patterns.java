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

import java.util.List;

class Patterns {
  static final Pattern EMPTY = new Empty();
  static final Pattern NOT_ALLOWED = new NotAllowed();
  static final Pattern TEXT = new Text();

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
   */

  static class Empty implements Pattern {
  }

  static class NotAllowed implements Pattern {
  }

  static class Text implements Pattern {
  }

  static class Choice extends PatternWithTwoPatternParameters implements Pattern {
    Choice(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Interleave extends PatternWithTwoPatternParameters implements Pattern {
    Interleave(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Group extends PatternWithTwoPatternParameters implements Pattern {
    Group(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Concur extends PatternWithTwoPatternParameters implements Pattern {
    Concur(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Partition extends PatternWithOnePatternParameter implements Pattern {
    Partition(Pattern pattern) {
      super(pattern);
    }
  }

  static class OneOrMore extends PatternWithOnePatternParameter implements Pattern {
    OneOrMore(Pattern pattern) {
      super(pattern);
    }
  }

  static class ConcurOneOrMore extends PatternWithOnePatternParameter implements Pattern {
    ConcurOneOrMore(Pattern pattern) {
      super(pattern);
    }
  }

  static class Range implements Pattern {
    private final NameClass nameClass;
    private final Pattern pattern;

    public Range(NameClass nameClass, Pattern pattern) {
      this.nameClass = nameClass;
      this.pattern = pattern;
    }

    NameClass getNameClass() {
      return nameClass;
    }

    Pattern getPattern() {
      return pattern;
    }
  }

  static class EndRange implements Pattern {
    private final Basics.QName qName;
    private final Basics.Id id;

    EndRange(Basics.QName qName, Basics.Id id) {
      this.qName = qName;
      this.id = id;
    }

    Basics.QName getQName() {
      return qName;
    }

    public Basics.Id getId() {
      return id;
    }

    @Override
    public String toString() {
      String postfix = id.isEmpty() ? "" : "~" + id;
      return "{" + qName + postfix + "]";
    }
  }

  static class After extends PatternWithTwoPatternParameters implements Pattern {
    public After(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class All extends PatternWithTwoPatternParameters implements Pattern {
    public All(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Atom implements Pattern {
    NameClass nc;
    List<Annotation> annotations;
  }

  static class Annotation implements Pattern {
    private final NameClass nameClass;
    private NameClass nc;
    private Pattern pattern;

    Annotation(NameClass nameClass, Pattern pattern) {
      this.nameClass = nameClass;
      this.pattern = pattern;
    }

    NameClass getNameClass() {
      return nc;
    }

    Pattern getPattern() {
      return pattern;
    }
  }


  static class PatternWithOnePatternParameter {
    private final Pattern pattern;

    PatternWithOnePatternParameter(Pattern pattern) {
      this.pattern = pattern;
    }

    public Pattern getPattern() {
      return pattern;
    }

  }

  static class PatternWithTwoPatternParameters {
    private final Pattern pattern1;
    private final Pattern pattern2;

    PatternWithTwoPatternParameters(Pattern pattern1, Pattern pattern2) {
      this.pattern1 = pattern1;
      this.pattern2 = pattern2;
    }

    Pattern getPattern1() {
      return pattern1;
    }

    Pattern getPattern2() {
      return pattern2;
    }

    @Override
    public boolean equals(Object obj) {
      return obj.getClass().equals(this.getClass())
          && pattern1.equals(((PatternWithTwoPatternParameters) obj).getPattern1())
          && pattern2.equals(((PatternWithTwoPatternParameters) obj).getPattern2());
    }
  }

}
