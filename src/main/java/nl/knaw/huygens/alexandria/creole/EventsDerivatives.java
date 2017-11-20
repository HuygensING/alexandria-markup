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

public class EventsDerivatives {
  /*
    Derivatives

    Finally, we can look at the calculation of derivatives.
    A document is a sequence of events.
    The derivative for a sequence of events against a pattern is the derivative of the remaining events against the derivative of the first event.


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

    if (event instanceof Events.TextEvent){
      Events.TextEvent textEvent = (Events.TextEvent) event;
      String s = textEvent.getText();
      Basics.Context cx = textEvent.getContext();
      if (whitespace(s) && !Utilities.allowsText(p)){
        return p;
      }else{
        return textDeriv(cx,p, s);
      }
    }
    //    eventDeriv p (StartTagEvent qn id) = startTagDeriv p qn id
    //    eventDeriv p (EndTagEvent qn id) = endTagDeriv p qn id

    return null;
  }

  private static Pattern textDeriv(Basics.Context context, Pattern pattern, String text) {
    return null;
  }

  private static boolean whitespace(String text) {
    return StringUtils.isBlank(text);
  }
}
