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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.Utilities.contains;

class Derivatives {
  private static final Logger LOG = LoggerFactory.getLogger(Derivatives.class);
  private final ValidationErrorListener errorListener;

  public Derivatives(ValidationErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /*
    Derivatives

    Finally, we can look at the calculation of derivatives.
    A document is a sequence of events.
    The derivative for a sequence of events against a pattern is the derivative of the remaining events
    against the derivative of the first event.
  */
  // eventsDeriv :: Pattern -> [Event] -> Pattern
  Pattern eventsDeriv(Pattern pattern, List<Event> events) {
//    LOG.debug("expected events: {}", expectedEvents(pattern).stream().map(Event::toString).sorted().distinct().collect(toList()));
//    LOG.debug("pattern:\n{}", patternTreeToDepth(pattern, 10));
//    LOG.debug("leafpatterns:\n{}", leafPatterns(pattern).stream().map(Pattern::toString).distinct().collect(toList()));
    // eventsDeriv p [] = p
    if (events.isEmpty()) {
      LOG.debug("\n{}", Utilities.patternTreeToDepth(pattern, 20));
      return pattern;
    }

    //  eventsDeriv p (h:t) = eventsDeriv (eventDeriv p h) t
    Event head = events.remove(0);
    LOG.debug("{}: {}", head.getClass().getSimpleName(), head);
    Pattern headDeriv = eventsDeriv(pattern, head);
//    LOG.debug("\n{}", Utilities.patternTreeToDepth(headDeriv, 20));

    if (headDeriv instanceof Patterns.NotAllowed) {
      // fail fast
      LOG.error("Unexpected " + head.getClass().getSimpleName() + ": {}", head);
      errorListener.setUnexpectedEvent(head);
      return notAllowed();
    }
    return eventsDeriv(headDeriv, events);
  }

  /*
    The derivative for an event depends on the kind of event, and we use different functions for each kind.
     Whitespace-only text nodes can be ignored if the pattern doesn't allow text.
    */
  //  eventDeriv :: Pattern -> Event -> Pattern
  static Pattern eventsDeriv(Pattern p, Event event) {
    //  eventDeriv p (TextEvent s cx) =
    //    if (whitespace s && not allowsText p)
    //    then p
    //    else (textDeriv cx p s)
    if (event instanceof Events.TextEvent) {
      Events.TextEvent textEvent = (Events.TextEvent) event;
      String s = textEvent.getText();
      Basics.Context cx = textEvent.getContext();
      if (whitespace(s) && !Utilities.allowsText(p)) {
        return p;
      } else {
        return textDeriv(cx, p, s);
      }
    }
    // eventDeriv p (StartTagEvent qn id) = startTagDeriv p qn id
    if (event instanceof Events.StartTagEvent) {
      Events.StartTagEvent startTagEvent = (Events.StartTagEvent) event;
      Basics.QName qn = startTagEvent.getQName();
      Basics.Id id = startTagEvent.getId();
      return startTagDeriv(p, qn, id);
    }
    //    eventDeriv p (EndTagEvent qn id) = endTagDeriv p qn id
    if (event instanceof Events.EndTagEvent) {
      Events.EndTagEvent endTagEvent = (Events.EndTagEvent) event;
      Basics.QName qn = endTagEvent.getQName();
      Basics.Id id = endTagEvent.getId();
      return endTagDeriv(p, qn, id);
    }

    if (event instanceof Events.EndTagEvent) {
      Events.EndTagEvent endTagEvent = (Events.EndTagEvent) event;
      Basics.QName qn = endTagEvent.getQName();
      Basics.Id id = endTagEvent.getId();
      return endTagDeriv(p, qn, id);
    }

    throw new RuntimeException("unexpected " + event.getClass().getTypeName() + " event: " + event);
  }


  /*
  Text Derivatives

  textDeriv computes the derivative of a pattern with respect to a text event.
   */
  //textDeriv :: Context -> Pattern -> String -> Pattern
  private static Pattern textDeriv(Basics.Context cx, Pattern pattern, String s) {
    //    For Choice, Group, Interleave and the other standard Relax NG patterns, the derivative is just the same as in
    // Relax NG:
    //
    //textDeriv cx (Choice p1 p2) s =
    //  choice (textDeriv cx p1 s) (textDeriv cx p2 s)
    if (pattern instanceof Patterns.Choice) {
      Patterns.Choice choice = (Patterns.Choice) pattern;
      Pattern p1 = choice.getPattern1();
      Pattern p2 = choice.getPattern2();
      return choice(//
          textDeriv(cx, p1, s),//
          textDeriv(cx, p2, s)//
      );
    }

    //textDeriv cx (Interleave p1 p2) s =
    //  choice (interleave (textDeriv cx p1 s) p2)
    //         (interleave p1 (textDeriv cx p2 s))
    if (pattern instanceof Patterns.Interleave) {
      Patterns.Interleave interleave = (Patterns.Interleave) pattern;
      Pattern p1 = interleave.getPattern1();
      Pattern p2 = interleave.getPattern2();
      return choice(
          interleave(textDeriv(cx, p1, s), p2),
          interleave(p1, textDeriv(cx, p2, s))
      );
    }

    //textDeriv cx (Group p1 p2) s =
    //  let p = group (textDeriv cx p1 s) p2
    //  in if nullable p1 then choice p (textDeriv cx p2 s)
    //                    else p
    if (pattern instanceof Patterns.Group) {
      Patterns.Group group = (Patterns.Group) pattern;
      Pattern p1 = group.getPattern1();
      Pattern p2 = group.getPattern2();
      Pattern p = group(textDeriv(cx, p1, s), p2);
      return p1.isNullable()//
          ? choice(p, textDeriv(cx, p2, s))//
          : p;
    }

    //textDeriv cx (After p1 p2) s =
    //  after (textDeriv cx p1 s) p2
    if (pattern instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern;
      Pattern p1 = after.getPattern1();
      Pattern p2 = after.getPattern2();
      return after(//
          textDeriv(cx, p1, s),//
          p2//
      );
    }

    //textDeriv cx (OneOrMore p) s =
    //  group (textDeriv cx p s) (choice (OneOrMore p) Empty)
    if (pattern instanceof Patterns.OneOrMore) {
      Patterns.OneOrMore oneOrMore = (Patterns.OneOrMore) pattern;
      Pattern p = oneOrMore.getPattern();
      return group(//
          textDeriv(cx, p, s),//
          choice(new Patterns.OneOrMore(p), empty())//
      );
    }

    //textDeriv cx Text _ = Text
    if (pattern instanceof Patterns.Text) {
      return pattern;
    }

    //For Concur, text is only allowed if it is allowed by both of the sub-patterns: we create a new Concur whose
    // sub-patterns are the derivatives of the original sub-patterns.
    //
    //textDeriv cx (Concur p1 p2) s =
    //  concur (textDeriv cx p1 s)
    //         (textDeriv cx p2 s)
    if (pattern instanceof Patterns.Concur) {
      Patterns.Concur concur = (Patterns.Concur) pattern;
      Pattern p1 = concur.getPattern1();
      Pattern p2 = concur.getPattern2();
      return concur(//
          textDeriv(cx, p1, s),//
          textDeriv(cx, p2, s)//
      );
    }

    //For ConcurOneOrMore, we partially expand the ConcurOneOrMore into a Concur. This mirrors the derivative for
    // OneOrMore, except that a new Concur pattern is constructed rather than a Group, and the second sub-pattern is a
    // choice between a ConcurOneOrMore and Text.
    //
    //textDeriv cx (ConcurOneOrMore p) s =
    //  concur (textDeriv cx p s)
    //         (choice (ConcurOneOrMore p) Text)
    if (pattern instanceof Patterns.ConcurOneOrMore) {
      Patterns.ConcurOneOrMore concurOneOrMore = (Patterns.ConcurOneOrMore) pattern;
      Pattern p = concurOneOrMore.getPattern();
      return choice(//
          textDeriv(cx, p, s),//
          choice(new Patterns.ConcurOneOrMore(p), text())//
      );
    }

    //For Partition, we create an After pattern that contains the derivative.
    //
    //textDeriv cx (Partition p) s =
    //  after (textDeriv cx p s) Empty
    if (pattern instanceof Patterns.Partition) {
      Patterns.Partition partition = (Patterns.Partition) pattern;
      Pattern p = partition.getPattern();
      return after(//
          textDeriv(cx, p, s),//
          empty()//
      );
    }

    //No other patterns can match a text event; the default is specified as
    //
    //textDeriv _ _ _ = NotAllowed
    return notAllowed();
  }


  // Start-tag Derivatives
  //
  // Start tags are handled in a very generic way by all the patterns, except the Range pattern,
  // whose derivative is a group of the content pattern for the range followed by an EndRange pattern for the range.
  // Note that the EndRange pattern is created with the same qualified name and ID as the matched range.
  private static final Map<String, TriFunction<Pattern, Basics.QName, Basics.Id, ? extends Pattern>> startTagDerivFunctions//
      = new HashMap<>();

  static {
    startTagDerivFunctions.put("Range",//
        (range, qn, id) -> startTagDerivForRange((Patterns.Range) range, qn, id));
    startTagDerivFunctions.put("Choice",//
        (choice, qn, id) -> startTagDerivForChoice((Patterns.Choice) choice, qn, id));
    startTagDerivFunctions.put("Group",//
        (group, qn, id) -> startTagDerivForGroup((Patterns.Group) group, qn, id));
    startTagDerivFunctions.put("Interleave",//
        (interleave, qn, id) -> startTagDerivForInterleave((Patterns.Interleave) interleave, qn, id));
    startTagDerivFunctions.put("Concur",//
        (concur, qn, id) -> startTagDerivForConcur((Patterns.Concur) concur, qn, id));
    startTagDerivFunctions.put("Partition",//
        (partition, qn, id) -> startTagDerivForPartition((Patterns.Partition) partition, qn, id));
    startTagDerivFunctions.put("OneOrMore",//
        (oneOrMore, qn, id) -> startTagDerivForOneOrMore((Patterns.OneOrMore) oneOrMore, qn, id));
    startTagDerivFunctions.put("ConcurOneOrMore",//
        (concurOneOrMore, qn, id) -> startTagDerivForConcurOneOrMore((Patterns.ConcurOneOrMore) concurOneOrMore, qn, id));
    startTagDerivFunctions.put("After",//
        (after, qn, id) -> startTagDerivForAfter((Patterns.After) after, qn, id));
  }

  //    startTagDeriv (Range nc p) qn id =
  //    if contains nc qn then group p (EndRange qn id)
  //                    else NotAllowed
  private static Pattern startTagDerivForRange(Patterns.Range range, Basics.QName qn, Basics.Id id) {
    NameClass nc = range.getNameClass();
    Pattern p = range.getPattern();
    return (contains(nc, qn))//
        ? group(p, endRange(qn, id))//
        : notAllowed();
  }

  //    startTagDeriv (Choice p1 p2) qn id =
  //        choice (startTagDeriv p1 qn id)
  //    (startTagDeriv p2 qn id)
  private static Pattern startTagDerivForChoice(Patterns.Choice choice, Basics.QName qn, Basics.Id id) {
    Pattern p1 = choice.getPattern1();
    Pattern p2 = choice.getPattern2();
    return choice(//
        startTagDeriv(p1, qn, id),//
        startTagDeriv(p2, qn, id)//
    );
  }

  // startTagDeriv (Group p1 p2) qn id =
  //   let d = group (startTagDeriv p1 qn id) p2
  //   in if nullable p1 then choice d (startTagDeriv p2 qn id)
  //                     else d
  private static Pattern startTagDerivForGroup(Patterns.Group group, Basics.QName qn, Basics.Id id) {
    Pattern p1 = group.getPattern1();
    Pattern p2 = group.getPattern2();
    Pattern d = group(startTagDeriv(p1, qn, id), p2);
    return p1.isNullable()//
        ? choice(d, startTagDeriv(p2, qn, id))//
        : d;
  }

  // startTagDeriv (Interleave p1 p2) qn id =
  //   choice (interleave (startTagDeriv p1 qn id) p2)
  //          (interleave p1 (startTagDeriv p2 qn id))
  private static Pattern startTagDerivForInterleave(Patterns.Interleave interleave, Basics.QName qn, Basics.Id id) {
    Pattern p1 = interleave.getPattern1();
    Pattern p2 = interleave.getPattern2();
    return choice(//
        interleave(startTagDeriv(p1, qn, id), p2),//
        interleave(p1, startTagDeriv(p2, qn, id))//
    );
  }

  // startTagDeriv (Concur p1 p2) qn id =
  //   let d1 = startTagDeriv p1 qn id
  //       d2 = startTagDeriv p2 qn id
  //   in choice (choice (concur d1 p2) (concur p1 d2))
  //             (concur d1 d2)
  private static Pattern startTagDerivForConcur(Patterns.Concur concur, Basics.QName qn, Basics.Id id) {
    Pattern p1 = concur.getPattern1();
    Pattern p2 = concur.getPattern2();
    Pattern d1 = startTagDeriv(p1, qn, id);
    Pattern d2 = startTagDeriv(p2, qn, id);
    return choice(//
        choice(//
            concur(d1, p2),//
            concur(p1, d2)//
        ),//
        concur(d1, d2)//
    );
  }

  // startTagDeriv (Partition p) qn id =
  //   after (startTagDeriv p qn id) Empty
  private static Pattern startTagDerivForPartition(Patterns.Partition partition, Basics.QName qn, Basics.Id id) {
    Pattern p = partition.getPattern();
    return after(//
        startTagDeriv(p, qn, id),//
        empty()//
    );
  }

  // startTagDeriv (OneOrMore p) qn id =
  //   group (startTagDeriv p qn id)
  //         (choice (OneOrMore p) Empty)
  private static Pattern startTagDerivForOneOrMore(Patterns.OneOrMore oneOrMore, Basics.QName qn, Basics.Id id) {
    Pattern p = oneOrMore.getPattern();
    return group(//
        startTagDeriv(p, qn, id),//
        choice(new Patterns.OneOrMore(p), empty())//
    );
  }

  // startTagDeriv (ConcurOneOrMore p) qn id =
  //   concur (startTagDeriv p qn id)
  //          (choice (ConcurOneOrMore p) anyContent)
  private static Pattern startTagDerivForConcurOneOrMore(Patterns.ConcurOneOrMore concurOneOrMore, Basics.QName qn, Basics.Id id) {
    Pattern p = concurOneOrMore.getPattern();
    return concur(//
        startTagDeriv(p, qn, id),//
        choice(new Patterns.ConcurOneOrMore(p), anyContent())//
    );
  }

  // startTagDeriv (After p1 p2) qn id =
  //   after (startTagDeriv p1 qn id) p2
  private static Pattern startTagDerivForAfter(Patterns.After after, Basics.QName qn, Basics.Id id) {
    Pattern p1 = after.getPattern1();
    Pattern p2 = after.getPattern2();
    return after(//
        startTagDeriv(p1, qn, id),//
        p2//
    );
  }

  // startTagDeriv :: Pattern -> QName -> Id -> Pattern
  private static Pattern startTagDeriv(Pattern pattern, Basics.QName qn, Basics.Id id) {
    String simpleName = pattern.getClass().getSimpleName();
    //    startTagDeriv _ _ _ = NotAllowed
    TriFunction<Pattern, Basics.QName, Basics.Id, ? extends Pattern> function //
        = startTagDerivFunctions.getOrDefault(simpleName, (a, b, c) -> notAllowed());
    return function.apply(pattern, qn, id);
  }

  //  End Tags
  //
  //  End tags are matched by EndRange patterns. An id is used to support self-overlap: when an EndTagEvent matches
  //  an EndRange pattern, the names have to match and so do the ids.
  //
  //  endTagDeriv :: Pattern -> QName -> Id -> Pattern
  private static Pattern endTagDeriv(Pattern pattern, Basics.QName qn, Basics.Id id) {
    //  endTagDeriv (EndRange (QName ns1 ln1) id1)
    //      (QName ns2 ln2) id2 =
    //      if id1 == id2 ||
    //      (id1 == '' && id2 == '' && ns1 == ns2 && ln1 == ln2)
    //  then Empty
    // else NotAllowed
    if (pattern instanceof Patterns.EndRange) {
      Patterns.EndRange endRange = (Patterns.EndRange) pattern;
      Basics.QName qName = endRange.getQName();
      Basics.Uri ns1 = qName.getUri();
      Basics.LocalName ln1 = qName.getLocalName();
      Basics.Id id1 = endRange.getId();
      Basics.Uri ns2 = qn.getUri();
      Basics.LocalName ln2 = qn.getLocalName();
      return (id1.equals(id) || (id1.isEmpty() && id.isEmpty() && ns1.equals(ns2) && ln1.equals(ln2)))
          ? empty()//
          : notAllowed();
    }

    //  endTagDeriv (Choice p1 p2) qn id =
    //  choice (endTagDeriv p1 qn id)
    //         (endTagDeriv p2 qn id)
    if (pattern instanceof Patterns.Choice) {
      Patterns.Choice choice = (Patterns.Choice) pattern;
      Pattern p1 = choice.getPattern1();
      Pattern p2 = choice.getPattern2();
      return choice(//
          endTagDeriv(p1, qn, id),//
          endTagDeriv(p2, qn, id)//
      );
    }

    //  endTagDeriv (Group p1 p2) qn id =
    //  let p = group (endTagDeriv p1 qn id) p2
    //  if nullable p1 then choice p (endTagDeriv p2 qn id)
    //                 else p
    if (pattern instanceof Patterns.Group) {
      Patterns.Group group = (Patterns.Group) pattern;
      Pattern p1 = group.getPattern1();
      Pattern p2 = group.getPattern2();
      Pattern p = group(endTagDeriv(p1, qn, id), p2);
      return p1.isNullable()//
          ? choice(p, endTagDeriv(p2, qn, id))//
          : p;
    }

    //  endTagDeriv (Interleave p1 p2) qn id =
    //  choice (interleave (endTagDeriv p1 qn id) p2)
    //      (interleave p1 (endTagDeriv p2 qn id))
    if (pattern instanceof Patterns.Interleave) {
      Patterns.Interleave interleave = (Patterns.Interleave) pattern;
      Pattern p1 = interleave.getPattern1();
      Pattern p2 = interleave.getPattern2();
      return choice(//
          interleave(endTagDeriv(p1, qn, id), p2),//
          interleave(p1, endTagDeriv(p2, qn, id))//
      );
    }

    // endTagDeriv (Concur p1 p2) qn id =
    //   let d1 = endTagDeriv p1 qn id
    //       d2 = endTagDeriv p2 qn id
    //   in choice (choice (concur d1 p2) (concur p1 d2))
    //       (concur d1 d2)
    if (pattern instanceof Patterns.Concur) {
      Patterns.Concur concur = (Patterns.Concur) pattern;
      Pattern p1 = concur.getPattern1();
      Pattern p2 = concur.getPattern2();
      Pattern d1 = endTagDeriv(p1, qn, id);
      Pattern d2 = endTagDeriv(p2, qn, id);
      return choice(//
          choice(//
              concur(d1, p2),//
              concur(p1, d2)//
          ),//
          concur(d1, d2)//
      );
    }

    // endTagDeriv (Partition p) qn id =
    //   after (endTagDeriv p qn id) Empty
    if (pattern instanceof Patterns.Partition) {
      Patterns.Partition partition = (Patterns.Partition) pattern;
      Pattern p = partition.getPattern();
      return after(//
          endTagDeriv(p, qn, id),//
          empty()//
      );
    }

    // endTagDeriv (OneOrMore p) qn id =
    //   group (endTagDeriv p qn id)
    //         (choice (OneOrMore p) Empty)
    if (pattern instanceof Patterns.OneOrMore) {
      Patterns.OneOrMore oneOrMore = (Patterns.OneOrMore) pattern;
      Pattern p = oneOrMore.getPattern();
      return group(//
          endTagDeriv(p, qn, id),//
          choice(new Patterns.OneOrMore(p), empty())//
      );
    }

    // endTagDeriv (ConcurOneOrMore p) qn id =
    //   concur (endTagDeriv p qn id)
    //          (choice (ConcurOneOrMore p) anyContent)
    if (pattern instanceof Patterns.ConcurOneOrMore) {
      Patterns.ConcurOneOrMore concurOneOrMore = (Patterns.ConcurOneOrMore) pattern;
      Pattern p = concurOneOrMore.getPattern();
      return concur(//
          endTagDeriv(p, qn, id),//
          choice(new Patterns.ConcurOneOrMore(p), anyContent())//
      );
    }

    // endTagDeriv (After p1 p2) qn id =
    //   after (endTagDeriv p1 qn id) p2
    if (pattern instanceof Patterns.After) {
      Patterns.After after = (Patterns.After) pattern;
      Pattern p1 = after.getPattern1();
      Pattern p2 = after.getPattern2();
      return after(//
          endTagDeriv(p1, qn, id),//
          p2//
      );
    }

    //  endTagDeriv _ _ _ = NotAllowed
    return notAllowed();
  }

  private static boolean whitespace(String text) {
    return StringUtils.isBlank(text);
  }

}
