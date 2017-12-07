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

import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.Events.startTagEvent;
import static nl.knaw.huygens.alexandria.creole.Events.textEvent;

//http://www.princexml.com/howcome/2007/xtech/papers/output/0077-30/index.xhtml
class Utilities {

  //  private static final String INDENT = "  ";
  private static final String INDENT = "| ";
  private static final String ELLIPSES = "...";

  static Set<Event> expectedEvents(Pattern pattern) {
    Set<Event> expectedEvents = new HashSet<>();
    if (pattern instanceof Patterns.Text) {
      expectedEvents.add(textEvent("*"));

    } else if (pattern instanceof Patterns.Choice//
        || pattern instanceof Patterns.Interleave//
        || pattern instanceof Patterns.Group//
        || pattern instanceof Patterns.Concur//
        || pattern instanceof Patterns.After//
        || pattern instanceof Patterns.All//
        ) {
      Patterns.PatternWithTwoPatternParameters p = (Patterns.PatternWithTwoPatternParameters) pattern;
      expectedEvents.addAll(expectedEvents(p.getPattern1()));
      expectedEvents.addAll(expectedEvents(p.getPattern2()));

//    } else if (pattern instanceof Patterns.Group
//        || pattern instanceof Patterns.After//
//        ) {
//      Patterns.PatternWithTwoPatternParameters p = (Patterns.PatternWithTwoPatternParameters) pattern;
//      expectedEvents.addAll(expectedEvents(p.getPattern1()));

    } else if (pattern instanceof Patterns.Partition
        || pattern instanceof Patterns.OneOrMore//
        || pattern instanceof Patterns.ConcurOneOrMore//
        ) {
      Patterns.PatternWithOnePatternParameter p = (Patterns.PatternWithOnePatternParameter) pattern;
      expectedEvents.addAll(expectedEvents(p.getPattern()));

    } else if (pattern instanceof Patterns.Range) {
      Patterns.Range range = (Patterns.Range) pattern;
      expectedEvents.addAll(expectedStartTagEvents(range.getNameClass()));

    } else if (pattern instanceof Patterns.EndRange) {
      Patterns.EndRange endRange = (Patterns.EndRange) pattern;
      expectedEvents.add(new Events.EndTagEvent(endRange.getQName(), endRange.getId()));
    }

    return expectedEvents;
  }

  private static List<Events.StartTagEvent> expectedStartTagEvents(NameClass nameClass) {
    List<Events.StartTagEvent> startTagEvents = new ArrayList<>();

    if (nameClass instanceof NameClasses.AnyName) {
      startTagEvents.add(startTagEvent(qName("*", "*")));

    } else if (nameClass instanceof NameClasses.AnyNameExcept) {
      NameClasses.AnyNameExcept anyNameExcept = (NameClasses.AnyNameExcept) nameClass;
      NameClass nameClassToExcept = anyNameExcept.getNameClassToExcept();
//      startTagEvents.add(startTagEvent(qName("*","*")));

    } else if (nameClass instanceof NameClasses.Name) {
      NameClasses.Name name = (NameClasses.Name) nameClass;
      startTagEvents.add(startTagEvent(new Basics.QName(name.getUri(), name.getLocalName())));

    } else if (nameClass instanceof NameClasses.NsName) {
      NameClasses.NsName nsName = (NameClasses.NsName) nameClass;
//      startTagEvents.add(startTagEvent(qName("*","*")));

    } else if (nameClass instanceof NameClasses.NsNameExcept) {
      NameClasses.NsNameExcept nsNameExcept = (NameClasses.NsNameExcept) nameClass;
//      startTagEvents.add(startTagEvent(qName("*","*")));

    } else if (nameClass instanceof NameClasses.NameClassChoice) {
      NameClasses.NameClassChoice nameClassChoice = (NameClasses.NameClassChoice) nameClass;
      startTagEvents.addAll(expectedStartTagEvents(nameClassChoice.getNameClass1()));
      startTagEvents.addAll(expectedStartTagEvents(nameClassChoice.getNameClass2()));
    }

    return startTagEvents;
  }

  public static String patternTreeToDepth(Pattern pattern, int maxDepth) {
    return patternTreeToDepth(pattern, 0, maxDepth);
  }

