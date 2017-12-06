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

import static nl.knaw.huygens.alexandria.creole.Basics.qName;

public class Events {
  /*
  data Event = StartTagEvent QName Id
             | EndTagEvent QName Id
             | TextEvent String Context
             | StartAnnotationEvent QName
             | EndAnnotationEvent QName
             | StartAtomEvent QName
             | EndAtomEvent QName
   */

  public static StartTagEvent startTagEvent(Basics.QName qName) {
    return startTagEvent(qName, "");
  }

  public static StartTagEvent startTagEvent(Basics.QName qName, String id) {
    return new StartTagEvent(qName, Basics.id(id));
  }

  public static Event startTagOpenEvent(Basics.QName qName, String id) {
    return new StartTagOpenEvent(qName, Basics.id(id));
  }

  public static Event startTagOpenEvent(Basics.QName qName) {
    return new StartTagOpenEvent(qName, Basics.id(""));
  }

  public static Event startTagOpenEvent(String name) {
    return new StartTagOpenEvent(qName(name), Basics.id(""));
  }

  public static Event startTagCloseEvent(Basics.QName qName) {
    return new StartTagCloseEvent(qName, Basics.id(""));
  }

  public static Event startTagCloseEvent(Basics.QName qName, String id) {
    return new StartTagCloseEvent(qName, Basics.id(id));
  }

  public static Event startTagCloseEvent(String name) {
    return new StartTagCloseEvent(qName(name), Basics.id(""));
  }

  public static Event endTagOpenEvent(Basics.QName qName, String id) {
    return new EndTagOpenEvent(qName, Basics.id(id));
  }

  public static Event endTagOpenEvent(Basics.QName qName) {
    return new EndTagOpenEvent(qName, Basics.id(""));
  }

  public static Event endTagOpenEvent(String name) {
    return new EndTagOpenEvent(qName(name), Basics.id(""));
  }

  public static Event endTagCloseEvent(Basics.QName qName, String id) {
    return new EndTagCloseEvent(qName, Basics.id(id));
  }

  public static Event endTagCloseEvent(Basics.QName qName) {
    return new EndTagCloseEvent(qName, Basics.id(""));
  }

  public static Event endTagCloseEvent(String name) {
    return new EndTagCloseEvent(qName(name), Basics.id(""));
  }

  static abstract class TagEvent implements Event {
    private final Basics.QName qName;
    private final Basics.Id id;

    TagEvent(Basics.QName qName, Basics.Id id) {
      this.qName = qName;
      this.id = id;
    }

    public Basics.QName getQName() {
      return qName;
    }

    public Basics.Id getId() {
      return id;
    }
  }

  @Deprecated
  static class StartTagEvent extends TagEvent {
    StartTagEvent(Basics.QName qName, Basics.Id id) {
      super(qName, id);
    }

    @Override
    public String toString() {
      return "[" + getQName().getLocalName().getValue() + "}";
    }
  }

  public static class StartTagOpenEvent extends TagEvent {
    public StartTagOpenEvent(Basics.QName qName, Basics.Id id) {
      super(qName, id);
    }

    @Override
    public String toString() {
      return "[" + getQName().getLocalName().getValue();
    }
  }

  public static class StartTagCloseEvent extends TagEvent {
    public StartTagCloseEvent(Basics.QName qName, Basics.Id id) {
      super(qName, id);
    }

    @Override
    public String toString() {
      return getQName().getLocalName().getValue() + "}";
    }
  }

  public static EndTagEvent endTagEvent(Basics.QName qName) {
    return endTagEvent(qName, "");
  }

  public static EndTagEvent endTagEvent(Basics.QName qName, String id) {
    return new EndTagEvent(qName, Basics.id(id));
  }

  @Deprecated
  static class EndTagEvent extends TagEvent {
    public EndTagEvent(Basics.QName qName, Basics.Id id) {
      super(qName, id);
    }

    @Override
    public String toString() {
      return "{" + getQName().getLocalName().getValue() + "]";
    }
  }

  public static class EndTagOpenEvent extends TagEvent {
    public EndTagOpenEvent(Basics.QName qName, Basics.Id id) {
      super(qName, id);
    }

    @Override
    public String toString() {
      return "{" + getQName().getLocalName().getValue();
    }
  }

  public static class EndTagCloseEvent extends TagEvent {
    public EndTagCloseEvent(Basics.QName qName, Basics.Id id) {
      super(qName, id);
    }

    @Override
    public String toString() {
      return getQName().getLocalName().getValue() + "]";
    }
  }

  // TextEvent

  public static TextEvent textEvent(String text) {
    return textEvent(text, Basics.context());
  }

  public static TextEvent textEvent(String text, Basics.Context context) {
    return new TextEvent(text, context);
  }

  static class TextEvent implements Event {
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

  /* Annotation events */

  public static StartAnnotationOpenEvent startAnnotationOpenEvent(String qName) {
    return startAnnotationOpenEvent(qName(qName));
  }

  public static StartAnnotationOpenEvent startAnnotationOpenEvent(Basics.QName qName) {
    return new StartAnnotationOpenEvent(qName);
  }

  public static StartAnnotationCloseEvent startAnnotationCloseEvent(String qName) {
    return startAnnotationCloseEvent(qName(qName));
  }

  public static StartAnnotationCloseEvent startAnnotationCloseEvent(Basics.QName qName) {
    return new StartAnnotationCloseEvent(qName);
  }

  public static EndAnnotationOpenEvent endAnnotationOpenEvent(String qName) {
    return endAnnotationOpenEvent(qName(qName));
  }

  public static EndAnnotationOpenEvent endAnnotationOpenEvent(Basics.QName qName) {
    return new EndAnnotationOpenEvent(qName);
  }

  public static EndAnnotationCloseEvent endAnnotationCloseEvent(String qName) {
    return endAnnotationCloseEvent(qName(qName));
  }

  public static EndAnnotationCloseEvent endAnnotationCloseEvent(Basics.QName qName) {
    return new EndAnnotationCloseEvent(qName);
  }

  static abstract class AnnotationEvent implements Event {
    private final Basics.QName qName;

    AnnotationEvent(Basics.QName qName) {
      this.qName = qName;
    }

    Basics.QName getQName() {
      return qName;
    }
  }

  static class StartAnnotationOpenEvent extends AnnotationEvent {
    StartAnnotationOpenEvent(Basics.QName qName) {
      super(qName);
    }

    @Override
    public String toString() {
      return "(" + getQName().getLocalName().getValue();
    }
  }

  static class StartAnnotationCloseEvent extends AnnotationEvent {
    StartAnnotationCloseEvent(Basics.QName qName) {
      super(qName);
    }

    @Override
    public String toString() {
      return getQName().getLocalName().getValue() + ">";
    }
  }

  static class EndAnnotationOpenEvent extends AnnotationEvent {
    EndAnnotationOpenEvent(Basics.QName qName) {
      super(qName);
    }

    @Override
    public String toString() {
      return "<" + getQName().getLocalName().getValue();
    }
  }

  static class EndAnnotationCloseEvent extends AnnotationEvent {
    EndAnnotationCloseEvent(Basics.QName qName) {
      super(qName);
    }

    @Override
    public String toString() {
      return getQName().getLocalName().getValue() + ")";
    }
  }
}
