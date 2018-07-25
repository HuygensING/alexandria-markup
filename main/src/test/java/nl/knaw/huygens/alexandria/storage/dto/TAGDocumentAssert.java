package nl.knaw.huygens.alexandria.storage.dto;

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

import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.data_model.Annotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.assertj.core.api.AbstractObjectAssert;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class TAGDocumentAssert extends AbstractObjectAssert<TAGDocumentAssert, TAGDocument> {

  public TAGDocumentAssert(final TAGDocument actual) {
    super(actual, TAGDocumentAssert.class);
  }

  public TAGDocumentAssert hasTextNodesMatching(final TextNodeSketch... textNodeSketches) {
    isNotNull();
    Set<TextNodeSketch> actualTextNodeSketches = getActualTextNodeSketches();
    Set<TextNodeSketch> expectedTextNodeSketches = new HashSet<>(Arrays.asList(textNodeSketches));
    expectedTextNodeSketches.removeAll(actualTextNodeSketches);

    String errorMessage = "\nNo TextNodes found matching %s;\nNodes found: %s";
    if (!expectedTextNodeSketches.isEmpty()) {
      failWithMessage(errorMessage, expectedTextNodeSketches, actualTextNodeSketches);
    }
    return myself;
  }

  public TAGDocumentAssert hasMarkupMatching(final MarkupSketch... markupSketches) {
    isNotNull();
    Set<MarkupSketch> actualMarkupSketches = getActualMarkupSketches();
    Set<MarkupSketch> expectedMarkupSketches = new HashSet<>(Arrays.asList(markupSketches));
    expectedMarkupSketches.removeAll(actualMarkupSketches);

    String errorMessage = "\nNo Markup found matching %s;\nMarkup found: %s";
    if (!expectedMarkupSketches.isEmpty()) {
      failWithMessage(errorMessage, expectedMarkupSketches, actualMarkupSketches);
    }
    return myself;
  }

  public TAGMarkupAssert hasMarkupWithTag(String tag) {
    isNotNull();
    List<TAGMarkup> relevantMarkup = actual.getMarkupStream()
//        .peek(System.out::println)
        .filter(m -> m.hasTag(tag))
        .collect(toList());
    if (relevantMarkup.isEmpty()) {
      failWithMessage("No markup found with tag %s", tag);
    }

    TAGMarkup markup = relevantMarkup.get(0);
    return new TAGMarkupAssert(markup);
  }

  private Set<TextNodeSketch> getActualTextNodeSketches() {
    return actual.getTextNodeStream()//
        .map(this::toTextNodeSketch)//
        .collect(toSet());
  }

  private TextNodeSketch toTextNodeSketch(final TAGTextNode textNode) {
    return textNodeSketch(textNode.getText());
  }

  //  public DocumentWrapperAssert hasLayerIds(final String... layerId) {
//    isNotNull();
//    List<String> actualLayerIds = actual.getLayerNames();
//    return myself;
//  }
//
  public static class TextNodeSketch {

    private final String text;

    public TextNodeSketch(final String text) {
      this.text = text;
    }

    @Override
    public int hashCode() {
      return text.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof TextNodeSketch
          && ((TextNodeSketch) obj).text.equals(text);
    }

    @Override
    public String toString() {
      return text;
    }
  }

  public static TextNodeSketch textNodeSketch() {
    return textNodeSketch("");
  }

  public static TextNodeSketch textNodeSketch(final String text) {
    return new TextNodeSketch(text);
  }

  public static TextNodeSketch textDivergenceSketch() {
    return textNodeSketch(TAGML.DIVERGENCE);
  }

  public static TextNodeSketch textConvergenceSketch() {
    return textNodeSketch(TAGML.CONVERGENCE);
  }

  public static class MarkupSketch {
    private final String tag;
    private final List<Annotation> annotations;
    private boolean optional = false;

    MarkupSketch(String tag, List<Annotation> annotations, Boolean optional) {
      this.tag = tag;
      this.annotations = annotations;
      this.optional = optional;
    }

    @Override
    public int hashCode() {
      return tag.hashCode() + annotations.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MarkupSketch
          && ((MarkupSketch) obj).tag.equals(tag)
          && ((MarkupSketch) obj).annotations.equals(annotations);
    }

    @Override
    public String toString() {
      return String.format("MarkupSketch(%s %s)", tag, annotations);
    }

    public boolean isOptional() {
      return optional;
    }
  }

  public static MarkupSketch markupSketch(String tag, List<Annotation> annotations, Boolean optional) {
    return new MarkupSketch(tag, annotations, optional);
  }

  public static MarkupSketch markupSketch(String tag, boolean optional) {
    return markupSketch(tag, new ArrayList<>(), optional);
  }

  public static MarkupSketch markupSketch(String tag) {
    return markupSketch(tag, new ArrayList<>(), false);
  }

  public static MarkupSketch optionalMarkupSketch(String tag) {
    return markupSketch(tag, new ArrayList<>(), true);
  }

  private Set<MarkupSketch> getActualMarkupSketches() {
    return actual.getMarkupStream()//
        .map(this::toMarkupSketch)//
        .collect(toSet());
  }

  public MarkupSketch toMarkupSketch(TAGMarkup markup) {
    return markupSketch(markup.getTag(), markup.isOptional());
  }

}
