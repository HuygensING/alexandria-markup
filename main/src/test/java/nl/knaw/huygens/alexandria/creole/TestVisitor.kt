package nl.knaw.huygens.alexandria.creole;

/*-
* #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * #L%
*/

import nl.knaw.huygens.tei.*;
import nl.knaw.huygens.tei.handlers.XmlTextHandler;

import java.util.ArrayList;
import java.util.List;

class TestVisitor extends DelegatingVisitor<XmlContext> {
  private static final List<LMNLTest> tests = new ArrayList<>();

  private static LMNLTest currentTest;

  public TestVisitor() {
    super(new XmlContext());
    setTextHandler(new XmlTextHandler());
    setDefaultElementHandler(new DefaultElementHandler());
    addElementHandler(new TestHandler(), "test:test");
    addElementHandler(new TestTitleHandler(), "test:title");
    addElementHandler(new TestParamHandler(), "test:param");
    addElementHandler(new TestExpectHandler(), "test:expect");
  }

  public List<LMNLTest> getTests() {
    return tests;
  }

  static class DefaultElementHandler implements ElementHandler<XmlContext> {
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

  static class TestHandler extends DefinitionVisitor.DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      currentTest = new LMNLTest();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      tests.add(currentTest);
      currentTest = null;
      return Traversal.NEXT;
    }
  }

  static class TestParamHandler extends DefinitionVisitor.DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      context.openLayer();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String content = context.closeLayer().trim()//
          .replaceAll("^ {6}", "")//
          .replaceAll("\n {6}", "\n")//
          .replaceAll("\n\n", "\n");
      String name = element.getAttribute("name");
      switch (name) {
        case "lmnl":
          currentTest.setLMNL(content);
          break;
        case "schema":
          currentTest.setCreole("<!-- " + currentTest.getTitle() + " -->\n<start>\n" + content + "\n</start>");
          break;
        default:
          throw new RuntimeException("unexpected value for name: " + name);
      }
      return Traversal.NEXT;
    }
  }

  static class TestTitleHandler extends DefinitionVisitor.DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      context.openLayer();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String content = context.closeLayer().trim();
      if (currentTest != null) {
        currentTest.setTitle(content);
      }
      return Traversal.NEXT;
    }
  }

  static class TestExpectHandler extends DefinitionVisitor.DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      String select = element.getAttribute("select");
      switch (select) {
        case "true()":
          currentTest.setValid(true);
          break;
        case "false()":
          currentTest.setValid(false);
          break;
        default:
          throw new RuntimeException("unexpected value for select: " + select);
      }
      return Traversal.NEXT;
    }
  }

}
