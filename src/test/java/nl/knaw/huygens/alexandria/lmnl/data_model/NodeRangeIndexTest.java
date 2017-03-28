package nl.knaw.huygens.alexandria.lmnl.data_model;

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeRangeIndexTest {

  @Test
  public void testRangesFromNodes() {
    String lmnl = "[excerpt\n"//
        + "  [source [date}1915{][title}The Housekeeper{]]\n"//
        + "  [author\n"//
        + "    [name}Robert Frost{]\n"//
        + "    [dates}1874-1963{]] }\n"//
        + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
        + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
        + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
        + "{excerpt]";
    NodeRangeIndex index = index(lmnl);
    // textnode 1= "He manages to keep the upper hand"
    Set<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1
    assertThat(rangeIndices).containsExactly(1, 2); // excerpt,s,l
  }

  @Test
  public void testNodesFromRanges() {
    String lmnl = "[excerpt\n"//
        + "  [source [date}1915{][title}The Housekeeper{]]\n"//
        + "  [author\n"//
        + "    [name}Robert Frost{]\n"//
        + "    [dates}1874-1963{]] }\n"//
        + "[s}[l [n}144{n]}He manages to keep the upper hand{l]\n"//
        + "[l [n}145{n]}On his own farm.{s] [s}He's boss.{s] [s}But as to hens:{l]\n"//
        + "[l [n}146{n]}We fence our flowers in and the hens range.{l]{s]\n"//
        + "{excerpt]";
    NodeRangeIndex index = index(lmnl);
    Set<Integer> textNodeIndices = index.getTextNodes(0); // indices of textnodes contained in range 0: excerpt
    assertThat(textNodeIndices).containsExactly(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
  }

  private NodeRangeIndex index(String lmnl) {
    LMNLImporter importer = new LMNLImporter();
    Document document = importer.importLMNL(lmnl);
    NodeRangeIndex index = new NodeRangeIndex(document.value());
    return index;
  }

}
