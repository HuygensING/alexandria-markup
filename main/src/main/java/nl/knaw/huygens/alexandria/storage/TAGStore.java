package nl.knaw.huygens.alexandria.storage;

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

import java.util.function.Supplier;

import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;

public interface TAGStore extends AutoCloseable {
  void open();

  @Override
  void close();

  Long persist(TAGDTO tagdto);

  void remove(TAGDTO tagdto);

  // Document
  TAGDocumentDTO getDocumentDTO(Long documentId);

  TAGDocument getDocument(Long documentId);

  TAGDocument createDocument();

  // TextNode
  TAGTextNodeDTO getTextNodeDTO(Long textNodeId);

  TAGTextNode createTextNode(String content);

  TAGTextNode createTextNode();

  TAGTextNode getTextNode(Long textNodeId);

  // Markup
  TAGMarkupDTO getMarkupDTO(Long markupId);

  TAGMarkup createMarkup(TAGDocument document, String tagName);

  TAGMarkup getMarkup(Long markupId);

  // transaction
  void runInTransaction(Runnable runner);

  <A> A runInTransaction(Supplier<A> supplier);

  Long createStringAnnotationValue(String value);

  Long createBooleanAnnotationValue(Boolean value);

  Long createNumberAnnotationValue(Double value);

  Long createListAnnotationValue();

  Long createMapAnnotationValue();

  Long createReferenceValue(String value);

  StringAnnotationValue getStringAnnotationValue(Long id);

  NumberAnnotationValue getNumberAnnotationValue(Long id);

  BooleanAnnotationValue getBooleanAnnotationValue(Long id);

  ReferenceValue getReferenceValue(Long id);
}
