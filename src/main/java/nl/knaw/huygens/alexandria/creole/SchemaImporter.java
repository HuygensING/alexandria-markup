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
import static nl.knaw.huygens.alexandria.creole.Constructors.*;
import static nl.knaw.huygens.alexandria.creole.NameClasses.name;
import nl.knaw.huygens.tei.Document;
import nl.knaw.huygens.tei.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

  static final Map<String, Function<Element, Pattern>> elementHandlers = new HashMap<>();

  static {
    elementHandlers.put("choice", SchemaImporter::handleChoice);
    elementHandlers.put("concur", SchemaImporter::handleConcur);
    elementHandlers.put("concurOneOrMore", SchemaImporter::handleConcurOneOrMore);
    elementHandlers.put("element", SchemaImporter::handleElement);
    elementHandlers.put("empty", SchemaImporter::handleEmpty);
    elementHandlers.put("group", SchemaImporter::handleGroup);
    elementHandlers.put("interleave", SchemaImporter::handleInterleave);
    elementHandlers.put("mixed", SchemaImporter::handleMixed);
    elementHandlers.put("oneOrMore", SchemaImporter::handleOneOrMore);
    elementHandlers.put("partition", SchemaImporter::handlePartition);
    elementHandlers.put("range", SchemaImporter::handleRange);
    elementHandlers.put("text", SchemaImporter::handleText);
    elementHandlers.put("zeroOrMore", SchemaImporter::handleZeroOrMore);
  }

  private static Pattern toPattern(Element element) {
    String elementName = element.getName();
    Function<Element, Pattern> handler = elementHandlers.get(elementName);
    if (handler == null) {
      throw new RuntimeException("no elementHandler defined for Element " + elementName);
    }
    return handler.apply(element);
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
    LOG.debug("concur children = {}", children);
    List<Element> attributes = removeAttributes(children);

    Pattern pattern1 = toPattern(children.remove(0));
    Pattern pattern2 = groupWhenNeeded(children);
    return concur(pattern1, pattern2);
  }

  private static Pattern handleConcurOneOrMore(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return concurOneOrMore(pattern);
  }

  private static Pattern handleElement(Element element) {
    String localName = element.getAttribute("name");
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
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
    List<Element> attributes = removeAttributes(children);

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

  private static Pattern handlePartition(Element element) {
    List<Element> children = getChildElements(element);
    Preconditions.checkState(children.size() == 1);
    Pattern pattern = toPattern(children.get(0));
    return partition(pattern);
  }

  private static Pattern handleRange(Element element) {
    String localName = element.getAttribute("name");
    List<Element> children = getChildElements(element);
    List<Element> attributes = removeAttributes(children);
    Pattern childPattern = (children.size() == 1)//
        ? toPattern(children.get(0))//
        : toGroup(children);
    return range(name(localName), childPattern);
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

}
