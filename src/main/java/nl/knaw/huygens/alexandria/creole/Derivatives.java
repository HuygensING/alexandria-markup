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

import java.util.List;

import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.Utilities.nullable;

public class Derivatives {
  /*
    Derivatives

    Finally, we can look at the calculation of derivatives.
    A document is a sequence of events.
    The derivative for a sequence of events against a pattern is the derivative of the remaining events
    against the derivative of the first event.
  */
  // eventsDeriv :: Pattern -> [Event] -> Pattern
  public static Pattern eventsDeriv(Pattern pattern, List<Event> events) {
    // eventsDeriv p [] = p
    if (events.isEmpty()) {
      return pattern;
    }

    //  eventsDeriv p (h:t) = eventsDeriv (eventDeriv p h) t
    Event head = events.remove(0);
    Pattern headDeriv = eventsDeriv(pattern, head);
    return eventsDeriv(headDeriv, events);
  }

  /*
    The derivative for an event depends on the kind of event, and we use different functions for each kind.
     Whitespace-only text nodes can be ignored if the pattern doesn't allow text.
    */
  //  eventDeriv :: Pattern -> Event -> Pattern
  public static Pattern eventsDeriv(Pattern p, Event event) {
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
    throw new RuntimeException("unexpected event: " + event);
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
      return (nullable(p1))//
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
          choice(Patterns.oneOrMore(p), Patterns.empty())//
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
          choice(Patterns.concurOneOrMore(p), Patterns.text())//
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
          Patterns.empty()//
      );
    }

    //No other patterns can match a text event; the default is specified as
    //
    //textDeriv _ _ _ = NotAllowed
    return Patterns.notAllowed();
  }

  private static Pattern startTagDeriv(Pattern p, Basics.QName qn, Basics.Id id) {
    throw new RuntimeException("unexpected pattern: " + p);

  }

  private static Pattern endTagDeriv(Pattern p, Basics.QName qn, Basics.Id id) {
    throw new RuntimeException("unexpected pattern: " + p);
  }

  private static boolean whitespace(String text) {
    return StringUtils.isBlank(text);
  }
}
