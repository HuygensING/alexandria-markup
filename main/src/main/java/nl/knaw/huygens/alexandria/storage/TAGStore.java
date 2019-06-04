package nl.knaw.huygens.alexandria.storage;

import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;

import java.util.function.Supplier;

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
