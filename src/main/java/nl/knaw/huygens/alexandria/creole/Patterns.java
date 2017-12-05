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
               | Atom Pattern
               | EndAtom
               | Annotation NameClass Pattern
               | EndAnnotation NameClass
   */

  static class Empty extends PatternWithoutParameters {
    @Override
    public String toString() {
      return "Empty()";
    }
  }

  static class NotAllowed extends PatternWithoutParameters {
    @Override
    public String toString() {
      return "NotAllowed()";
    }
  }

  static class Text extends PatternWithoutParameters {
    @Override
    public String toString() {
      return "Text()";
    }
  }

  static class Choice extends PatternWithTwoPatternParameters {
    Choice(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Interleave extends PatternWithTwoPatternParameters {
    Interleave(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Group extends PatternWithTwoPatternParameters {
    Group(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Concur extends PatternWithTwoPatternParameters {
    Concur(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Partition extends PatternWithOnePatternParameter {
    Partition(Pattern pattern) {
      super(pattern);
    }
  }

  static class OneOrMore extends PatternWithOnePatternParameter {
    OneOrMore(Pattern pattern) {
      super(pattern);
    }
  }

  static class ConcurOneOrMore extends PatternWithOnePatternParameter {
    ConcurOneOrMore(Pattern pattern) {
      super(pattern);
    }
  }

  static class Range extends AbstractPattern {
    private final NameClass nameClass;
    private final Pattern pattern;

    public Range(NameClass nameClass, Pattern pattern) {
      this.nameClass = nameClass;
      this.pattern = pattern;
      setHashcode(getClass().hashCode() * nameClass.hashCode() * pattern.hashCode());
    }

    NameClass getNameClass() {
      return nameClass;
    }

    Pattern getPattern() {
      return pattern;
    }

    @Override
    public String toString() {
      return "[" + nameClass + ">";
    }
  }

  static class EndRange extends AbstractPattern {
    private final Basics.QName qName;
    private final Basics.Id id;

    EndRange(Basics.QName qName, Basics.Id id) {
      this.qName = qName;
      this.id = id;
      setHashcode(getClass().hashCode() * qName.hashCode() * id.hashCode());
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
      return "<" + qName + postfix + "]";
    }
  }

  static class After extends PatternWithTwoPatternParameters {
    public After(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class All extends PatternWithTwoPatternParameters {
    public All(Pattern pattern1, Pattern pattern2) {
      super(pattern1, pattern2);
    }
  }

  static class Atom extends AbstractPattern {
    final NameClass nc;
    final List<Annotation> annotations;

    Atom(NameClass nc, List<Annotation> annotations) {
      this.nc = nc;
      this.annotations = annotations;
    }
  }

  static class Annotation extends AbstractPattern {
    private final NameClass nameClass;
    private final Pattern pattern;

    Annotation(NameClass nameClass, Pattern pattern) {
      this.nameClass = nameClass;
      this.pattern = pattern;
      setHashcode(getClass().hashCode() * nameClass.hashCode() * pattern.hashCode());
    }

    NameClass getNameClass() {
      return nameClass;
    }

    Pattern getPattern() {
      return pattern;
    }
  }

  static class EndAnnotation extends AbstractPattern {
    private final NameClass nameClass;

    EndAnnotation(NameClass nameClass) {
      this.nameClass = nameClass;
      setHashcode(getClass().hashCode() * nameClass.hashCode());
    }

    NameClass getNameClass() {
      return nameClass;
    }
  }

  static class PatternWithOnePatternParameter extends AbstractPattern {
    private final Pattern pattern;

    PatternWithOnePatternParameter(Pattern pattern) {
      this.pattern = pattern;
      setHashcode(getClass().hashCode() * pattern.hashCode());
    }

    public Pattern getPattern() {
      return pattern;
    }

  }

  static class PatternWithoutParameters extends AbstractPattern {

  }

  static class PatternWithTwoPatternParameters extends AbstractPattern {
    private final Pattern pattern1;
    private final Pattern pattern2;

    PatternWithTwoPatternParameters(Pattern pattern1, Pattern pattern2) {
      this.pattern1 = pattern1;
      this.pattern2 = pattern2;
      setHashcode(getClass().hashCode() * pattern1.hashCode() * pattern2.hashCode());
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

  static class AbstractPattern implements Pattern {
    int hashcode = getClass().hashCode();

    void setHashcode(int hashcode) {
      this.hashcode = hashcode;
    }

    @Override
    public int hashCode() {
      return hashcode;
    }
  }

}
