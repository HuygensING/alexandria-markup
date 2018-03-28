package nl.knaw.huygens.alexandria.storage.wrappers;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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
import nl.knaw.huygens.alexandria.data_model.Annotation;
import org.assertj.core.api.AbstractObjectAssert;

import java.util.*;

import static java.util.stream.Collectors.toSet;

public class DocumentWrapperAssert extends AbstractObjectAssert<DocumentWrapperAssert, DocumentWrapper> {

  public DocumentWrapperAssert(final DocumentWrapper actual) {
    super(actual, DocumentWrapperAssert.class);
  }

  public DocumentWrapperAssert hasTextNodesMatching(final TextNodeSketch... textNodeSketches) {
    Set<TextNodeSketch> actualTextNodeSketches = getActualTextNodeSketches();
    Set<TextNodeSketch> expectedTextNodeSketches = new HashSet<>(Arrays.asList(textNodeSketches));
    expectedTextNodeSketches.removeAll(actualTextNodeSketches);

    String errorMessage = "\nNo TextNodes found matching %s;\nNodes found: %s";
    if (!expectedTextNodeSketches.isEmpty()) {
      failWithMessage(errorMessage, expectedTextNodeSketches, actualTextNodeSketches);
    }
    return myself;
  }

  public DocumentWrapperAssert hasMarkupMatching(final MarkupSketch... markupSketches) {
    Set<MarkupSketch> actualMarkupSketches = getActualMarkupSketches();
    Set<MarkupSketch> expectedMarkupSketches = new HashSet<>(Arrays.asList(markupSketches));
    expectedMarkupSketches.removeAll(actualMarkupSketches);

    String errorMessage = "\nNo Markup found matching %s;\nMarkup found: %s";
    if (!expectedMarkupSketches.isEmpty()) {
      failWithMessage(errorMessage, expectedMarkupSketches, actualMarkupSketches);
    }
    return myself;
  }

  private Set<TextNodeSketch> getActualTextNodeSketches() {
    return actual.getTextNodeStream()//
        .map(this::toTextNodeSketch)//
        .collect(toSet());
  }

  private TextNodeSketch toTextNodeSketch(final TextNodeWrapper textNodeWrapper) {
    return textNodeSketch(textNodeWrapper.getText());
  }

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

  public static class MarkupSketch {
    private final String tag;
    private final List<Annotation> annotations;

    MarkupSketch(String tag, List<Annotation> annotations) {
      this.tag = tag;
      this.annotations = annotations;
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
  }

  public static MarkupSketch markupSketch(String tag, List<Annotation> annotations) {
    return new MarkupSketch(tag, annotations);
  }

  public static MarkupSketch markupSketch(String tag) {
    return markupSketch(tag, new ArrayList<>());
  }

  private Set<MarkupSketch> getActualMarkupSketches() {
    return actual.getMarkupStream()//
        .map(this::toMarkupSketch)//
        .collect(toSet());
  }

  public MarkupSketch toMarkupSketch(MarkupWrapper markup) {
    return markupSketch(markup.getTag());
  }

}
