package nl.knaw.huygens.alexandria.storage;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import static com.sleepycat.persist.model.Relationship.ONE_TO_MANY;
import com.sleepycat.persist.model.SecondaryKey;

import java.util.*;

@Entity(version = 1)
public class TAGDocument implements TAGObject {
  // previously: Limen
  @PrimaryKey(sequence = "document_pk_sequence")
  private Long id;

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGTextNode.class)
  private List<Long> textNodeIds = new ArrayList<>();

  @SecondaryKey(relate = ONE_TO_MANY, relatedEntity = TAGMarkup.class)
  private List<Long> markupIds = new ArrayList<>();

  private final Map<Long, Set<Long>> textNodeIdToMarkupIds = new LinkedHashMap<>();

  protected TAGDocument() {
  }

  public Long getId() {
    return id;
  }

  public List<Long> getTextNodeIds() {
    return textNodeIds;
  }

  public void setTextNodeIds(List<Long> textNodeIds) {
    this.textNodeIds = textNodeIds;
  }

  public List<Long> getMarkupIds() {
    return markupIds;
  }

  public void setMarkupIds(List<Long> markupIds) {
    this.markupIds = markupIds;
  }

  public boolean hasTextNodes() {
    return !getTextNodeIds().isEmpty();
  }

  public Map<Long, Set<Long>> getTextNodeIdToMarkupIds() {
    return textNodeIdToMarkupIds;
  }

  public boolean containsAtLeastHalfOfAllTextNodes(TAGMarkup markup) {
    int textNodeSize = textNodeIds.size();
    return textNodeSize > 2 //
        && markup.getTextNodeIds().size() >= textNodeSize / 2d;
  }

  public Set<Long> getMarkupIdsForTextNodeIds(Long textNodeId) {
    Set<Long> markups = textNodeIdToMarkupIds.get(textNodeId);
    return markups == null ? new LinkedHashSet<>() : markups;
  }

  public void addTextNode(TAGTextNode textNode) {
    textNodeIds.add(textNode.getId());
  }

  public void associateTextWithMarkup(TAGTextNode textNode, TAGMarkup markup) {
    textNodeIdToMarkupIds.computeIfAbsent(textNode.getId(), f -> new LinkedHashSet<>()).add(markup.getId());
  }

}
