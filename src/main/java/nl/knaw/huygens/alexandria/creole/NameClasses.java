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

public class NameClasses {

  public static final AnyName ANY_NAME = new AnyName();

  /*
  A NameClass represents a name class.

  data NameClass = AnyName
                   | AnyNameExcept NameClass
                   | Name Uri LocalName
                   | NsName Uri
                   | NsNameExcept Uri NameClass
                   | NameClassChoice NameClass NameClass
   */

  public static AnyName anyName() {
    return ANY_NAME;
  }

  static class AnyName extends AbstractNameClass {
  }

  public static AnyNameExcept anyNameExcept(NameClass nameClassToExcept) {
    return new AnyNameExcept(nameClassToExcept);
  }

  static class AnyNameExcept extends AbstractNameClass {
    private final NameClass nameClassToExcept;

    public AnyNameExcept(NameClass nameClassToExcept) {
      this.nameClassToExcept = nameClassToExcept;
    }

    public NameClass getNameClassToExcept() {
      return nameClassToExcept;
    }
  }

  public static Name name(String localName) {
    return name("", localName);
  }

  public static Name name(String uri, String localName) {
    return name(Basics.uri(uri), Basics.localName(localName));
  }

  public static Name name(Basics.Uri uri, Basics.LocalName localName) {
    return new Name(uri, localName);
  }

  static class Name extends AbstractNameClass {
    private final Basics.Uri uri;
    private final Basics.LocalName localName;

    public Name(Basics.Uri uri, Basics.LocalName localName) {
      this.uri = uri;
      this.localName = localName;
      setHashCode(getClass().hashCode() * uri.hashCode() * localName.hashCode());
    }

    public Basics.Uri getUri() {
      return uri;
    }

    public Basics.LocalName getLocalName() {
      return localName;
    }
  }

  public static NsNameExcept nsNameExcept(String uri, NameClass nameClass) {
    return nsNameExcept(Basics.uri(uri), nameClass);
  }

  public static NsNameExcept nsNameExcept(Basics.Uri uri, NameClass nameClass) {
    return new NsNameExcept(uri, nameClass);
  }

  static class NsNameExcept extends AbstractNameClass {
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

  public static NsName nsName(String uri) {
    return nsName(Basics.uri(uri));
  }

  public static NsName nsName(Basics.Uri uri) {
    return new NsName(uri);
  }

  static class NsName extends AbstractNameClass {
    private final Basics.Uri uri;

    public NsName(Basics.Uri uri) {
      this.uri = uri;
    }

    public String getValue() {
      return uri.getValue();
    }
  }

  public static NameClassChoice nameClassChoice(NameClass nameClass1, NameClass nameClass2) {
    return new NameClassChoice(nameClass1, nameClass2);
  }

  static class NameClassChoice extends AbstractNameClass {
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

  static class AbstractNameClass implements NameClass {
    int hashCode;

    AbstractNameClass() {
      hashCode = getClass().hashCode();
    }

    public void setHashCode(int hashCode) {
      this.hashCode = hashCode;
    }

    @Override
    public int hashCode() {
      return hashCode;
    }
  }

}
