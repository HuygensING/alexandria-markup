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

import java.util.HashMap;
import java.util.Map;

public class Basics {

  public static QName qName(String localName) {
    return qName("", localName);
  }

  public static QName qName(String uri, String localName) {
    return new QName(uri(uri), localName(localName));
  }

  public static LocalName localName(String localName) {
    return new LocalName(localName);
  }

  public static Uri uri(String uri) {
    return new Uri(uri);
  }

  public static Id id(String id) {
    return new Id(id);
  }

  public static Context context() {
    return new Context(uri(""), new HashMap<>());
  }

  static class Uri extends StringWrapper {
    Uri(String uri) {
      super(uri);
    }
  }

  public static class LocalName extends StringWrapper {
    LocalName(String localName) {
      super(localName);
    }
  }

  public static class Id extends StringWrapper {
    Id(String id) {
      super(id);
    }
  }

  private static class Prefix extends StringWrapper {
    public Prefix(String prefix) {
      super(prefix);
    }
  }

  public static class QName {
    private final Uri uri;
    private final LocalName localName;

    public QName(Uri uri, LocalName localName) {
      this.uri = uri;
      this.localName = localName;
    }

    public Uri getUri() {
      return uri;
    }

    public LocalName getLocalName() {
      return localName;
    }

    @Override
    public String toString() {
      String prefix = uri.isEmpty() ? "" : uri + ": ";
      return prefix + localName;
    }
  }

  /*
   A Context represents the context of an XML element.
   It consists of a base URI and a mapping from prefixes to namespace URIs.
   */
  public static class Context {
    private final Uri uri;
    private final Map<Prefix, Uri> nameSpaceURI4Prefix;

    Context(Uri uri, Map<Prefix, Uri> prefixUriMap) {
      this.uri = uri;
      this.nameSpaceURI4Prefix = prefixUriMap;
    }

    public Uri getUri() {
      return uri;
    }

    public Uri getNameSpaceURIForPrefix(Prefix prefix) {
      return nameSpaceURI4Prefix.get(prefix);
    }
  }

  private static class StringWrapper {
    private final String value;
    final int hashCode;

    StringWrapper(String value) {
      this.value = value;
      int baseHashCode = getClass().hashCode();
      int valueHashCode = value.hashCode();
      hashCode = valueHashCode == 0 ? baseHashCode : baseHashCode * valueHashCode;
    }

    public String getValue() {
      return value;
    }

    public boolean isEmpty() {
      return value.isEmpty();
    }

    @Override
    public int hashCode() {
      return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
      return obj.getClass().equals(this.getClass())//
          && value.equals(((StringWrapper) obj).getValue());
    }

    @Override
    public String toString() {
      return value;
    }
  }

}
