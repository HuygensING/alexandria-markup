package nl.knaw.huygens.alexandria.lmnl.data_model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;

public class NodeRangeIndexTest {

  @Test
  public void testRangesFromNodes() {
    String lmnl = "";
    NodeRangeIndex index = index(lmnl);
    List<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1
    assertThat(rangeIndices).containsExactly(1, 2, 3);
  }

  @Test
  public void testNodesFromRanges() {
    String lmnl = "";
    NodeRangeIndex index = index(lmnl);
    List<Integer> textNodeIndices = index.getTextNodes(1); // indices of textnodes contained in range 1
    assertThat(textNodeIndices).containsExactly(1, 2, 3);
  }

  private NodeRangeIndex index(String lmnl) {
    LMNLImporter importer = new LMNLImporter();
    Document document = importer.importLMNL(lmnl);
    NodeRangeIndex index = new NodeRangeIndex(document.value());
    return index;
  }

}
