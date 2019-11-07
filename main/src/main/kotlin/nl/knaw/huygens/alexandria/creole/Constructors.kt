package nl.knaw.huygens.alexandria.creole

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

import nl.knaw.huygens.alexandria.creole.NameClasses.anyName
import nl.knaw.huygens.alexandria.creole.NameClasses.name
import nl.knaw.huygens.alexandria.creole.patterns.*
import nl.knaw.huygens.alexandria.creole.patterns.Annotation
import nl.knaw.huygens.alexandria.creole.patterns.Patterns.EMPTY
import nl.knaw.huygens.alexandria.creole.patterns.Patterns.NOT_ALLOWED
import nl.knaw.huygens.alexandria.creole.patterns.Patterns.TEXT
import java.util.*

object Constructors {

    /*
  Constructors

  When we create a derivative, we often need to create a new pattern.
  These constructors take into account special handling of NotAllowed, Empty and After patterns.
   */

    //  choice :: Pattern -> Pattern -> Pattern
    fun choice(pattern1: Pattern, pattern2: Pattern): Pattern {
        //  choice p NotAllowed = p
        if (pattern1 is NotAllowed) {
            return pattern2
        }
        //  choice NotAllowed p = p
        if (pattern2 is NotAllowed) {
            return pattern1
        }
        //  choice Empty Empty = Empty
        return if (pattern1 is Empty && pattern2 is Empty) {
            pattern1
        } else Choice(pattern1, pattern2)
        //  choice p1 p2 = Choice p1 p2
    }

    //  group :: Pattern -> Pattern -> Pattern
    fun group(pattern1: Pattern, pattern2: Pattern): Pattern {
        //  group p NotAllowed = NotAllowed
        //  group NotAllowed p = NotAllowed
        if (pattern1 is NotAllowed || pattern2 is NotAllowed) {
            return notAllowed()
        }
        //  group p Empty = p
        if (pattern2 is Empty) {
            return pattern1
        }
        //  group Empty p = p
        if (pattern1 is Empty) {
            return pattern2
        }
        //  group (After p1 p2) p3 = after p1 (group p2 p3)
        if (pattern1 is After) {
            return after(pattern1.pattern1, group(pattern1.pattern2, pattern2))
        }
        //  group p1 (After p2 p3) = after p2 (group p1 p3)
        return if (pattern2 is After) {
            after(pattern2.pattern1, group(pattern1, pattern2.pattern2))
        } else Group(pattern1, pattern2)
        //  group p1 p2 = Group p1 p2
    }

    //  interleave :: Pattern -> Pattern -> Pattern
    fun interleave(pattern1: Pattern, pattern2: Pattern): Pattern {
        //  interleave p NotAllowed = NotAllowed
        //  interleave NotAllowed p = NotAllowed
        if (pattern1 is NotAllowed || pattern2 is NotAllowed) {
            return notAllowed()
        }
        //  interleave p Empty = p
        if (pattern2 is Empty) {
            return pattern1
        }
        //  group Empty p = p
        if (pattern1 is Empty) {
            return pattern2
        }
        //  interleave (After p1 p2) p3 = after p1 (interleave p2 p3)
        if (pattern1 is After) {
            return after(pattern1.pattern1, interleave(pattern1.pattern2, pattern2))
        }
        //  interleave p1 (After p2 p3) = after p2 (interleave p1 p3)
        return if (pattern2 is After) {
            after(pattern2.pattern1, interleave(pattern1, pattern2.pattern2))
        } else Interleave(pattern1, pattern2)
        //  interleave p1 p2 = Interleave p1 p2
    }

    //  concur :: Pattern -> Pattern -> Pattern
    fun concur(pattern1: Pattern, pattern2: Pattern): Pattern {
        //  concur p NotAllowed = NotAllowed
        //  concur NotAllowed p = NotAllowed
        if (pattern1 is NotAllowed || pattern2 is NotAllowed) {
            return notAllowed()
        }

        //  concur p Text = p
        if (pattern2 is Text) {
            return pattern1
        }

        //  concur Text p = p
        if (pattern1 is Text) {
            return pattern2
        }

        //  concur (After p1 p2) (After p3 p4) = after (all p1 p3) (concur p2 p4)
        if (pattern1 is After && pattern2 is After) {
            val p1 = pattern1.pattern1
            val p2 = pattern1.pattern2
            val p3 = pattern2.pattern1
            val p4 = pattern2.pattern2
            return after(all(p1, p3), concur(p2, p4))
        }

        //  concur (After p1 p2) p3 = after p1 (concur p2 p3)
        if (pattern1 is After) {
            val p1 = pattern1.pattern1
            val p2 = pattern1.pattern2
            return after(p1, concur(p2, pattern2))
        }

        //  concur p1 (After p2 p3) = after p2 (concur p1 p3)
        if (pattern2 is After) {
            val p2 = pattern2.pattern1
            val p3 = pattern2.pattern2
            return after(p2, concur(pattern1, p3))
        }

        //  concur p1 p2 = Concur p1 p2
        return Concur(pattern1, pattern2)
    }

