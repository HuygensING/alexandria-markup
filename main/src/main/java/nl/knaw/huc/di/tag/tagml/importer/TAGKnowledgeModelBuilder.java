package nl.knaw.huc.di.tag.tagml.importer;

import nl.knaw.huc.di.tag.tagml.grammar.TAGMLParser;
import nl.knaw.huygens.alexandria.ErrorListener;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;

import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class TAGKnowledgeModelBuilder implements TAGModelBuilder {

  private ErrorListener errorListener;

  public TAGKnowledgeModelBuilder(ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void exitDocument(final Map<String, String> namespaces) {

  }

  @Override
  public boolean isFirstTag() {
    return false;
  }

  @Override
  public void addLayer(final String newLayerId, final TAGMarkup markup, final String parentLayer) {
  }

  @Override
  public TAGMarkup addMarkup(final String tagName) {
    return new TAGMarkup(null, null);
  }

  @Override
  public void addMarkup(final TAGMarkup markup) {

  }

  @Override
  public TAGMarkup createMarkup(final String extendedTag) {
    return null;
  }

  @Override
  public TAGMarkup getMarkup(final Long rootMarkupId) {
    return null;
  }

  @Override
  public void openMarkupInLayer(final TAGMarkup markup, final String layerId) {

  }

  @Override
  public void closeMarkupInLayer(final TAGMarkup markup, final String layerName) {

  }

  @Override
  public void enterRichTextValue() {

  }

  @Override
  public void exitRichTextValue() {

  }

  @Override
  public TAGTextNode getLastTextNode() {
    return null;
  }

  @Override
  public Stream<TAGMarkup> getMarkupStreamForTextNode(final TAGTextNode previousTextNode) {
    return null;
  }

  @Override
  public TAGMarkup resumeMarkup(final TAGMarkup suspendedMarkup, final Set<String> layers) {
    return null;
  }

  @Override
  public TAGTextNode createConnectedTextNode(final String s, final Deque<TAGMarkup> allOpenMarkup) {
    return null;
  }

  @Override
  public void associateTextNodeWithMarkupForLayer(final TAGTextNode tn, final TAGMarkup markup, final String layerName) {

  }

  @Override
  public void addRefAnnotation(final TAGMarkup markup, final String aName, final String refId) {

  }

  @Override
  public void addBasicAnnotation(final TAGMarkup markup, final TAGMLParser.BasicAnnotationContext actx) {

  }

  @Override
  public List<TAGMarkup> getRelevantOpenMarkup(final Deque<TAGMarkup> allOpenMarkup) {
    return null;
  }

  @Override
  public TAGDocument getDocument() {
    return null;
  }

  @Override
  public void persist(final TAGMarkup markup) {

  }

  @Override
  public Long persist(final TAGDTO tagdto) {
    return null;
  }

  @Override
  public ErrorListener getErrorListener() {
    return errorListener;
  }

  public String asTurtle() {
    return "";
  }
}
