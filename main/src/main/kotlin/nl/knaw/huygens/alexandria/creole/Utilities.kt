package nl.knaw.huygens.alexandria.creole

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

import nl.knaw.huygens.alexandria.creole.patterns.*
import org.apache.commons.lang3.StringUtils
import java.util.*

//http://www.princexml.com/howcome/2007/xtech/papers/output/0077-30/index.xhtml
object Utilities {

    //  private static final String INDENT = "  ";
    private val INDENT = "| "
    private val ELLIPSES = "..."

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
    fun patternTreeToDepth(pattern: Pattern, maxDepth: Int): String {
        return patternTreeToDepth(pattern, 0, maxDepth)
    }

    private fun leafPatterns(pattern: Pattern): List<Pattern> {
        val leafPatterns = ArrayList<Pattern>()
        if (pattern is PatternWithoutParameters || pattern is EndRange) {
            leafPatterns.add(pattern)
            return leafPatterns
        }
        if (pattern is Range) {
            leafPatterns.add(pattern)
            leafPatterns.addAll(leafPatterns(pattern.pattern))
            return leafPatterns
        }
        if (pattern is PatternWithOnePatternParameter) {
            leafPatterns.addAll(leafPatterns(pattern.pattern))
            return leafPatterns
        }
        if (pattern is PatternWithTwoPatternParameters) {
            leafPatterns.addAll(leafPatterns(pattern.pattern1))
            leafPatterns.addAll(leafPatterns(pattern.pattern2))
            return leafPatterns
        }
        throw RuntimeException("unexpected pattern: $pattern")
    }

    /* private */

    private fun patternTreeToDepth(pattern: Pattern, indent: Int, maxDepth: Int): String {
        val patternName = pattern.javaClass.getSimpleName()
        val parameters = parametersToString(pattern, indent, maxDepth)
        return StringUtils.repeat(INDENT, indent) + patternName + parameters
    }

    private fun parametersToString(pattern: Pattern, indent: Int, maxDepth: Int): String {
        val parameterBuilder = StringBuilder("(")
        val innerIndent = StringUtils.repeat(INDENT, indent)
        val goDeeper = indent - 1 < maxDepth
        val nextIndent = indent + 1
        if (pattern is PatternWithOnePatternParameter) {
            if (goDeeper) {
                parameterBuilder.append("\n")//
                        .append(patternTreeToDepth(pattern.pattern, nextIndent, maxDepth))//
                        .append("\n")//
                        .append(innerIndent)
            } else {
                parameterBuilder.append(ELLIPSES)
            }

        } else if (pattern is PatternWithTwoPatternParameters) {
            if (goDeeper) {
                parameterBuilder.append("\n")//
                        .append(patternTreeToDepth(pattern.pattern1, nextIndent, maxDepth))//
                        .append(",\n")//
                        .append(patternTreeToDepth(pattern.pattern2, nextIndent, maxDepth))//
                        .append("\n")//
                        .append(innerIndent)
            } else {
                parameterBuilder.append(ELLIPSES)
            }

        } else if (pattern is Range) {
            parameterBuilder.append(nameClassVisualization(pattern.nameClass))
            if (goDeeper) {
                parameterBuilder.append(",\n")//
                        .append(patternTreeToDepth(pattern.pattern, nextIndent, maxDepth))//
                        .append("\n")//
                        .append(innerIndent)
            } else {
                parameterBuilder.append(",").append(ELLIPSES)
            }

        } else if (pattern is EndRange) {
            parameterBuilder.append("\"").append(pattern.qName.toString()).append("\"")
            parameterBuilder.append(",")
            parameterBuilder.append("\"").append(pattern.id.toString()).append("\"")
        }
        parameterBuilder.append(")")

        return parameterBuilder.toString()
    }

    private fun nameClassVisualization(nameClass: NameClass): String {
        return if (nameClass is NameClasses.Name) {
            "\"" + nameClass.localName.value + "\""
        } else nameClass.javaClass.getSimpleName()
    }

}
