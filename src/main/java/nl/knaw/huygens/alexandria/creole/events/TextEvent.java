package nl.knaw.huygens.alexandria.creole.events;

/*-
 * #%L
 * alexandria-markup
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
import nl.knaw.huygens.alexandria.creole.Basics;
import nl.knaw.huygens.alexandria.creole.Event;
import nl.knaw.huygens.alexandria.creole.Pattern;
import org.apache.commons.lang3.StringUtils;

public class TextEvent implements Event {
  private final String text;
  private final Basics.Context context;

  TextEvent(String text, Basics.Context context) {
    this.text = text;
    this.context = context;
  }

  public String getText() {
    return text;
  }

  public Basics.Context getContext() {
    return context;
  }

  @Override
  public Pattern eventDeriv(Pattern p) {
    //  eventDeriv p (TextEvent s cx) =
    //    if (whitespace s && not allowsText p)
    //    then p
    //    else (textDeriv cx p s)
    return whitespace(text) && !p.allowsText()//
        ? p //
        : p.textDeriv(context, text);
  }

  @Override
  public String toString() {
    return text;
  }
  //    @Override
//    public int hashCode() {
//      return text.hashCode() * context.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//      return (obj instanceof TextEvent)
//          && (text.equals(((TextEvent) obj).getText())
//          && (context.equals(((TextEvent) obj).getContext()))
//      );
//    }

  private static boolean whitespace(String text) {
    return StringUtils.isBlank(text);
  }
}
