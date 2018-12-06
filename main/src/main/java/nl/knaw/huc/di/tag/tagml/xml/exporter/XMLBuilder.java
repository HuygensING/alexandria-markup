package nl.knaw.huc.di.tag.tagml.xml.exporter;

/*-
 * #%L
 * main
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

import nl.knaw.huc.di.tag.TAGVisitor;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.join;
import static java.util.stream.Collectors.joining;

public class XMLBuilder implements TAGVisitor {
  public static final String TH_NAMESPACE = "xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\"";
  public static final String TAG_NAMESPACE = "xmlns:tag=\"http://tag.di.huc.knaw.nl/ns/tag\"";
  public static final String DEFAULT_DOC = "_default";

  final StringBuilder xmlBuilder = new StringBuilder();
  final Map<Object, String> thIds = new HashMap<>();
  final AtomicInteger thIdCounter = new AtomicInteger(0);
  final List<String> namespaceDefinitions = new ArrayList<>();
  boolean useTagNamespace = false;
  boolean useTrojanHorse = false;
  private Set<String> relevantLayers;
  private String result;
  private TAGView tagView;
  private final AtomicInteger discontinuityCounter = new AtomicInteger(1);
  private final Map<String, Integer> discontinuityNumber = new HashMap<>();

  public String getResult() {
    return result;
  }

  @Override
  public void setView(final TAGView tagView) {
    this.tagView = tagView;
  }

  @Override
  public void setRelevantLayers(final Set<String> relevantLayers) {
    useTrojanHorse = relevantLayers.size() > 1;
    this.relevantLayers = relevantLayers;
  }

  @Override
  public void enterDocument(TAGDocument document) {
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    if (relevantLayers.size() > 1) {
      namespaceDefinitions.add(TH_NAMESPACE);
    }
    document.getNamespaces().forEach((ns, url) -> namespaceDefinitions.add("xmlns:" + ns + "=\"" + url + "\""));
    xmlBuilder.append("<xml");
    if (!namespaceDefinitions.isEmpty()) {
      xmlBuilder.append(" ").append(join(" ", namespaceDefinitions));
    }
    if (useTrojanHorse) {
      final String thDoc = getThDoc(relevantLayers);
      xmlBuilder.append(" th:doc=\"").append(thDoc).append("\"");
    }
    xmlBuilder.append(">\n");
  }

  @Override
  public void exitDocument(final TAGDocument document) {
    xmlBuilder.append("\n</xml>");
    result = xmlBuilder.toString();
    if (useTagNamespace) {
      result = result.replaceFirst("<xml", "<xml " + TAG_NAMESPACE);
    }
  }

  @Override
  public void enterOpenTag(final TAGMarkup markup) {
    String markupName = getMarkupName(markup);
    xmlBuilder.append("<").append(markupName);
    if (markup.isOptional()) {
      useTagNamespace = true;
      xmlBuilder.append(" tag:optional=\"true\"");
    }
    String discontinuityKey = discontinuityKey(markup, markupName);
    if (markup.isSuspended()) {
      useTagNamespace = true;
      final Integer n = discontinuityCounter.getAndIncrement();
      discontinuityNumber.put(discontinuityKey, n);
      xmlBuilder.append(" tag:n=\"").append(n).append("\"");

    } else if (markup.isResumed()) {
      final Integer n = discontinuityNumber.get(discontinuityKey);
      xmlBuilder.append(" tag:n=\"").append(n).append("\"");
    }
  }

  private String discontinuityKey(final TAGMarkup markup, final String markupName) {
    return markup.getLayers().stream()
            .sorted()
            .collect(joining(",", markupName + "|", ""));
  }

  private String getMarkupName(final TAGMarkup markup) {
    String markupName = markup.getTag();
    if (markupName.startsWith(":")) {
      markupName = "tag" + markupName;
      useTagNamespace = true;
    }
    return markupName;
  }

  @Override
  public void addAnnotation(String serializedAnnotation) {
    xmlBuilder.append(" ").append(serializedAnnotation);
  }

  @Override
  public void exitOpenTag(final TAGMarkup markup) {
    Set<String> layers = markup.getLayers();
    layers.retainAll(relevantLayers);
    if (useTrojanHorse) {
      String thId = markup.getTag() + thIdCounter.getAndIncrement();
      thIds.put(markup, thId);
      final String thDoc = getThDoc(layers);

      String id = markup.isAnonymous() ? "soleId" : "sId";
      xmlBuilder.append(" th:doc=\"").append(thDoc).append("\"")
          .append(" th:").append(id).append("=\"").append(thId).append("\"/");

    } else if (markup.isAnonymous()) {
      xmlBuilder.append("/");
    }
    xmlBuilder.append(">");
  }

  @Override
  public void exitCloseTag(final TAGMarkup markup) {
    String markupName = getMarkupName(markup);
    Set<String> layers = markup.getLayers();
    layers.retainAll(relevantLayers);
    if (markup.isAnonymous()) {
      return;
    }
    xmlBuilder.append("<");
    if (!useTrojanHorse) {
      xmlBuilder.append("/");
    }
    xmlBuilder.append(markupName);
    if (useTrojanHorse) {
      final String thDoc = getThDoc(layers);
      String thId = thIds.remove(markup);
      xmlBuilder.append(" th:doc=\"").append(thDoc).append("\"")
          .append(" th:eId=\"").append(thId).append("\"/");
    }
    xmlBuilder.append(">");
  }

  @Override
  public void exitText(final String text, final boolean inVariation) {
    String xmlEscapedText = StringEscapeUtils.escapeXml11(text);
    xmlBuilder.append(xmlEscapedText);
  }

  @Override
  public void enterTextVariation() {
    useTagNamespace = true;
  }

  @Override
  public void exitTextVariation() {
  }

  @Override
  public String serializeStringAnnotationValue(String stringValue) {
    return "\"" + StringEscapeUtils.escapeXml11(stringValue) + "\"";
  }

  @Override
  public String serializeNumberAnnotationValue(Double numberValue) {
    return serializeStringAnnotationValue(String.valueOf(numberValue).replaceFirst(".0$", ""));
  }

  @Override
  public String serializeBooleanAnnotationValue(Boolean booleanValue) {
    return serializeStringAnnotationValue(booleanValue.toString());
  }

  @Override
  public String serializeListAnnotationValue(List<String> serializedItems) {
    return serializeStringAnnotationValue(serializedItems.stream()
        .collect(joining(",", "[", "]")));
  }

  @Override
  public String serializeMapAnnotationValue(List<String> serializedMapItems) {
    return serializeStringAnnotationValue(serializedMapItems.stream()
        .collect(joining(",", "{", "}")));
  }

  @Override
  public String serializeAnnotationAssigner(String name) {
    return name + "=";
  }

  private String getThDoc(final Set<String> layerNames) {
    return layerNames.stream()
        .map(l -> TAGML.DEFAULT_LAYER.equals(l) ? DEFAULT_DOC : l)
        .sorted()
        .collect(joining(" "));
  }
}
