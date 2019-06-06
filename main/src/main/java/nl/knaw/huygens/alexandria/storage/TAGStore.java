package nl.knaw.huygens.alexandria.storage;

/*-
 * #%L
 * alexandria-markup-core
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

import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGElement;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;

import java.util.function.Supplier;
import java.util.stream.Stream;

public interface TAGStore extends AutoCloseable {
  void open();

  @Override
  void close();

  Long update(TAGElement TAGElement);

  void remove(TAGElement TAGElement);

  // Document
  TAGDocument createDocument();

  TAGDocument getDocument(Long documentId);

  Stream<TAGMarkup> getMarkupStream(TAGDocument document);

  Stream<TAGMarkup> getMarkupStreamForTextNode(TAGDocument document, TAGTextNode nodeToProcess);

  Stream<TAGTextNode> getTextNodeStream(TAGDocument document);

  // TextNode
  TAGTextNode createTextNode(String content);

  TAGTextNode getTextNode(Long textNodeId);

  // Markup
  TAGMarkup createMarkup(TAGDocument document, String tagName);

  TAGMarkup getMarkup(Long markupId);

  // transaction
  void runInTransaction(Runnable runner);

  <A> A runInTransaction(Supplier<A> supplier);

  // Annotation

  Long createStringAnnotationValue(String value);

  StringAnnotationValue getStringAnnotationValue(Long id);

  Long createBooleanAnnotationValue(Boolean value);

  BooleanAnnotationValue getBooleanAnnotationValue(Long id);

  Long createNumberAnnotationValue(Double value);

  NumberAnnotationValue getNumberAnnotationValue(Long id);

  Long createListAnnotationValue();

  Long createMapAnnotationValue();

  Long createReferenceValue(String value);

  ReferenceValue getReferenceValue(Long id);

  Stream<AnnotationInfo> getAnnotationStream(TAGMarkup markup);

  boolean isSuspended(TAGDocument document, TAGMarkup markup);

  boolean isAnonymous(TAGDocument document, TAGMarkup markup);

  boolean isResumed(TAGDocument document, TAGMarkup markup);
}
