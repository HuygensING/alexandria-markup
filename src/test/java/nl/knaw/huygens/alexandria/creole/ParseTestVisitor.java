package nl.knaw.huygens.alexandria.creole;

    /*-
     * #%L
 * alexandria-markup
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

import nl.knaw.huygens.tei.*;
import nl.knaw.huygens.tei.handlers.XmlTextHandler;

import java.util.ArrayList;
import java.util.List;

public class ParseTestVisitor extends DelegatingVisitor<XmlContext> {
  private static final List<LMNLTest> tests = new ArrayList<>();

  protected static LMNLTest currentTest;
  protected static StringBuilder testCodeBuilder = new StringBuilder();
  static int testCount = 0;

  public ParseTestVisitor() {
    super(new XmlContext());
    setTextHandler(new XmlTextHandler());
    setDefaultElementHandler(new DefaultElementHandler());
    addElementHandler(new TestHandler(), "test:test");
    addElementHandler(new TestTitleHandler(), "test:title");
    addElementHandler(new TestParamHandler(), "test:param");
    addElementHandler(new TestExpectHandler(), "test:expect");
    addElementHandler(new ExpectedEventHandler("atomOpen"), "ev:atom-open");
    addElementHandler(new ExpectedEventHandler("atomClose"), "ev:atom-close");
    addElementHandler(new ExpectedEventHandler("startAnnotationOpen"), "ev:start-annotation-open");
    addElementHandler(new ExpectedEventHandler("startAnnotationClose"), "ev:start-annotation-close");
    addElementHandler(new ExpectedEventHandler("endAnnotationOpen"), "ev:end-annotation-open");
    addElementHandler(new ExpectedEventHandler("endAnnotationClose"), "ev:end-annotation-close");
    addElementHandler(new ExpectedEventHandler("startTagOpen"), "ev:start-tag-open");
    addElementHandler(new ExpectedEventHandler("startTagClose"), "ev:start-tag-close");
    addElementHandler(new ExpectedEventHandler("endTagOpen"), "ev:end-tag-open");
    addElementHandler(new ExpectedEventHandler("endTagClose"), "ev:end-tag-close");
    addElementHandler(new ExpectedTextHandler(), "ev:text");
  }

  public List<LMNLTest> getTests() {
    return tests;
  }

  public String getTestCode() {
    return testCodeBuilder.toString().replace(",//\n)", "//\n)");
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

  static class TestHandler extends DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      currentTest = new LMNLTest();
      testCodeBuilder.append("@Test public void parseLMNL").append(testCount++).append("() {\n");
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      tests.add(currentTest);
      currentTest = null;
      return Traversal.NEXT;
    }
  }

  static class TestParamHandler extends DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      context.openLayer();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String content = context.closeLayer().trim()//
          .replaceAll("\"", "\\\\\"")//
          .replaceAll("\n", "\\\\n");
      String name = element.getAttribute("name");
      switch (name) {
        case "lmnl":
          currentTest.setLMNL(content);
          testCodeBuilder.append("String lmnl=\"").append(content).append("\";\n");
          break;
        default:
          throw new RuntimeException("unexpected value for name: " + name);
      }
      return Traversal.NEXT;
    }
  }

  static class TestTitleHandler extends DefaultElementHandler {
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
      if (currentTest != null) {
        currentTest.setTitle(content);
        testCodeBuilder.append("    // ").append(content).append("\n");
      }
      return Traversal.NEXT;
    }
  }

  private static List<String> expectedEvents = new ArrayList<>();

  static class TestExpectHandler extends DefinitionVisitor.DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      expectedEvents.clear();
      testCodeBuilder.append("List<Event> expectedEvents = asList(//\n");
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      testCodeBuilder.append(");\n")//
          .append("assertEventsAreExpected(lmnl, expectedEvents);\n}\n");
      return Traversal.NEXT;
    }
  }

  private class ExpectedTextHandler extends DefaultElementHandler {
    @Override
    public Traversal enterElement(Element element, XmlContext context) {
      context.openLayer();
      return Traversal.NEXT;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String chars = element.getAttribute("chars");
      testCodeBuilder.append("Events.textEvent(\"").append(chars).append("\"),//\n");
      return Traversal.NEXT;
    }
  }

  private class ExpectedEventHandler extends DefaultElementHandler {
    private String event;

    ExpectedEventHandler(String event) {
      this.event = event;
    }

    @Override
    public Traversal leaveElement(Element element, XmlContext context) {
      String name = element.getAttribute("name");
      testCodeBuilder.append(event + "Event(\"").append(name).append("\"),//\n");
      return Traversal.NEXT;
    }
  }

}

