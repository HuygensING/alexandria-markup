package nl.knaw.huc.di.tag.tagml.importer;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.model.graph.TextGraph;
import nl.knaw.huc.di.tag.model.graph.edges.AnnotationEdge;
import nl.knaw.huc.di.tag.model.graph.edges.ListItemEdge;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.BasicAnnotationContext;
import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser.IdentifyingAnnotationContext;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;
import static java.util.stream.Collectors.*;

public class AnnotationFactory {

  private TAGStore store;
  private ErrorListener errorListener;
  private TextGraph textGraph;

  public AnnotationFactory(TAGStore store, TextGraph textGraph, ErrorListener errorListener) {
    this.store = store;
    this.errorListener = errorListener;
    this.textGraph = textGraph;
  }

  public AnnotationFactory(final TAGStore store, TextGraph textGraph) {
    this(store, textGraph, null);
  }

  public AnnotationInfo makeAnnotation(BasicAnnotationContext basicAnnotationContext) {
    String aName = basicAnnotationContext.annotationName().getText();
    TAGMLParser.AnnotationValueContext annotationValueContext = basicAnnotationContext.annotationValue();
    Object value = annotationValue(annotationValueContext);
    AnnotationInfo annotationInfo = makeAnnotation(aName, annotationValueContext, value);

    if (annotationInfo == null) {
      throw new RuntimeException("unhandled basic annotation " + basicAnnotationContext.getText());
    }

    return annotationInfo;
  }

  private AnnotationInfo makeAnnotation(String aName, TAGMLParser.AnnotationValueContext annotationValueContext, Object value) {
    AnnotationInfo annotationInfo = null;
    if (value instanceof String) {
      Long id = store.createStringAnnotationValue((String) value);
      annotationInfo = new AnnotationInfo(id, AnnotationType.String, aName);

    } else if (value instanceof Boolean) {
      Long id = store.createBooleanAnnotationValue((Boolean) value);
      annotationInfo = new AnnotationInfo(id, AnnotationType.Boolean, aName);

    } else if (value instanceof Double) {
      Long id = store.createNumberAnnotationValue((Double) value);
      annotationInfo = new AnnotationInfo(id, AnnotationType.Number, aName);

    } else if (value instanceof List) {
      List<Object> list = (List<Object>) value;
      Set<String> valueTypes = list.stream()
          .map(v -> ((Object) v).getClass().getName())
          .collect(toSet());
      if (valueTypes.size() > 1) {
        errorListener.addError("%s All elements of ListAnnotation %s should be of the same type.",
            errorPrefix(annotationValueContext), aName);
      }
      Long id = store.createListAnnotationValue();
      annotationInfo = new AnnotationInfo(id, AnnotationType.List, aName);
      list.forEach(e -> {
        AnnotationInfo listElement = makeAnnotation("", annotationValueContext, e);
        textGraph.addListItem(id, listElement);
      });

    } else if (value instanceof HashMap) {
      Long id = store.createMapAnnotationValue();
      annotationInfo = new AnnotationInfo(id, AnnotationType.Map, aName);
      HashMap<String, Object> map = (HashMap<String, Object>) value;
      map.forEach((k, v) -> {
        final AnnotationInfo aInfo = makeAnnotation(k, annotationValueContext, v);
        textGraph.addAnnotationEdge(id, aInfo);
      });

    } else {
      String placeholder = annotationValueContext.getText();
      Long id = store.createStringAnnotationValue(placeholder);
      annotationInfo = new AnnotationInfo(id, AnnotationType.String, aName);
    }
    return annotationInfo;
  }

