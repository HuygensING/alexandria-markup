package nl.knaw.huc.di.tag.tagml.importer;

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public interface TAGModelBuilder {
  void exitDocument(final Map<String, String> namespaces);

  void exitText(String text, final Deque<TAGMarkup> allOpenMarkup);

  boolean isFirstTag();

  void addLayer(String newLayerId, TAGMarkup markup, String parentLayer);

  void openMarkupInLayer(TAGMarkup markup, String layerId);

  void persist(TAGMarkup markup);

  void enterRichTextValue();

  void exitRichTextValue();

  TAGMarkup addMarkup(String tagName);

  void closeMarkupInLayer(TAGMarkup markup, String layerName);

  TAGMarkup getMarkup(Long rootMarkupId);

  TAGTextNode getLastTextNode();

  Stream<TAGMarkup> getMarkupStreamForTextNode(TAGTextNode previousTextNode);

  TAGMarkup resumeMarkup(TAGMarkup suspendedMarkup, Set<String> layers);

  void associateTextNodeWithMarkupForLayer(TAGTextNode tn, TAGMarkup markup, String layerName);

  void addRefAnnotation(TAGMarkup markup, String aName, String refId);

  void addBasicAnnotation(TAGMarkup markup, TAGMLParser.BasicAnnotationContext actx);

  TAGMarkup createMarkup(String extendedTag);

  void addMarkup(TAGMarkup markup);

  List<TAGMarkup> getRelevantOpenMarkup(Deque<TAGMarkup> allOpenMarkup);

  TAGTextNode createConnectedTextNode(String s, Deque<TAGMarkup> allOpenMarkup);

  TAGDocument getDocument();

  Long persist(TAGDTO tagdto);
}
