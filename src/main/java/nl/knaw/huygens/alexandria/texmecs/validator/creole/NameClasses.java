package nl.knaw.huygens.alexandria.texmecs.validator.creole;

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

public class NameClasses {

  /*
  A NameClass represents a name class.

  data NameClass = AnyName
                   | AnyNameExcept NameClass
                   | Name Uri LocalName
                   | NsName Uri
                   | NsNameExcept Uri NameClass
                   | NameClassChoice NameClass NameClass
   */

  public class AnyName implements NameClass {
  }

  public class AnyNameExcept implements NameClass {
    private final NameClass nameClassToExcept;

    public AnyNameExcept(NameClass nameClassToExcept) {
      this.nameClassToExcept = nameClassToExcept;
    }

    public NameClass getNameClassToExcept() {
      return nameClassToExcept;
    }
  }

  public class Name implements NameClass {
    private final Basics.Uri uri;
    private final Basics.LocalName localName;

    public Name(Basics.Uri uri, Basics.LocalName localName) {
      this.uri = uri;
      this.localName = localName;
    }

    public Basics.Uri getUri() {
      return uri;
    }

    public Basics.LocalName getLocalName() {
      return localName;
    }
  }


  public class NsNameExcept implements NameClass {
    private final Basics.Uri uri;
    private final NameClass nameClass;

    public NsNameExcept(Basics.Uri uri, NameClass nameClass) {
      this.uri = uri;
      this.nameClass = nameClass;
    }

    public Basics.Uri getUri() {
      return uri;
    }

    public NameClass getNameClass() {
      return nameClass;
    }
  }

  public class NsName implements NameClass {
    private final String uri;

    public NsName(String uri) {
      this.uri = uri;
    }

    public String getValue() {
      return uri;
    }
  }

  public class NameClassChoice implements NameClass {
    private final NameClass nameClass1;
    private final NameClass nameClass2;

    public NameClassChoice(NameClass nameClass1, NameClass nameClass2) {
      this.nameClass1 = nameClass1;
      this.nameClass2 = nameClass2;
    }

    public NameClass getNameClass1() {
      return nameClass1;
    }

    public NameClass getNameClass2() {
      return nameClass2;
    }
  }

}