  private Object annotationValue(final TAGMLParser.AnnotationValueContext annotationValueContext) {
    if (annotationValueContext.AV_StringValue() != null) {
      return annotationValueContext.AV_StringValue().getText()
          .replaceFirst("^.", "")
          .replaceFirst(".$", "")
          .replace("\\\"", "\"")
          .replace("\\'", "'")
          ;

    } else if (annotationValueContext.booleanValue() != null) {
      return Boolean.valueOf(annotationValueContext.booleanValue().getText());

    } else if (annotationValueContext.AV_NumberValue() != null) {
      return Double.valueOf(annotationValueContext.AV_NumberValue().getText());

    } else if (annotationValueContext.listValue() != null) {
      return annotationValueContext.listValue()
          .annotationValue().stream()
          .map(this::annotationValue)
          .collect(toList());

    } else if (annotationValueContext.objectValue() != null) {
      return readObject(annotationValueContext.objectValue());

    } else if (annotationValueContext.richTextValue() != null) {
      return annotationValueContext.richTextValue().getText();
    }
    errorListener.addBreakingError("%s Cannot determine the type of this annotation: %s",
        errorPrefix(annotationValueContext), annotationValueContext.getText());
    return null;
  }

  private Map<String, Object> readObject(TAGMLParser.ObjectValueContext objectValueContext) {
    return objectValueContext.children.stream()
        .filter(c -> !(c instanceof TerminalNode))
        .map(this::parseAttribute)
        .peek(System.out::println)
        .collect(toMap(KeyValue::getKey, KeyValue::getValue, (a, b) -> b));
  }

  private KeyValue parseAttribute(ParseTree parseTree) {
    if (parseTree instanceof BasicAnnotationContext) {
      BasicAnnotationContext basicAnnotationContext = (BasicAnnotationContext) parseTree;
      String aName = basicAnnotationContext.annotationName().getText();
      TAGMLParser.AnnotationValueContext annotationValueContext = basicAnnotationContext.annotationValue();
      Object value = annotationValue(annotationValueContext);
      return new KeyValue(aName, value);

    } else if (parseTree instanceof IdentifyingAnnotationContext) {
      //TODO: deal with this identifier
      IdentifyingAnnotationContext identifyingAnnotationContext = (IdentifyingAnnotationContext) parseTree;
      String value = identifyingAnnotationContext.idValue().getText();
      return new KeyValue(":id", value);

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
    StringAnnotationValue stringAnnotationValue = store.getStringAnnotationValue(annotationInfo.getNodeId());
    return stringAnnotationValue.getValue();
  }

  public Double getNumberValue(final AnnotationInfo annotationInfo) {
    NumberAnnotationValue numberAnnotationValue = store.getNumberAnnotationValue(annotationInfo.getNodeId());
    return numberAnnotationValue.getValue();
  }

  public Boolean getBooleanValue(final AnnotationInfo annotationInfo) {
    BooleanAnnotationValue booleanAnnotationValue = store.getBooleanAnnotationValue(annotationInfo.getNodeId());
    return booleanAnnotationValue.getValue();
  }

  public List<AnnotationInfo> getListValue(AnnotationInfo annotationInfo) {
    Long nodeId = annotationInfo.getNodeId();
    return textGraph.getOutgoingEdges(nodeId).stream()
        .filter(ListItemEdge.class::isInstance)
        .map(ListItemEdge.class::cast)
        .map(this::toAnnotationInfo)
        .collect(toList());
  }

  public List<AnnotationInfo> getMapValue(final AnnotationInfo annotationInfo) {
    Long nodeId = annotationInfo.getNodeId();
    return textGraph.getOutgoingEdges(nodeId).stream()
        .filter(AnnotationEdge.class::isInstance)
        .map(AnnotationEdge.class::cast)
        .map(this::toAnnotationInfo)
        .collect(toList());
  }

  private AnnotationInfo toAnnotationInfo(ListItemEdge listItemEdge) {
    Long nodeId = textGraph.getTargets(listItemEdge).iterator().next();
    AnnotationType type = listItemEdge.getAnnotationType();
    return new AnnotationInfo(nodeId, type, "");
  }

  private AnnotationInfo toAnnotationInfo(AnnotationEdge annotationEdge) {
    Long nodeId = textGraph.getTargets(annotationEdge).iterator().next();
    AnnotationType type = annotationEdge.getAnnotationType();
    String name = annotationEdge.getField();
    return new AnnotationInfo(nodeId, type, name);
  }

  private class KeyValue {
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