  private static List<Pattern> leafPatterns(Pattern pattern) {
    List<Pattern> leafPatterns = new ArrayList<>();
    if (pattern instanceof Patterns.PatternWithoutParameters
        || pattern instanceof Patterns.EndRange) {
      leafPatterns.add(pattern);
      return leafPatterns;
    }
    if (pattern instanceof Patterns.Range) {
      Patterns.Range range = (Patterns.Range) pattern;
      leafPatterns.add(range);
      leafPatterns.addAll(leafPatterns(range.getPattern()));
      return leafPatterns;
    }
    if (pattern instanceof Patterns.PatternWithOnePatternParameter) {
      Patterns.PatternWithOnePatternParameter pattern1 = (Patterns.PatternWithOnePatternParameter) pattern;
      leafPatterns.addAll(leafPatterns(pattern1.getPattern()));
      return leafPatterns;
    }
    if (pattern instanceof Patterns.PatternWithTwoPatternParameters) {
      Patterns.PatternWithTwoPatternParameters pattern1 = (Patterns.PatternWithTwoPatternParameters) pattern;
      leafPatterns.addAll(leafPatterns(pattern1.getPattern1()));
      leafPatterns.addAll(leafPatterns(pattern1.getPattern2()));
      return leafPatterns;
    }
    throw new RuntimeException("unexpected pattern: " + pattern);
  }

  /* private */

  private static String patternTreeToDepth(Pattern pattern, int indent, int maxDepth) {
    String patternName = pattern.getClass().getSimpleName();
    String parameters = parametersToString(pattern, indent, maxDepth);
    return StringUtils.repeat(INDENT, indent) + patternName + parameters;
  }

  private static String parametersToString(Pattern pattern, int indent, int maxDepth) {
    StringBuilder parameterBuilder = new StringBuilder("(");
    String innerIndent = StringUtils.repeat(INDENT, indent);
    boolean goDeeper = indent - 1 < maxDepth;
    int nextIndent = indent + 1;
    if (pattern instanceof Patterns.PatternWithOnePatternParameter) {
      if (goDeeper) {
        Patterns.PatternWithOnePatternParameter p = (Patterns.PatternWithOnePatternParameter) pattern;
        parameterBuilder.append("\n")//
            .append(patternTreeToDepth(p.getPattern(), nextIndent, maxDepth))//
            .append("\n")//
            .append(innerIndent);
      } else {
        parameterBuilder.append(ELLIPSES);
      }

    } else if (pattern instanceof Patterns.PatternWithTwoPatternParameters) {
      if (goDeeper) {
        Patterns.PatternWithTwoPatternParameters p = (Patterns.PatternWithTwoPatternParameters) pattern;
        parameterBuilder.append("\n")//
            .append(patternTreeToDepth(p.getPattern1(), nextIndent, maxDepth))//
            .append(",\n")//
            .append(patternTreeToDepth(p.getPattern2(), nextIndent, maxDepth))//
            .append("\n")//
            .append(innerIndent);
      } else {
        parameterBuilder.append(ELLIPSES);
      }

    } else if (pattern instanceof Patterns.Range) {
      Patterns.Range p = (Patterns.Range) pattern;
      parameterBuilder.append(nameClassVisualization(p.getNameClass()));
      if (goDeeper) {
        parameterBuilder.append(",\n")//
            .append(patternTreeToDepth(p.getPattern(), nextIndent, maxDepth))//
            .append("\n")//
            .append(innerIndent);
      } else {
        parameterBuilder.append(",").append(ELLIPSES);
      }

    } else if (pattern instanceof Patterns.EndRange) {
      Patterns.EndRange p = (Patterns.EndRange) pattern;
      parameterBuilder.append("\"").append(p.getQName().toString()).append("\"");
      parameterBuilder.append(",");
      parameterBuilder.append("\"").append(p.getId().toString()).append("\"");
    }
    parameterBuilder.append(")");

    return parameterBuilder.toString();
  }

  private static String nameClassVisualization(NameClass nameClass) {
    if (nameClass instanceof NameClasses.Name) {
      NameClasses.Name name = (NameClasses.Name) nameClass;
      return "\"" + name.getLocalName().getValue() + "\"";
    }
    return nameClass.getClass().getSimpleName();
  }

  private static RuntimeException unexpectedPattern(Pattern pattern) {
    return new RuntimeException("Unexpected " + pattern.getClass().getSimpleName() + " Pattern: " + pattern);
  }

  private static RuntimeException unexpectedNameClass(NameClass nameClass) {
    return new RuntimeException("Unexpected " + nameClass.getClass().getSimpleName() + " NameClass: " + nameClass);
  }
}
