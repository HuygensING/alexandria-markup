package nl.knaw.huygens.alexandria.creole;

    /*-
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

import com.google.common.base.Preconditions;
import nl.knaw.huygens.tei.Document;
import nl.knaw.huygens.tei.Element;
import nl.knaw.huygens.tei.Node;
import nl.knaw.huygens.tei.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.NameClasses.*;

public class SchemaImporter {
  private static final Logger LOG = LoggerFactory.getLogger(SchemaImporter.class);

  public static Pattern fromXML(String xml) {
    Document doc = Document.createFromXml(xml, true);
    DefinitionVisitor definitionVisitor = new DefinitionVisitor();
    doc.accept(definitionVisitor);
    String schemaTree = definitionVisitor.getExpandedSchemaTree();

    Document doc2 = Document.createFromXml(schemaTree, true);
    Element root = doc2.getRoot();
    return toPattern(root);
  }

  public static Pattern fromCompactGrammar(String xml) {
    return Patterns.EMPTY;
  }

  private static final Map<String, Function<Element, Pattern>> elementToPattern = new HashMap<>();

  static {
    elementToPattern.put("atom", SchemaImporter::handleAtom);
    elementToPattern.put("annotation", SchemaImporter::handleAnnotation);
    elementToPattern.put("attribute", SchemaImporter::handleAttribute);
    elementToPattern.put("choice", SchemaImporter::handleChoice);
    elementToPattern.put("concur", SchemaImporter::handleConcur);
    elementToPattern.put("concurOneOrMore", SchemaImporter::handleConcurOneOrMore);
    elementToPattern.put("concurZeroOrMore", SchemaImporter::handleConcurZeroOrMore);
    elementToPattern.put("element", SchemaImporter::handleElement);
    elementToPattern.put("empty", SchemaImporter::handleEmpty);
    elementToPattern.put("group", SchemaImporter::handleGroup);
    elementToPattern.put("interleave", SchemaImporter::handleInterleave);
    elementToPattern.put("mixed", SchemaImporter::handleMixed);
    elementToPattern.put("oneOrMore", SchemaImporter::handleOneOrMore);
    elementToPattern.put("optional", SchemaImporter::handleOptional);
    elementToPattern.put("partition", SchemaImporter::handlePartition);
    elementToPattern.put("range", SchemaImporter::handleRange);
    elementToPattern.put("text", SchemaImporter::handleText);
    elementToPattern.put("zeroOrMore", SchemaImporter::handleZeroOrMore);
  }

  private static Pattern toPattern(Element element) {
    String elementName = element.getName();
    Function<Element, Pattern> handler = elementToPattern.get(elementName);
    if (handler == null) {
      throw new RuntimeException("no elementHandler defined for Element " + elementName);
    }
    return handler.apply(element);
  }

  private static Pattern handleAtom(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);
    String name = element.getAttribute("name");
    return atom(name);
  }

  private static Pattern handleAnnotation(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);
    if (element.hasAttribute("name")) {
      String name = element.getAttribute("name");
      Preconditions.checkState(children.size() == 1);
      Pattern pattern = toPattern(children.get(0));
      return annotation(name, pattern);

    } else {
      Preconditions.checkState(children.size() == 2);
      NameClass nameClass = toNameClass(children.get(0));
      Pattern pattern = toPattern(children.get(1));
      return annotation(nameClass, pattern);
    }
  }

  private static Pattern handleAttribute(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);
    String name = element.getAttribute("name");
    return attribute(name);
  }

  private static Pattern handleChoice(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);

    Pattern pattern1 = toPattern(children.remove(0));
    Pattern pattern2 = groupWhenNeeded(children);
    return choice(pattern1, pattern2);
  }

  private static Pattern handleConcur(Element element) {
    List<Element> children = getChildElements(element);
//    List<Element> attributes = removeAttributes(children);

    Pattern pattern1 = toPattern(children.remove(0));
    Pattern pattern2 = groupWhenNeeded(children);
    return concur(pattern1, pattern2);
  }

  private static Pattern handleConcurOneOrMore(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return concurOneOrMore(pattern);
  }

  private static Pattern handleConcurZeroOrMore(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return concurZeroOrMore(pattern);
  }

  private static Pattern handleElement(Element element) {
    String localName = element.getAttribute("name");
    List<Element> children = getChildElements(element);
    Pattern pattern = children.size() == 1
        ? toPattern(children.get(0))
        : toGroup(children);
    return element(localName, pattern);
  }

  private static Pattern handleEmpty(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 0);
    return empty();
  }

  private static Pattern handleGroup(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);

    Pattern pattern1 = toPattern(children.remove(0));
    Pattern pattern2 = groupWhenNeeded(children);
    return group(pattern1, pattern2);
  }

  private static Pattern handleInterleave(Element element) {
    List<Element> children = getChildElements(element);
//    List<Element> attributes = removeAttributes(children);

    Pattern pattern1 = toPattern(children.remove(0));
    Pattern pattern2 = groupWhenNeeded(children);
    return interleave(pattern1, pattern2);
  }

  private static Pattern handleMixed(Element element) {
    List<Element> children = getChildElements(element);
    Pattern pattern = (children.size() == 1)//
        ? toPattern(children.get(0))//
        : toGroup(children);
    return mixed(pattern);
  }

  private static Pattern handleOneOrMore(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return oneOrMore(pattern);
  }

  private static Pattern handleOptional(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return optional(pattern);
  }

  private static Pattern handlePartition(Element element) {
    List<Element> children = getChildElements(element);
    Pattern pattern = children.size() == 1
        ? toPattern(children.get(0))
        : toGroup(children);
    return partition(pattern);
  }

  private static Pattern handleRange(Element element) {
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);
    NameClass nameClass = null;
    if (element.hasAttribute("name")) {
      nameClass = name(element.getAttribute("name"));

    } else {
      nameClass = toNameClass(children.remove(0));
    }
    Pattern childPattern = (children.size() == 1)//
        ? toPattern(children.get(0))//
        : toGroup(children);
    return range(nameClass, childPattern);
  }

  private static Pattern handleZeroOrMore(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return zeroOrMore(pattern);
  }

  private static Pattern handleText(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 0);
    return text();
  }

  private static List<Element> getChildElements(Element element) {
    return element.getNodes()//
        .stream()//
        .filter(Element.class::isInstance)//
        .map(Element.class::cast)//
        .collect(Collectors.toList());
  }

  private static List<Element> removeAttributes(List<Element> children) {
    List<Element> attributes = children.stream().filter(e -> "attribute".equals(e.getName())).collect(Collectors.toList());
    children.removeAll(attributes);
    return attributes;
  }

  private static Pattern toGroup(List<Element> children) {
    Pattern pattern1 = toPattern(children.remove(0));
    Pattern pattern2 = groupWhenNeeded(children);
    return group(pattern1, pattern2);
  }

  private static Pattern groupWhenNeeded(List<Element> children) {
    Pattern pattern2;
    if (children.size() == 1) {
      pattern2 = toPattern(children.remove(0));
    } else {
      pattern2 = toGroup(children);
    }
    return pattern2;
  }

  private static final Map<String, Function<Element, NameClass>> elementToNameClass = new HashMap<>();

  static {
    elementToNameClass.put("anyName", SchemaImporter::handleAnyName);
    elementToNameClass.put("name", SchemaImporter::handleName);
    elementToNameClass.put("nsName", SchemaImporter::handleNsName);
    elementToNameClass.put("choice", SchemaImporter::handleNameClassChoice);
  }

  private static NameClass toNameClass(Element element) {
    String elementName = element.getName();
    Function<Element, NameClass> handler = elementToNameClass.get(elementName);
    if (handler == null) {
      throw new RuntimeException("no elementHandler defined for Element " + elementName);
    }
    return handler.apply(element);
  }

  private static NameClass handleAnyName(Element element) {
    List<Element> children = getChildElements(element);
    if (hasExcept(children)) {
      Element except = children.get(0);
      List<Element> exceptChildren = getChildElements(except);
      Preconditions.checkState(exceptChildren.size() == 1);
      NameClass nc = toNameClass(exceptChildren.get(0));
      return anyNameExcept(nc);
    } else {
      return anyName();
    }
  }

  private static boolean hasExcept(List<Element> children) {
    return children.size() == 1 && children.get(0).getName().equals("except");
  }

  private static NameClass handleName(Element element) {
    List<Node> nodes = element.getNodes();
    Preconditions.checkState(nodes.size() == 1);
    Text nameNode = (Text) nodes.get(0);
    return name(nameNode.getText());
  }

  private static NameClass handleNsName(Element element) {
    String uri = element.getAttribute("uri");
    List<Element> children = getChildElements(element);
    if (hasExcept(children)) {
      Element except = children.get(0);
      List<Element> exceptChildren = getChildElements(except);
      Preconditions.checkState(exceptChildren.size() == 1);
      NameClass nc = toNameClass(exceptChildren.get(0));
      return nsNameExcept(uri, nc);
    } else {
      return nsName(uri);
    }
  }

  private static NameClass handleNameClassChoice(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 2);
    NameClass nc1 = toNameClass(children.remove(0));
    NameClass nc2 = toNameClass(children.remove(0));
    return nameClassChoice(nc1, nc2);
  }


}
