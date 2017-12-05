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
import nl.knaw.huygens.tei.*;
import nl.knaw.huygens.tei.handlers.XmlTextHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DefinitionVisitor extends DelegatingVisitor<XmlContext> {
  private static final Logger LOG = LoggerFactory.getLogger(DefinitionVisitor.class);

  private static String start;
  private static final Map<String, String> definitionMap = new HashMap<>();
  private static final Map<String, Set<String>> requirements = new HashMap<>(); // the definitions referred to in this definition
  private static final Map<String, Set<String>> dependants = new HashMap<>(); // the definitions referring to this definition

  private static String currentDefinition;


  public DefinitionVisitor() {
    super(new XmlContext());
    setTextHandler(new XmlTextHandler());
    setDefaultElementHandler(new DefaultElementHandler());
    addElementHandler(new DefineHandler(), "define");
    addElementHandler(new GrammarHandler(), "grammar");
    addElementHandler(new RefHandler(), "ref");
    addElementHandler(new StartHandler(), "start");
  }

  public String getExpandedSchemaTree() {
    String schemaTree = start;
    while (schemaTree.contains("<ref name=")) {
      for (String name : definitionMap.keySet()) {
        schemaTree = schemaTree.replace("<ref name=\"" + name + "\"/>", definitionMap.get(name));
      }
    }
    return schemaTree;
  }

  public static class DefaultElementHandler implements ElementHandler<XmlContext> {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      if (element.hasChildren()) {
        context.addOpenTag(element);
      } else {
        context.addEmptyElementTag(element);
      }
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      if (element.hasChildren()) {
        context.addCloseTag(element);
      }
      return Traversal.NEXT;
    }
  }

  public static class GrammarHandler extends DefaultElementHandler {
  }

  public static class StartHandler extends DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      currentDefinition = "_start";
      requirements.putIfAbsent(currentDefinition, new HashSet<>());
      context.openLayer();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      start = context.closeLayer().trim();
      return Traversal.NEXT;
    }
  }

  public static class RefHandler extends DefaultElementHandler {
    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String ref = element.getAttribute("name");
      requirements.get(currentDefinition).add(ref);
      dependants.putIfAbsent(ref, new HashSet<>());
      dependants.get(ref).add(currentDefinition);
      return super.leaveElement(element, context);
    }
  }

  public static class DefineHandler extends DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      currentDefinition = element.getAttribute("name");
      requirements.putIfAbsent(currentDefinition, new HashSet<>());
      dependants.putIfAbsent(currentDefinition, new HashSet<>());
      context.openLayer();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String value = context.closeLayer().replaceAll(" *\n *","");
      definitionMap.put(currentDefinition, value);
      return Traversal.NEXT;
    }
  }


}