    //  partition :: Pattern -> Pattern
    internal fun partition(pattern: Pattern): Pattern {
        //  partition NotAllowed = NotAllowed
        //  partition Empty = Empty
        return if (pattern is NotAllowed //
                || pattern is Empty) {
            pattern
        } else Partition(pattern)
        //  partition p = Partition p
    }

    //  oneOrMore :: Pattern -> Pattern
    internal fun oneOrMore(pattern: Pattern): Pattern {
        //  oneOrMore NotAllowed = NotAllowed
        //  oneOrMore Empty = Empty
        return if (pattern is NotAllowed //
                || pattern is Empty) {
            pattern
        } else OneOrMore(pattern)
        //  oneOrMore p = OneOrMore p
    }

    //  concurOneOrMore :: Pattern -> Pattern
    internal fun concurOneOrMore(pattern: Pattern): Pattern {
        //  concurOneOrMore NotAllowed = NotAllowed
        //  concurOneOrMore Empty = Empty
        return if (pattern is NotAllowed //
                || pattern is Empty) {
            pattern
        } else ConcurOneOrMore(pattern)
        //  concurOneOrMore p = ConcurOneOrMore p
    }

    //  after :: Pattern -> Pattern -> Pattern
    fun after(pattern1: Pattern, pattern2: Pattern): Pattern {
        //  after p NotAllowed = NotAllowed
        //  after NotAllowed p = NotAllowed
        if (pattern1 is NotAllowed //
                || pattern2 is NotAllowed) {
            return notAllowed()
        }

        //  after Empty p = p
        if (pattern1 is Empty) {
            return pattern2
        }

        //  after (After p1 p2) p3 = after p1 (after p2 p3)
        if (pattern1 is After) {
            val p1 = pattern1.pattern1
            val p2 = pattern1.pattern2
            return after(p1, after(p2, pattern2))
        }

        //  after p1 p2 = After p1 p2
        return After(pattern1, pattern2)
    }

    //  all :: Pattern -> Pattern -> Pattern
    internal fun all(pattern1: Pattern, pattern2: Pattern): Pattern {
        //  all p NotAllowed = NotAllowed
        //  all NotAllowed p = NotAllowed
        if (pattern1 is NotAllowed || pattern2 is NotAllowed) {
            return notAllowed()
        }

        //  all p Empty = if nullable p then Empty else NotAllowed
        if (pattern2 is Empty) {
            return if (pattern1.isNullable) empty() else notAllowed()
        }

        //  all Empty p = if nullable p then Empty else NotAllowed
        if (pattern1 is Empty) {
            return if (pattern2.isNullable) empty() else notAllowed()
        }

        //  all (After p1 p2) (After p3 p4) = after (all p1 p3) (all p2 p4)
        if (pattern1 is After && pattern2 is After) {
            val p1 = pattern1.pattern1
            val p2 = pattern1.pattern2
            val p3 = pattern2.pattern1
            val p4 = pattern2.pattern2
            return after(all(p1, p3), all(p2, p4))
        }

        //  all p1 p2 = All p1 p2
        return All(pattern1, pattern2)
    }

    // convenience method: element is a range in a partition
    internal fun element(localName: String, pattern: Pattern): Pattern {
        val nameClass = name(localName)
        return partition(range(nameClass, pattern))
    }

    fun empty(): Pattern {
        return EMPTY
    }

    fun notAllowed(): Pattern {
        return NOT_ALLOWED
    }

    fun text(): Pattern {
        return TEXT
    }

    internal fun range(nameClass: NameClass, pattern: Pattern): Pattern {
        return Range(nameClass, pattern)
    }

    fun endRange(qName: Basics.QName, id: Basics.Id): Pattern {
        return EndRange(qName, id)
    }

    internal fun endRange(qName: Basics.QName, id: String): Pattern {
        return EndRange(qName, Basics.id(id))
    }

    internal fun zeroOrMore(pattern: Pattern): Pattern {
        return choice(oneOrMore(pattern), empty())
    }

    internal fun concurZeroOrMore(pattern: Pattern): Pattern {
        return choice(concurOneOrMore(pattern), empty())
    }

    internal fun optional(pattern: Pattern): Pattern {
        return choice(pattern, empty())
    }

    internal fun mixed(pattern: Pattern): Pattern {
        return interleave(text(), pattern)
    }

    fun anyContent(): Pattern {
        return interleave(text(), anyAtoms())
    }

    internal fun atom(name: String): Pattern {
        return Atom(name(name), ArrayList<Annotation>())
    }

    internal fun attribute(name: String): Pattern {
        return annotation(name, text())
    }

    internal fun annotation(name: String, pattern: Pattern): Pattern {
        return Annotation(name(name), pattern)
    }

    internal fun endAnnotation(name: String): Pattern {
        return EndAnnotation(name(name))
    }

    internal fun annotation(nameClass: NameClass, pattern: Pattern): Pattern {
        return Annotation(nameClass, pattern)
    }

    private fun anyAtoms(): Pattern {
        return choice(oneOrMoreAtoms(), empty())
    }

    private fun oneOrMoreAtoms(): Pattern {
        return oneOrMore(anyAtom())
    }

    private fun anyAtom(): Pattern {
        //    return atom(anyName(), anyAnnotations());
        return Atom(anyName(), null)
    }

    //  private static Pattern anyAnnotations() {
    //    return null;
    //  }

}
