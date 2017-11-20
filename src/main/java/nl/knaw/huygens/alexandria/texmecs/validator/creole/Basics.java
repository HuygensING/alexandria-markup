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

import java.util.Map;

public class Basics {
  public class Uri extends StringWrapper {
    public Uri(String uri) {
      super(uri);
    }
  }

  public class LocalName extends StringWrapper {
    public LocalName(String localName) {
      super(localName);
    }
  }

  public class Id extends StringWrapper {
    public Id(String id) {
      super(id);
    }
  }

  public class Prefix extends StringWrapper {
    public Prefix(String prefix) {
      super(prefix);
    }
  }

  public class QName {
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
  }

  /*
   A Context represents the context of an XML element.
   It consists of a base URI and a mapping from prefixes to namespace URIs.
   */
  public class Context {
    private final Uri uri;
    private final Map<Prefix, Uri> nameSpaceURI4Prefix;

    public Context(Uri uri, Map<Prefix,Uri> prefixUriMap){
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

    StringWrapper(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public int hashCode() {
      return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj.getClass().equals(this.getClass())//
          && value.equals(((StringWrapper) obj).getValue());
    }
  }

}
