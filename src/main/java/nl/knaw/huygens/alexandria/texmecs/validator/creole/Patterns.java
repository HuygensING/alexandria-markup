package nl.knaw.huygens.alexandria.texmecs.validator.creole;

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

public class Patterns {
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

  public class Empty implements Pattern {
  }

  public class NotAllowed implements Pattern {
  }

  public class Text implements Pattern {
  }

  public class Choice extends PatternWithTwoPatternParameters implements Pattern {
    public Choice(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  public class Interleave extends PatternWithTwoPatternParameters implements Pattern {
    public Interleave(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  public class Group extends PatternWithTwoPatternParameters implements Pattern {
    public Group(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  public class Concur extends PatternWithTwoPatternParameters implements Pattern {
    public Concur(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  public class Partition extends PatternWithOnePatternParameter implements Pattern {
    public Partition(Pattern pattern) {
      super(pattern);
    }
  }

  public class OneOrMore extends PatternWithOnePatternParameter implements Pattern {
    public OneOrMore(Pattern pattern) {
      super(pattern);
    }
  }

  public class ConcurOneOrMore extends PatternWithOnePatternParameter implements Pattern {
    public ConcurOneOrMore(Pattern pattern) {
      super(pattern);
    }
  }

  public class Range implements Pattern {
    private final NameClass nameClass;
    private final Pattern pattern;

    public Range(NameClass nameClass, Pattern pattern) {
      this.nameClass = nameClass;
      this.pattern = pattern;
    }

    public NameClass getNameClass() {
      return nameClass;
    }

    public Pattern getPattern() {
      return pattern;
    }
  }

  public class EndRange implements Pattern {
    private final Basics.QName qName;
    private final Basics.Id id;

    public EndRange(Basics.QName qName, Basics.Id id) {
      this.qName = qName;
      this.id = id;
    }

    public Basics.QName getQName() {
      return qName;
    }

    public Basics.Id getId() {
      return id;
    }
  }

  public class After extends PatternWithTwoPatternParameters implements Pattern {
    public After(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  public class All extends PatternWithTwoPatternParameters implements Pattern {
    public All(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  /* private classes */

  private class PatternWithOnePatternParameter {
    private final Pattern pattern;

    PatternWithOnePatternParameter(Pattern pattern) {
      this.pattern = pattern;
    }

    public Pattern getPattern() {
      return pattern;
    }

  }

  private class PatternWithTwoPatternParameters {
    private final Pattern pattern1;
    private final Pattern pattern2;

    PatternWithTwoPatternParameters(Pattern pattern1, Pattern pattern2) {
      this.pattern1 = pattern1;
      this.pattern2 = pattern2;
    }

    public Pattern getPattern1() {
      return pattern1;
    }

    public Pattern getPattern2() {
      return pattern2;
    }
  }

}
