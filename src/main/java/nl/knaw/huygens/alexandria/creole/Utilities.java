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
import java.util.function.Function;

import static nl.knaw.huygens.alexandria.creole.Basics.qName;
import static nl.knaw.huygens.alexandria.creole.Events.startTagEvent;
import static nl.knaw.huygens.alexandria.creole.Events.textEvent;

//http://www.princexml.com/howcome/2007/xtech/papers/output/0077-30/index.xhtml
class Utilities {

  //  private static final String INDENT = "  ";
  private static final String INDENT = "| ";
  private static final String ELLIPSES = "...";

  /*
        The most important utility function is nullable, which tests whether a given pattern can match an empty sequence
         of events. Nullable is defined as follows for the various kinds of patterns:
         */
  //  nullable:: Pattern -> Bool
  public static Boolean nullable(Pattern pattern) {
    //  nullable Empty = True
    //  nullable Text = True
    if (pattern instanceof Patterns.Empty
        || pattern instanceof Patterns.Text) {
      return true;
    }

    //  nullable NotAllowed = False
    if (pattern instanceof Patterns.NotAllowed) {
      return false;
    }
    //  nullable (Choice p1 p2) = nullable p1 || nullable p2
    if (pattern instanceof Patterns.Choice) {
      Patterns.Choice choice = (Patterns.Choice) pattern;
      return nullable(choice.getPattern1()) || nullable(choice.getPattern2());
    }
    //  nullable (Interleave p1 p2) = nullable p1 && nullable p2
    if (pattern instanceof Patterns.Interleave) {
      Patterns.Interleave interleave = (Patterns.Interleave) pattern;
      return nullable(interleave.getPattern1()) && nullable(interleave.getPattern2());
    }
    //  nullable (Group p1 p2) = nullable p1 && nullable p2
    if (pattern instanceof Patterns.Group) {
      Patterns.Group group = (Patterns.Group) pattern;
      return nullable(group.getPattern1()) && nullable(group.getPattern2());
    }
    //  nullable (Concur p1 p2) = nullable p1 && nullable p2
    if (pattern instanceof Patterns.Concur) {
      Patterns.Concur concur = (Patterns.Concur) pattern;
      return nullable(concur.getPattern1()) && nullable(concur.getPattern2());
    }
    //  nullable (Partition p) = nullable p
    if (pattern instanceof Patterns.Partition) {
      return nullable(((Patterns.Partition) pattern).getPattern());
    }
    //  nullable (OneOrMore p) = nullable p
    if (pattern instanceof Patterns.OneOrMore) {
      return nullable(((Patterns.OneOrMore) pattern).getPattern());
    }
    //  nullable (ConcurOneOrMore p) = nullable p
    if (pattern instanceof Patterns.ConcurOneOrMore) {
      return nullable(((Patterns.ConcurOneOrMore) pattern).getPattern());
    }
    //  nullable (Range _ _) = False
    if (pattern instanceof Patterns.Range) {
      return false;
    }
    //  nullable (EndRange _ _) = False
    if (pattern instanceof Patterns.EndRange) {
      return false;
    }
    //  nullable (After _ _) = False
    if (pattern instanceof Patterns.After) {
//      Patterns.After after = (Patterns.After) pattern;
//      return nullable(after.getPattern1()) && nullable(after.getPattern2());
      return false;
    }
    //  nullable (All p1 p2) = nullable p1 && nullable p2
    if (pattern instanceof Patterns.All) {
      Patterns.All all = (Patterns.All) pattern;
      return nullable(all.getPattern1()) && nullable(all.getPattern2());
    }

    if (pattern instanceof Patterns.Atom || pattern instanceof Patterns.Annotation) {
      return false;
    }

    throw unexpectedPattern(pattern);
  }

  /*
  The second utility function is allowsText, which returns true if the pattern can match text.
  This is important because whitespace-only text events are ignored if text isn't allowed by a pattern.

  allowsText:: Pattern -> Bool
  allowsText (Choice p1 p2) = allowsText p1 || allowsText p2
  allowsText (Group p1 p2) =
    if nullable p1 then (allowsText p1 || allowsText p2)
                   else allowsText p1
  allowsText (Interleave p1 p2) =
    allowsText p1 || allowsText p2
  allowsText (Concur p1 p2) = allowsText p1 && allowsText p2
  allowsText (Partition p) = allowsText p
  allowsText (OneOrMore p) = allowsText p
  allowsText (ConcurOneOrMore p) = allowsText p
  allowsText (After p1 p2) =
    if nullable p1 then (allowsText p1 || allowsText p2)
                   else allowsText p1
  allowsText (All p1 p2) = allowsText p1 && allowsText p2
  allowsText Text = True
  allowsText _ = False
   */

  private static final Map<Class, Function<Pattern, Boolean>> allowsTextMap = new HashMap<>();

