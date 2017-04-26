package nl.knaw.huygens.alexandria.lmnl.data_model;

import nl.knaw.huygens.alexandria.lmnl.importer.LMNLImporter;
import org.junit.Test;

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

  @Test
  public void testIndexWithAlice() {
    String lmnl = "[excerpt}[p}\n" +
        "Alice was beginning to get very tired of sitting by her sister on the bank,\n" +
        "and of having nothing to do: once or twice she had peeped into the book her sister\n" +
        "was reading, but it had no pictures or conversations in it, \n" +
        "[q=a}and what is the use of a book,{q=a]\n" +
        "thought Alice\n" +
        "[q=a}without pictures or conversation?{q=a]\n" +
        "{p]{excerpt]";
    NodeRangeIndex index = index(lmnl);
    Set<Integer> textNodeIndices = index.getTextNodes(2); // indices of textnodes contained in range 2: q=a
    assertThat(textNodeIndices).containsExactly(1, 3);

    Set<Integer> rangeIndices = index.getRanges(1); // indices of ranges that contain textnode 1
    assertThat(rangeIndices).containsExactly(0, 1, 2); // excerpt,p,q=a
  }

  private NodeRangeIndex index(String lmnl) {
    LMNLImporter importer = new LMNLImporter();
    Document document = importer.importLMNL(lmnl);
    NodeRangeIndex index = new NodeRangeIndex(document.value());
    return index;
  }

}
