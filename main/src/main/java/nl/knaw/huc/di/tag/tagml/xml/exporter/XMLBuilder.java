package nl.knaw.huc.di.tag.tagml.xml.exporter;

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

import nl.knaw.huc.di.tag.TAGVisitor;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.joining;

public class XMLBuilder implements TAGVisitor {
  public static final String TH_NAMESPACE = "xmlns:th=\"http://www.blackmesatech.com/2017/nss/trojan-horse\"";

  final StringBuilder xmlBuilder = new StringBuilder();
  final Map<Object, String> thIds = new HashMap<>();
  final AtomicInteger thIdCounter = new AtomicInteger();
  final List<String> namespaceDefinitions = new ArrayList<>();

  public String getResult() {
    return xmlBuilder.toString();
  }

  @Override
  public void enterDocument(TAGDocument document) {
    xmlBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
    final int numberOfHierarchies = document.getLayerNames().size();
    if (numberOfHierarchies > 1 ||
        (numberOfHierarchies == 1 && !document.getLayerNames().iterator().next().equals(TAGML.DEFAULT_LAYER))) {
      namespaceDefinitions.add(TH_NAMESPACE);
    }
    document.getNamespaces().forEach((ns, url) -> {
      namespaceDefinitions.add("xmlns:" + ns + "=\"" + url + "\"");
    });
    xmlBuilder.append("<xml");
    if (!namespaceDefinitions.isEmpty()) {
      xmlBuilder.append(" ").append(namespaceDefinitions.stream().collect(joining(" ")));
    }
    Set<String> layerNames = document.getLayerNames();
    if (!justDefaultLayer(layerNames)) {
      final String thDoc = getThDoc(layerNames);
      xmlBuilder.append(" th:doc=\"").append(thDoc).append("\"");
    }
    xmlBuilder.append(">\n");
  }

  private String getThDoc(final Set<String> layerNames) {
    return layerNames.stream().collect(joining(" "));
  }

  @Override
  public void exitDocument(final TAGDocument document) {
    xmlBuilder.append("\n</xml>");
  }

  @Override
  public void enterOpenTag(final TAGMarkup markup) {
    final String markupName = markup.getTag();
    xmlBuilder.append("<").append(markupName);
  }

  @Override
  public void addAnnotation(final AnnotationInfo annotationInfo) {
    xmlBuilder.append(" ").append(annotationInfo.getName()).append("='").append("'");
  }

  @Override
  public void exitOpenTag(final TAGMarkup markup) {
    Set<String> layers = markup.getLayers();
    final boolean inDefaultLayer = justDefaultLayer(layers);
    if (!inDefaultLayer) {
      String thId = markup.getTag() + thIdCounter.getAndIncrement();
      thIds.put(markup, thId);
      final String thDoc = getThDoc(layers);

      xmlBuilder.append(" th:doc=\"").append(thDoc).append("\"")
          .append(" th:sId=\"").append(thId).append("\"/");
    } else if (markup.isAnonymous()) {
      xmlBuilder.append("/");
    }
    xmlBuilder.append(">");
  }

  private boolean justDefaultLayer(final Set<String> layers) {
    return layers.size() == 1 && layers.iterator().next().equals(TAGML.DEFAULT_LAYER);
  }

  @Override
  public void exitCloseTag(final TAGMarkup markup) {
    final String markupName = markup.getTag();
    Set<String> layers = markup.getLayers();
    final boolean inDefaultLayer = justDefaultLayer(layers);
    if (inDefaultLayer && markup.isAnonymous()) {
      return;
    }
    xmlBuilder.append("<");
    if (inDefaultLayer) {
      xmlBuilder.append("/");
    }
    xmlBuilder.append(markupName);
    if (!inDefaultLayer) {
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

  }

  @Override
  public void exitTextVariation() {

  }

}
