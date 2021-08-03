package nl.knaw.huc.di.tag.tagml.importer2;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.AnnotationEdge;
import nl.knaw.huc.di.tag.model.graph.edges.ListItemEdge;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.AnnotationValueContext;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.BasicAnnotationContext;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.IdentifyingAnnotationContext;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.ObjectValueContext;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.RefAnnotationContext;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.AnnotationType;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class AnnotationFactory2 {

  private static final Logger LOG = LoggerFactory.getLogger(AnnotationFactory2.class);

  private TAGKnowledgeModel model;
  private ErrorListener errorListener;

  public AnnotationFactory2(
      TAGKnowledgeModel model, TextGraph textGraph, ErrorListener errorListener) {
    this.model = model;
    this.errorListener = errorListener;
  }

  public AnnotationFactory2(final TAGKnowledgeModel model, TextGraph textGraph) {
    this(model, textGraph, null);
  }

  public AnnotationInfo makeAnnotation(BasicAnnotationContext basicAnnotationContext) {
    String aName = basicAnnotationContext.annotationName().getText();
    AnnotationValueContext annotationValueContext = basicAnnotationContext.annotationValue();
    Object value = annotationValue(annotationValueContext);
    AnnotationInfo annotationInfo = makeAnnotation(aName, annotationValueContext, value);

    if (annotationInfo == null) {
      throw new RuntimeException("unhandled basic annotation " + basicAnnotationContext.getText());
    }

    return annotationInfo;
  }

  private AnnotationInfo makeAnnotation(
      String aName, AnnotationValueContext annotationValueContext, Object value) {
    AnnotationInfo annotationInfo = null;
    if (value instanceof String) {
      annotationInfo = makeStringAnnotation(aName, (String) value);

    } else if (value instanceof Boolean) {
      annotationInfo = makeBooleanAnnotation(aName, (Boolean) value);

    } else if (value instanceof Double) {
      annotationInfo = makeDoubleAnnotation(aName, (Double) value);

    } else if (value instanceof List) {
      annotationInfo = makeListAnnotation(aName, annotationValueContext, (List<Object>) value);

    } else if (value instanceof HashMap) {
      annotationInfo =
          makeMapAnnotation(aName, annotationValueContext, (HashMap<String, Object>) value);

    } else {
      annotationInfo = makeOtherAnnotation(aName, annotationValueContext);
    }
    return annotationInfo;
  }

  private AnnotationInfo makeStringAnnotation(final String aName, final String value) {
    Long id = model.createStringAnnotationValue(value);
    return new AnnotationInfo(id, AnnotationType.String, aName);
  }

  private AnnotationInfo makeBooleanAnnotation(final String aName, final Boolean value) {
    Long id = model.createBooleanAnnotationValue(value);
    return new AnnotationInfo(id, AnnotationType.Boolean, aName);
  }

  private AnnotationInfo makeDoubleAnnotation(final String aName, final Double value) {
    Long id = model.createNumberAnnotationValue(value);
    return new AnnotationInfo(id, AnnotationType.Number, aName);
  }

  public AnnotationInfo makeReferenceAnnotation(final String aName, final String value) {
    Long id = model.createReferenceValue(value);
    return new AnnotationInfo(id, AnnotationType.Reference, aName);
  }

  private AnnotationInfo makeListAnnotation(
      final String aName,
      final AnnotationValueContext annotationValueContext,
      final List<Object> value) {
    final AnnotationInfo annotationInfo;
    verifyListElementsAreSameType(aName, annotationValueContext, value);
    verifySeparatorsAreCommas(aName, annotationValueContext);
    ListAnnotationValueResource resource = model.createListAnnotationValue();
    //    annotationInfo = new AnnotationInfo(resource, AnnotationType.List, aName);
    ParseTree valueTree = annotationValueContext.children.get(0);
    int childCount = valueTree.getChildCount();
    for (int i = 1; i < childCount; i += 2) {
      ParseTree listElement = valueTree.getChild(i);
      final ParseTree subValueParseTree = listElement.getChild(0);
      Object subValue = value.get((i - 1) / 2);
      final AnnotationValueContext subValueContext = (AnnotationValueContext) listElement;
      AnnotationInfo listElementInfo = makeAnnotation("", subValueContext, subValue);
      resource.addListItem(listElementInfo);
    }
    //    return annotationInfo;
    return null;
  }

  private AnnotationInfo makeMapAnnotation(
      final String aName,
      final AnnotationValueContext annotationValueContext,
      final HashMap<String, Object> value) {
    //    Long id = store.createMapAnnotationValue();
    //    annotationInfo = new AnnotationInfo(id, AnnotationType.Map, aName);
    //    HashMap<String, Object> map = value;
    //    ParseTree valueTree = annotationValueContext.children.get(0);
    //    int childCount = valueTree.getChildCount(); // children: '{' annotation+ '}'
    //    for (int i = 1; i < childCount - 1; i++) {
    //      ParseTree hashElement = valueTree.getChild(i);
    //      String subName = hashElement.getChild(0).getText();
    //      ParseTree subValueParseTree = hashElement.getChild(2);
    //      if (subValueParseTree instanceof AnnotationValueContext) {
    //        final AnnotationValueContext subValueContext = (AnnotationValueContext)
    // subValueParseTree;
    //        final Object subValue = map.get(subName);
    //        final AnnotationInfo aInfo = makeAnnotation(subName, subValueContext, subValue);
    //        textGraph.addAnnotationEdge(id, aInfo);

    //      } else if (subValueParseTree instanceof IdValueContext) {
    //        IdValueContext idValueContext = (IdValueContext) subValueParseTree;
    //        String idValue = idValueContext.getText();
    //        LOG.warn("TODO: handle idValue {}", idValue);
    //        // TODO: handle idValue

    //      } else if (subValueParseTree instanceof RefValueContext) {
    //        RefValueContext refValueContext = (RefValueContext) subValueParseTree;
    //        String refValue = refValueContext.getText();
    //        final AnnotationInfo aInfo = makeReferenceAnnotation(subName, refValue);
    //        textGraph.addAnnotationEdge(id, aInfo);

    //      } else {
    //        throw new RuntimeException("TODO: handle " + subValueParseTree.getClass());
    //      }
    //    }
    return null;
  }

  private AnnotationInfo makeOtherAnnotation(
      final String aName, final AnnotationValueContext annotationValueContext) {
    //    String placeholder = annotationValueContext.getText();
    //    Long id = store.createStringAnnotationValue(placeholder);
    //    annotationInfo = new AnnotationInfo(id, AnnotationType.String, aName);
    return null;
  }

  private void verifyListElementsAreSameType(
      final String aName,
      final AnnotationValueContext annotationValueContext,
      final List<Object> list) {
    Set<String> valueTypes =
        list.stream().map(v -> ((Object) v).getClass().getName()).collect(toSet());
    if (valueTypes.size() > 1) {
      errorListener.addError(
          "%s All elements of ListAnnotation %s should be of the same type.",
          errorPrefix(annotationValueContext), aName);
    }
  }

  private void verifySeparatorsAreCommas(
      final String aName, final AnnotationValueContext annotationValueContext) {
    ParseTree valueTree = annotationValueContext.children.get(0);
    int childCount = valueTree.getChildCount(); // children: '[' value (separator value)* ']'
    Set<String> separators = new HashSet<>();
    for (int i = 2; i < childCount - 1; i += 2) {
      separators.add(valueTree.getChild(i).getText().trim());
    }

    boolean allSeparatorsAreCommas =
        separators.isEmpty() || (separators.size() == 1 && separators.contains(","));
    if (!allSeparatorsAreCommas) {
      errorListener.addError(
          "%s The elements of ListAnnotation %s should be separated by commas.",
          errorPrefix(annotationValueContext), aName);
    }
  }

  private Object annotationValue(final AnnotationValueContext annotationValueContext) {
    if (annotationValueContext.AV_StringValue() != null) {
      return annotationValueContext
          .AV_StringValue()
          .getText()
          .replaceFirst("^.", "")
          .replaceFirst(".$", "")
          .replace("\\\"", "\"")
          .replace("\\'", "'");

    } else if (annotationValueContext.booleanValue() != null) {
      return Boolean.valueOf(annotationValueContext.booleanValue().getText());

    } else if (annotationValueContext.AV_NumberValue() != null) {
      return Double.valueOf(annotationValueContext.AV_NumberValue().getText());

    } else if (annotationValueContext.listValue() != null) {
      return annotationValueContext.listValue().annotationValue().stream()
          .map(this::annotationValue)
          .collect(toList());

    } else if (annotationValueContext.objectValue() != null) {
      return readObject(annotationValueContext.objectValue());

    } else if (annotationValueContext.richTextValue() != null) {
      return annotationValueContext.richTextValue().getText();
    }
    errorListener.addBreakingError(
        "%s Cannot determine the type of this annotation: %s",
        errorPrefix(annotationValueContext), annotationValueContext.getText());
    return null;
  }

  private Map<String, Object> readObject(ObjectValueContext objectValueContext) {
    Map<String, Object> map = new LinkedHashMap<>();
    objectValueContext.children.stream()
        .filter(c -> !(c instanceof TerminalNode))
        .map(this::parseAttribute)
        //        .peek(System.out::println)
        .forEach(kv -> map.put(kv.key, kv.value));
    return map;
  }

  private KeyValue parseAttribute(ParseTree parseTree) {
    if (parseTree instanceof BasicAnnotationContext) {
      BasicAnnotationContext basicAnnotationContext = (BasicAnnotationContext) parseTree;
      String aName = basicAnnotationContext.annotationName().getText();
      AnnotationValueContext annotationValueContext = basicAnnotationContext.annotationValue();
      Object value = annotationValue(annotationValueContext);
      return new KeyValue(aName, value);

    } else if (parseTree instanceof IdentifyingAnnotationContext) {
      // TODO: deal with this identifier
      IdentifyingAnnotationContext identifyingAnnotationContext =
          (IdentifyingAnnotationContext) parseTree;
      String value = identifyingAnnotationContext.idValue().getText();
      return new KeyValue(":id", value);

    } else if (parseTree instanceof RefAnnotationContext) {
      RefAnnotationContext refAnnotationContext = (RefAnnotationContext) parseTree;
      final String aName = refAnnotationContext.annotationName().getText();
      final String value = refAnnotationContext.refValue().getText();
      return new KeyValue("!" + aName, value);

    } else {
      throw new RuntimeException("unhandled type " + parseTree.getClass().getName());
      //      errorListener.addBreakingError("%s Cannot determine the type of this annotation: %s",
      //          errorPrefix(parseTree.), parseTree.getText());

    }
  }

  private String errorPrefix(ParserRuleContext ctx) {
    Token token = ctx.start;
    return format("line %d:%d :", token.getLine(), token.getCharPositionInLine() + 1);
  }

  public String getStringValue(final AnnotationInfo annotationInfo) {
    //    StringAnnotationValue stringAnnotationValue =
    // store.getStringAnnotationValue(annotationInfo.getNodeId());
    //    return stringAnnotationValue.getValue();
    return null;
  }

  public Double getNumberValue(final AnnotationInfo annotationInfo) {
    //    NumberAnnotationValue numberAnnotationValue =
    // store.getNumberAnnotationValue(annotationInfo.getNodeId());
    //    return numberAnnotationValue.getValue();
    return null;
  }

  public Boolean getBooleanValue(final AnnotationInfo annotationInfo) {
    //    BooleanAnnotationValue booleanAnnotationValue =
    // store.getBooleanAnnotationValue(annotationInfo.getNodeId());
    //    return booleanAnnotationValue.getValue();
    return null;
  }

  public List<AnnotationInfo> getListValue(AnnotationInfo annotationInfo) {
    //    Long nodeId = annotationInfo.getNodeId();
    //    return textGraph.getOutgoingEdges(nodeId).stream()
    //        .filter(ListItemEdge.class::isInstance)
    //        .map(ListItemEdge.class::cast)
    //        .map(this::toAnnotationInfo)
    //        .collect(toList());
    return null;
  }

  public List<AnnotationInfo> getMapValue(final AnnotationInfo annotationInfo) {
    //    Long nodeId = annotationInfo.getNodeId();
    //    return textGraph.getOutgoingEdges(nodeId).stream()
    //        .filter(AnnotationEdge.class::isInstance)
    //        .map(AnnotationEdge.class::cast)
    //        .map(this::toAnnotationInfo)
    //        .collect(toList());
    return null;
  }

  public String getReferenceValue(final AnnotationInfo annotationInfo) {
    //    Long nodeId = annotationInfo.getNodeId();
    //    return store.getReferenceValue(nodeId).getValue();
    return null;
  }

  private AnnotationInfo toAnnotationInfo(ListItemEdge listItemEdge) {
    //    Long nodeId = textGraph.getTargets(listItemEdge).iterator().next();
    //    AnnotationType type = listItemEdge.getAnnotationType();
    //    return new AnnotationInfo(nodeId, type, "");
    return null;
  }

  private AnnotationInfo toAnnotationInfo(AnnotationEdge annotationEdge) {
    //    Long nodeId = textGraph.getTargets(annotationEdge).iterator().next();
    //    AnnotationType type = annotationEdge.getAnnotationType();
    //    String name = annotationEdge.getField();
    //    return new AnnotationInfo(nodeId, type, name);
    return null;
  }

  private static class KeyValue {
    private String key;
    private Object value;

    public KeyValue(String key, Object value) {
      this.key = key;
      this.value = value;
    }

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public Object getValue() {
      return value;
    }

    public void setValue(Object value) {
      this.value = value;
    }
  }
}
