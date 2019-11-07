package nl.knaw.huygens.alexandria.creole.patterns;

/*-
 * #%L
 * alexandria-markup-core
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
import static nl.knaw.huygens.alexandria.creole.Constructors.empty;
import static nl.knaw.huygens.alexandria.creole.Constructors.notAllowed;
import nl.knaw.huygens.alexandria.creole.Pattern;

public class EndRange extends AbstractPattern {
  private final Basics.QName qName;
  private final Basics.Id id;

  public EndRange(Basics.QName qName, Basics.Id id) {
    this.qName = qName;
    this.id = id;
    setHashcode(getClass().hashCode() + qName.hashCode() * id.hashCode());
  }

  public Basics.QName getQName() {
    return qName;
  }

  public Basics.Id getId() {
    return id;
  }

  @Override
  void init() {
    nullable = false;
    allowsText = false;
    allowsAnnotations = false;
    onlyAnnotations = false;
  }

  @Override
  public Pattern endTagDeriv(Basics.QName qn, Basics.Id id2) {
    // endTagDeriv (EndRange (QName ns1 ln1) id1)
    //             (QName ns2 ln2) id2 =
    //   if id1 == id2 ||
    //      (id1 == '' && id2 == '' && ns1 == ns2 && ln1 == ln2)
    //   then Empty
    //   else NotAllowed
    Basics.Uri ns1 = qn.getUri();
    Basics.LocalName ln1 = qn.getLocalName();
    Basics.Uri ns2 = qn.getUri();
    Basics.LocalName ln2 = qn.getLocalName();
    return (id.equals(id2) || (id.isEmpty() && id2.isEmpty() && ns1.equals(ns2) && ln1.equals(ln2)))
        ? empty()//
        : notAllowed();
  }

  @Override
  public String toString() {
    String postfix = id.isEmpty() ? "" : "~" + id;
    return "<" + qName + postfix + "]";
  }
}
