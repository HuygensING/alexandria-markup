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
import nl.knaw.huygens.alexandria.storage.AnnotationType;
import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import org.assertj.core.api.AbstractObjectAssert;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;

public class TAGMarkupAssert extends AbstractObjectAssert<TAGMarkupAssert, TAGMarkup> {

  private String layerName = TAGML.DEFAULT_LAYER;

  public TAGMarkupAssert(final TAGMarkup actual) {
    super(actual, TAGMarkupAssert.class);
  }

  public TAGMarkupAssert isOptional() {
    isNotNull();
    String errorMessage = "\nExpected markup %s to be optional, but it wasn't.";
    if (!actual.isOptional()) {
      failWithMessage(errorMessage, actual);
    }
    return myself;
  }

  public TAGMarkupAssert withTextNodesWithText(String... text) {
    isNotNull();
    List<String> actualTexts = actual.getTextNodeStreamForLayer(layerName)
        .map(TAGTextNode::getText)
        .collect(toList());
    assertThat(actualTexts).containsExactly(text);
    return myself;
  }

  public TAGMarkupAssert hasMarkupId(String markupId) {
    isNotNull();
    String actualMarkupId = actual.getMarkupId();
    if (!markupId.equals(actualMarkupId)) {
      failWithMessage("\nExpected markup %s to have markupId %s, but was %s.",
          actual, markupId, actualMarkupId);
    }
    return myself;
  }

  public TAGMarkupAssert hasTag(final String tag) {
    isNotNull();
    String actualTag = actual.getTag();
    if (!tag.equals(actualTag)) {
      failWithMessage("\nExpected markup %s to have tag %s, but was %s.",
          actual, tag, actualTag);
    }
    return myself;
  }

  public TAGMarkupAssert inLayer(final String layerName) {
    isNotNull();
    this.layerName = layerName;
    return myself;
  }

  public TAGMarkupAssert withStringAnnotation(String key, String value) {
    basicAnnotationAssertions(key, value, AnnotationType.String);
    return myself;
  }

  public TAGMarkupAssert withBooleanAnnotation(String key, Boolean value) {
    basicAnnotationAssertions(key, value, AnnotationType.Boolean);
    return myself;
  }

  public TAGMarkupAssert withNumberAnnotation(String key, Float value) {
    basicAnnotationAssertions(key, value, AnnotationType.Number);
    return myself;
  }

  public void withListAnnotation(final String key, final List<?> value) {
    isNotNull();
    assertAnnotationExists(key);
    assertAnnotationHasType(key, AnnotationType.List);
    assertAnnotationHasValue(key, value);
  }

  public void withObjectAnnotation(String key, Map<String, Object> value) {
    isNotNull();
    assertAnnotationExists(key);
    assertAnnotationHasType(key, AnnotationType.Object);
    assertAnnotationHasValue(key, value);
  }

  private void basicAnnotationAssertions(String key, Object value, AnnotationType number) {
    isNotNull();
    assertAnnotationExists(key);
    assertAnnotationHasType(key, number);
    assertAnnotationHasValue(key, value);
  }

  private void assertAnnotationExists(String key) {
    if (!actual.hasAnnotation(key)) {
      String annotationKeys = actual.getAnnotationStream()
          .map(TAGAnnotation::getTag)
          .collect(joining(","));
      failWithMessage(
          "\nExpected markup %s to have annotation %s, but no such annotation was found. Available annotations: %s",
          actual, actual.getDbId(), key, annotationKeys);
    }
  }

  private void assertAnnotationHasType(String key, AnnotationType expectedAnnotationType) {
    TAGAnnotation actualAnnotation = actual.getAnnotation(key);
    if (!expectedAnnotationType.equals(actualAnnotation.getType())) {
      failWithMessage(
          "\nExpected annotation %s of markup %s to be a %s annotation, but was %s",
          key, actual, expectedAnnotationType.name(), actualAnnotation.getType());
    }
  }

  private void assertAnnotationHasValue(String key, Object expectedValue) {
    TAGAnnotation actualAnnotation = actual.getAnnotation(key);
    Object actualValue = actualAnnotation.getValue();
    if (!expectedValue.equals(actualValue)) {
      failWithMessage(
          "\nExpected annotation %s of markup %s to have value %s, but was %s",
          key, actual, expectedValue, actualValue);
    }
  }

}
