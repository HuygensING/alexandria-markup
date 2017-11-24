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

public class Events {
  /*
  data Event = StartTagEvent QName Id
             | EndTagEvent QName Id
             | TextEvent String Context
   */

  public static StartTagEvent startTagEvent(Basics.QName qName) {
    return startTagEvent(qName, "");
  }

  public static StartTagEvent startTagEvent(Basics.QName qName, String id) {
    return new StartTagEvent(qName, Basics.id(id));
  }

  static class StartTagEvent implements Event {
    private final Basics.QName qName;
    private final Basics.Id id;

    public StartTagEvent(Basics.QName qName, Basics.Id id) {
      this.qName = qName;
      this.id = id;
    }

    public Basics.QName getQName() {
      return qName;
    }

    public Basics.Id getId() {
      return id;
    }

    @Override
    public String toString() {
      return "[" + qName.getLocalName().getValue() + "}";
    }

//    @Override
//    public int hashCode() {
//      return qName.hashCode() * id.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//      return (obj instanceof StartTagEvent)
//          && (qName.equals(((StartTagEvent) obj).getQName())
//          && (id.equals(((StartTagEvent) obj).getId()))
//      );
//    }
  }

  public static EndTagEvent endTagEvent(Basics.QName qName) {
    return endTagEvent(qName, "");
  }

  public static EndTagEvent endTagEvent(Basics.QName qName, String id) {
    return new EndTagEvent(qName, Basics.id(id));
  }

  static class EndTagEvent implements Event {
    private final Basics.QName qName;
    private final Basics.Id id;

    public EndTagEvent(Basics.QName qName, Basics.Id id) {
      this.qName = qName;
      this.id = id;
    }

    public Basics.QName getQName() {
      return qName;
    }

    public Basics.Id getId() {
      return id;
    }

    @Override
    public String toString() {
      return "{" + qName.getLocalName().getValue() + "]";
    }

//    @Override
//    public int hashCode() {
//      return qName.hashCode() * id.hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//      return (obj instanceof EndTagEvent)
//          && (qName.equals(((EndTagEvent) obj).getQName())
//          && (id.equals(((EndTagEvent) obj).getId()))
//      );
//    }
  }

  public static TextEvent textEvent(String text) {
    return textEvent(text, Basics.context());
  }

  public static TextEvent textEvent(String text, Basics.Context context) {
    return new TextEvent(text, context);
  }

  static class TextEvent implements Event {
    private final String text;
    private final Basics.Context context;

    public TextEvent(String text, Basics.Context context) {
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

  }

}
