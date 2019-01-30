package nl.knaw.huc.di.tag.treediff;

import nl.knaw.huc.di.tag.treediff.TreeDiff.Mapping;
import nl.knaw.huc.di.tag.treediff.TreeDiff.TreeDiffResult;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huc.di.tag.treediff.TreeDiff.computeDiff;
import static nl.knaw.huc.di.tag.treediff.TreeDiff.produceHumanFriendlyMapping;
import static org.junit.Assert.assertEquals;

public class TestTreeDiff {
  private Tree treeOne;
  private Tree treeTwo;
  private Tree treeThree;
  private Tree treeFour;

  @Before
  public void setUp() {
    TreeNode a = new TreeNode("A");
    TreeNode b = new TreeNode("B");
    a.addChild(b);
    TreeNode d = new TreeNode("D");
    b.addChild(d);
    treeOne = new Tree(a);
    treeOne.buildCaches();

    a = new TreeNode("A");
    b = new TreeNode("B");
    TreeNode c = new TreeNode("C");
    d = new TreeNode("D");
    a.addChild(b);
    a.addChild(c);
    c.addChild(d);
    treeTwo = new Tree(a);
    treeTwo.buildCaches();

    a = new TreeNode("A");
    b = new TreeNode("B");
    c = new TreeNode("C");
    d = new TreeNode("D");
    TreeNode e = new TreeNode("E");
    a.addChild(b);
    a.addChild(c);
    c.addChild(d);
    d.addChild(e);
    treeThree = new Tree(a);
    treeThree.buildCaches();

    a = new TreeNode("A");
    b = new TreeNode("B");
    c = new TreeNode("CC");
    d = new TreeNode("D");
    e = new TreeNode("E");
    a.addChild(b);
    a.addChild(c);
    c.addChild(d);
    d.addChild(e);
    treeFour = new Tree(a);
    treeFour.buildCaches();
  }

  @Test
  public void testTreeDiff12() {
    final TreeDiffResult computeDiff = computeDiff(treeOne, treeTwo);
    Integer distance = computeDiff.distance;
    assertThat(distance).isEqualTo(2);

    Mapping mapping = computeDiff.mapping;
    String expectedMapping = "[(1,1), (2,3), (3,4), (α,2)]";
    assertEquals(expectedMapping, mapping.toString());

    List<String> description = produceHumanFriendlyMapping(mapping, treeOne, treeTwo);
    List<String> expected = new ArrayList<>(asList(
        "No change for A (@1 and @1)",
        "Change from B (@2) to C (@3)",
        "No change for D (@3 and @4)",
        "Insert B (@2)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff13() {
    final TreeDiffResult computeDiff = computeDiff(treeOne, treeThree);
    Integer distance = computeDiff.distance;
    assertThat(distance).isEqualTo(3);

    final Mapping mapping = computeDiff.mapping;
    final String expectedMapping = "[(1,1), (2,3), (3,4), (α,2), (α,5)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeOne, treeThree);
    final List<String> expected = new ArrayList<>(asList(
        "No change for A (@1 and @1)",
        "Change from B (@2) to C (@3)",
        "No change for D (@3 and @4)",
        "Insert B (@2)",
        "Insert E (@5)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff23() {
    TreeDiffResult computeDiff = computeDiff(treeTwo, treeThree);
    assertThat(computeDiff.distance).isEqualTo(1);

    final Mapping mapping = computeDiff.mapping;
    final String expectedMapping = "[(1,1), (2,2), (3,3), (4,4), (α,5)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeTwo, treeThree);
    final List<String> expected = new ArrayList<>(asList(
        "No change for A (@1 and @1)",
        "No change for B (@2 and @2)",
        "No change for C (@3 and @3)",
        "No change for D (@4 and @4)",
        "Insert E (@5)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff34() {
    TreeDiffResult computeDiff = computeDiff(treeThree, treeFour);
    Integer distance = computeDiff.distance;
    assertThat(distance).isEqualTo(1);

    final Mapping mapping = computeDiff.mapping;
    final String expectedMapping = "[(1,1), (2,2), (3,3), (4,4), (5,5)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeThree, treeFour);
    final List<String> expected = new ArrayList<>(asList(
        "No change for A (@1 and @1)",
        "No change for B (@2 and @2)",
        "Change from C (@3) to CC (@3)",
        "No change for D (@4 and @4)",
        "No change for E (@5 and @5)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff22() {
    TreeDiffResult computeDiff = computeDiff(treeTwo, treeTwo);
    Integer distance = computeDiff.distance;
    assertThat(distance).isEqualTo(0);

    final Mapping mapping = computeDiff.mapping;
    final String expectedMapping = "[(1,1), (2,2), (3,3), (4,4)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeTwo, treeTwo);
    final List<String> expected = new ArrayList<>(asList(
        "No change for A (@1 and @1)",
        "No change for B (@2 and @2)",
        "No change for C (@3 and @3)",
        "No change for D (@4 and @4)"));
    assertEquals(expected, description);
  }
}