  static {
    allowsTextMap.put(Patterns.Choice.class, pattern -> {
      Patterns.Choice choice = (Patterns.Choice) pattern;
      return orCombination(choice.getPattern1(), choice.getPattern2());
    });

    allowsTextMap.put(Patterns.Group.class, pattern -> {
      Patterns.Group group = (Patterns.Group) pattern;
      return nullable(group.getPattern1())//
          ? (orCombination(group.getPattern1(), group.getPattern2()))//
          : allowsText(group.getPattern1());
    });

    allowsTextMap.put(Patterns.Interleave.class, pattern -> {
      Patterns.Interleave interleave = (Patterns.Interleave) pattern;
      return orCombination(interleave.getPattern1(), interleave.getPattern2());
    });

    allowsTextMap.put(Patterns.Concur.class, pattern -> {
      Patterns.Concur concur = (Patterns.Concur) pattern;
      return andCombination(concur.getPattern1(), concur.getPattern2());
    });

    allowsTextMap.put(Patterns.Partition.class, pattern -> {
      Patterns.Partition partition = (Patterns.Partition) pattern;
      return allowsText(partition.getPattern());
    });

    allowsTextMap.put(Patterns.OneOrMore.class, pattern -> {
      Patterns.OneOrMore oneOrMore = (Patterns.OneOrMore) pattern;
      return allowsText(oneOrMore.getPattern());
    });

    allowsTextMap.put(Patterns.ConcurOneOrMore.class, pattern -> {
      Patterns.ConcurOneOrMore concurOneOrMore = (Patterns.ConcurOneOrMore) pattern;
      return allowsText(concurOneOrMore.getPattern());
    });

    allowsTextMap.put(Patterns.After.class, pattern -> {
      Patterns.After group = (Patterns.After) pattern;
      return nullable(group.getPattern1())//
          ? (orCombination(group.getPattern1(), group.getPattern2()))//
          : allowsText(group.getPattern1());
    });

    allowsTextMap.put(Patterns.All.class, pattern -> {
      Patterns.All all = (Patterns.All) pattern;
      return andCombination(all.getPattern1(), all.getPattern2());
    });

    allowsTextMap.put(Patterns.Text.class, pattern -> true);

    allowsTextMap.put(Patterns.Range.class, pattern -> false);
    allowsTextMap.put(Patterns.EndRange.class, pattern -> false);
    allowsTextMap.put(Patterns.Annotation.class, pattern -> false);
    allowsTextMap.put(Patterns.Empty.class, pattern -> false);

  }

  public static Boolean allowsText(Pattern pattern) {
    Function<Pattern, Boolean> function = allowsTextMap.get(pattern.getClass());
    if (function == null) {
      throw unexpectedPattern(pattern);
    }
    return function.apply(pattern);
  }

  /*
  Finally, like Relax NG, Creole needs a method of testing whether a given qualified name matches a given name class:

  contains :: NameClass -> QName -> Bool
  contains AnyName _ = True
  contains (AnyNameExcept nc) n = not (contains nc n)
  contains (NsName ns1) (QName ns2 _) = (ns1 == ns2)
  contains (NsNameExcept ns1 nc) (QName ns2 ln) =
    ns1 == ns2 && not (contains nc (QName ns2 ln))
  contains (Name ns1 ln1) (QName ns2 ln2) =
    (ns1 == ns2) && (ln1 == ln2)
  contains (NameClassChoice nc1 nc2) n =
    (contains nc1 n) || (contains nc2 n)

   */
  public static Boolean contains(NameClass nameClass, Basics.QName qName) {
    if (nameClass instanceof NameClasses.AnyName) {
      return true;
    }
    if (nameClass instanceof NameClasses.AnyNameExcept) {
      NameClasses.AnyNameExcept anyNameExcept = (NameClasses.AnyNameExcept) nameClass;
      return !contains(anyNameExcept.getNameClassToExcept(), qName);
    }
    if (nameClass instanceof NameClasses.NsName) {
      NameClasses.NsName nsName = (NameClasses.NsName) nameClass;
      return nsName.getValue().equals(qName.getUri().getValue());
    }
    if (nameClass instanceof NameClasses.NsNameExcept) {
      NameClasses.NsNameExcept nsNameExcept = (NameClasses.NsNameExcept) nameClass;
      return nsNameExcept.getUri().equals(qName.getUri())
          && !contains(nsNameExcept.getNameClass(), qName);
    }
    if (nameClass instanceof NameClasses.Name) {
      NameClasses.Name name = (NameClasses.Name) nameClass;
      return name.getUri().equals(qName.getUri())
          && name.getLocalName().equals(qName.getLocalName());
    }
    if (nameClass instanceof NameClasses.NameClassChoice) {
      NameClasses.NameClassChoice nameClassChoice = (NameClasses.NameClassChoice) nameClass;
      return contains(nameClassChoice.getNameClass1(), qName)
          || contains(nameClassChoice.getNameClass2(), qName);
    }
    return false;
  }

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

  public static List<Pattern> leafPatterns(Pattern pattern) {
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

  private static boolean andCombination(Pattern pattern1, Pattern pattern2) {
    return allowsText(pattern1) && allowsText(pattern2);
  }

  private static boolean orCombination(Pattern pattern1, Pattern pattern2) {
    return allowsText(pattern1) || allowsText(pattern2);
  }

  private static RuntimeException unexpectedPattern(Pattern pattern) {
    return new RuntimeException("Unexpected " + pattern.getClass().getSimpleName() + " Pattern: " + pattern);
  }

  private static RuntimeException unexpectedNameClass(NameClass nameClass) {
    return new RuntimeException("Unexpected " + nameClass.getClass().getSimpleName() + " NameClass: " + nameClass);
  }
}
