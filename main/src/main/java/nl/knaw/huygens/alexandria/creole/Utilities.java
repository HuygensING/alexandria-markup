package nl.knaw.huygens.alexandria.creole;

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
import nl.knaw.huygens.alexandria.creole.events.EndTagEvent;
import static nl.knaw.huygens.alexandria.creole.events.Events.textEvent;
import nl.knaw.huygens.alexandria.creole.patterns.*;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//http://www.princexml.com/howcome/2007/xtech/papers/output/0077-30/index.xhtml
public class Utilities {

  //  private static final String INDENT = "  ";
  private static final String INDENT = "| ";
  private static final String ELLIPSES = "...";

//  static Set<Event> expectedEvents(Pattern pattern) {
//    Set<Event> expectedEvents = new HashSet<>();
//    if (pattern instanceof Text) {
//      expectedEvents.add(textEvent("*"));
//
//    } else if (pattern instanceof Choice//
//        || pattern instanceof Interleave//
//        || pattern instanceof Group//
//        || pattern instanceof Concur//
//        || pattern instanceof After//
//        || pattern instanceof All//
//        ) {
//      PatternWithTwoPatternParameters p = (PatternWithTwoPatternParameters) pattern;
//      expectedEvents.addAll(expectedEvents(p.getPattern1()));
//      expectedEvents.addAll(expectedEvents(p.getPattern2()));
//
////    } else if (pattern instanceof Group
////        || pattern instanceof After//
////        ) {
////      PatternWithTwoPatternParameters p = (PatternWithTwoPatternParameters) pattern;
////      expectedEvents.addAll(expectedEvents(p.getPattern1()));
//
//    } else if (pattern instanceof Partition
//        || pattern instanceof OneOrMore//
//        || pattern instanceof ConcurOneOrMore//
//        ) {
//      PatternWithOnePatternParameter p = (PatternWithOnePatternParameter) pattern;
//      expectedEvents.addAll(expectedEvents(p.getPattern()));
//
//    } else if (pattern instanceof Range) {
//      Range range = (Range) pattern;
//      expectedEvents.addAll(expectedStartTagEvents(range.getNameClass()));
//
//    } else if (pattern instanceof EndRange) {
//      EndRange endRange = (EndRange) pattern;
//      expectedEvents.add(new EndTagEvent(endRange.getQName(), endRange.getId()));
//    }
//
//    return expectedEvents;
//  }

  //  private static List<StartTagEvent> expectedStartTagEvents(NameClass nameClass) {
//    List<StartTagEvent> startTagEvents = new ArrayList<>();
//
//    if (nameClass instanceof NameClasses.AnyName) {
//      startTagEvents.add(startTagEvent(qName("*", "*")));
//
//    } else if (nameClass instanceof NameClasses.AnyNameExcept) {
//      NameClasses.AnyNameExcept anyNameExcept = (NameClasses.AnyNameExcept) nameClass;
//      NameClass nameClassToExcept = anyNameExcept.getNameClassToExcept();
////      startTagEvents.add(startTagEvent(qName("*","*")));
//
//    } else if (nameClass instanceof NameClasses.Name) {
//      NameClasses.Name name = (NameClasses.Name) nameClass;
//      startTagEvents.add(startTagEvent(new Basics.QName(name.getUri(), name.getLocalName())));
//
//    } else if (nameClass instanceof NameClasses.NsName) {
//      NameClasses.NsName nsName = (NameClasses.NsName) nameClass;
////      startTagEvents.add(startTagEvent(qName("*","*")));
//
//    } else if (nameClass instanceof NameClasses.NsNameExcept) {
//      NameClasses.NsNameExcept nsNameExcept = (NameClasses.NsNameExcept) nameClass;
////      startTagEvents.add(startTagEvent(qName("*","*")));
//
//    } else if (nameClass instanceof NameClasses.NameClassChoice) {
//      NameClasses.NameClassChoice nameClassChoice = (NameClasses.NameClassChoice) nameClass;
//      startTagEvents.addAll(expectedStartTagEvents(nameClassChoice.getNameClass1()));
//      startTagEvents.addAll(expectedStartTagEvents(nameClassChoice.getNameClass2()));
//    }
//
//    return startTagEvents;
//  }
//
  public static String patternTreeToDepth(Pattern pattern, int maxDepth) {
    return patternTreeToDepth(pattern, 0, maxDepth);
  }

  private static List<Pattern> leafPatterns(Pattern pattern) {
    List<Pattern> leafPatterns = new ArrayList<>();
    if (pattern instanceof PatternWithoutParameters
        || pattern instanceof EndRange) {
      leafPatterns.add(pattern);
      return leafPatterns;
    }
    if (pattern instanceof Range) {
      Range range = (Range) pattern;
      leafPatterns.add(range);
      leafPatterns.addAll(leafPatterns(range.getPattern()));
      return leafPatterns;
    }
    if (pattern instanceof PatternWithOnePatternParameter) {
      PatternWithOnePatternParameter pattern1 = (PatternWithOnePatternParameter) pattern;
      leafPatterns.addAll(leafPatterns(pattern1.getPattern()));
      return leafPatterns;
    }
    if (pattern instanceof PatternWithTwoPatternParameters) {
      PatternWithTwoPatternParameters pattern1 = (PatternWithTwoPatternParameters) pattern;
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
    if (pattern instanceof PatternWithOnePatternParameter) {
      if (goDeeper) {
        PatternWithOnePatternParameter p = (PatternWithOnePatternParameter) pattern;
        parameterBuilder.append("\n")//
            .append(patternTreeToDepth(p.getPattern(), nextIndent, maxDepth))//
            .append("\n")//
            .append(innerIndent);
      } else {
        parameterBuilder.append(ELLIPSES);
      }

    } else if (pattern instanceof PatternWithTwoPatternParameters) {
      if (goDeeper) {
        PatternWithTwoPatternParameters p = (PatternWithTwoPatternParameters) pattern;
        parameterBuilder.append("\n")//
            .append(patternTreeToDepth(p.getPattern1(), nextIndent, maxDepth))//
            .append(",\n")//
            .append(patternTreeToDepth(p.getPattern2(), nextIndent, maxDepth))//
            .append("\n")//
            .append(innerIndent);
      } else {
        parameterBuilder.append(ELLIPSES);
      }

    } else if (pattern instanceof Range) {
      Range p = (Range) pattern;
      parameterBuilder.append(nameClassVisualization(p.getNameClass()));
      if (goDeeper) {
        parameterBuilder.append(",\n")//
            .append(patternTreeToDepth(p.getPattern(), nextIndent, maxDepth))//
            .append("\n")//
            .append(innerIndent);
      } else {
        parameterBuilder.append(",").append(ELLIPSES);
      }

    } else if (pattern instanceof EndRange) {
      EndRange p = (EndRange) pattern;
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

}
