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

  public class StartTagEvent implements Event {
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
  }

  public class EndTagEvent implements Event {
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
  }

  public class TextEvent implements Event {
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
  }

}
