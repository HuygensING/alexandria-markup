package nl.knaw.huc.di.tag.treediff;

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

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huc.di.tag.treediff.TreeDiffer.Mapping;
import nl.knaw.huc.di.tag.treediff.TreeDiffer.TreeMapping;

import static java.util.Arrays.asList;
import static nl.knaw.huc.di.tag.TAGAssertions.assertThat;
import static nl.knaw.huc.di.tag.treediff.TreeDiffer.computeDiff;
import static nl.knaw.huc.di.tag.treediff.TreeDiffer.produceHumanFriendlyMapping;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TreeDifferTest {
  private static final Logger LOG = LoggerFactory.getLogger(TreeDifferTest.class);
  private Tree treeOne;
  private Tree treeTwo;
  private Tree treeThree;
  private Tree treeFour;

  @BeforeEach
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
    final TreeMapping diff = computeDiff(treeOne, treeTwo);
    Integer distance = diff.cost;
    assertThat(distance).isEqualTo(2);

    Mapping mapping = diff.mapping;
    String expectedMapping = "[(1,1), (2,3), (3,4), (null,2)]";
    assertEquals(expectedMapping, mapping.toString());

    List<String> description = produceHumanFriendlyMapping(mapping, treeOne, treeTwo);
    List<String> expected =
        new ArrayList<>(
            asList(
                "No change for A (@1 and @1)",
                "Change from B (@2) to C (@3)",
                "No change for D (@3 and @4)",
                "Insert B (@2)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff13() {
    final TreeMapping diff = computeDiff(treeOne, treeThree);
    Integer distance = diff.cost;
    assertThat(distance).isEqualTo(3);

    final Mapping mapping = diff.mapping;
    final String expectedMapping = "[(1,1), (2,3), (3,4), (null,2), (null,5)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeOne, treeThree);
    final List<String> expected =
        new ArrayList<>(
            asList(
                "No change for A (@1 and @1)",
                "Change from B (@2) to C (@3)",
                "No change for D (@3 and @4)",
                "Insert B (@2)",
                "Insert E (@5)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff23() {
    TreeMapping diff = computeDiff(treeTwo, treeThree);
    assertThat(diff.cost).isEqualTo(1);

    final Mapping mapping = diff.mapping;
    final String expectedMapping = "[(1,1), (2,2), (3,3), (4,4), (null,5)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeTwo, treeThree);
    final List<String> expected =
        new ArrayList<>(
            asList(
                "No change for A (@1 and @1)",
                "No change for B (@2 and @2)",
                "No change for C (@3 and @3)",
                "No change for D (@4 and @4)",
                "Insert E (@5)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff34() {
    TreeMapping diff = computeDiff(treeThree, treeFour);
    Integer distance = diff.cost;
    assertThat(distance).isEqualTo(1);

    final Mapping mapping = diff.mapping;
    final String expectedMapping = "[(1,1), (2,2), (3,3), (4,4), (5,5)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeThree, treeFour);
    final List<String> expected =
        new ArrayList<>(
            asList(
                "No change for A (@1 and @1)",
                "No change for B (@2 and @2)",
                "Change from C (@3) to CC (@3)",
                "No change for D (@4 and @4)",
                "No change for E (@5 and @5)"));
    assertEquals(expected, description);
  }

  @Test
  public void testTreeDiff22() {
    TreeMapping diff = computeDiff(treeTwo, treeTwo);
    Integer distance = diff.cost;
    assertThat(distance).isEqualTo(0);

    final Mapping mapping = diff.mapping;
    final String expectedMapping = "[(1,1), (2,2), (3,3), (4,4)]";
    assertEquals(expectedMapping, mapping.toString());

    final List<String> description = produceHumanFriendlyMapping(mapping, treeTwo, treeTwo);
    final List<String> expected =
        new ArrayList<>(
            asList(
                "No change for A (@1 and @1)",
                "No change for B (@2 and @2)",
                "No change for C (@3 and @3)",
                "No change for D (@4 and @4)"));
    assertEquals(expected, description);
  }

  @Test
  public void test() {
    TreeNode textA = new TreeNode("<text>");
    TreeNode dogA = new TreeNode("The dog's big eyes.");
    textA.addChild(dogA);
    Tree treeA = new Tree(textA);
    treeA.buildCaches();

    TreeNode textB = new TreeNode("<text>");
    TreeNode dogB1 = new TreeNode("The dog's ");
    TreeNode dogB2 = new TreeNode("big black ears");
    TreeNode dogB3 = new TreeNode("brown eyes");
    TreeNode dogB4 = new TreeNode(".");
    TreeNode addB = new TreeNode("<add>");
    TreeNode delB = new TreeNode("<del>");
    textB.addChild(dogB1);
    textB.addChild(delB);
    textB.addChild(addB);
    textB.addChild(dogB4);
    delB.addChild(dogB2);
    addB.addChild(dogB3);
    Tree treeB = new Tree(textB);
    treeB.buildCaches();

    final TreeMapping diff = computeDiff(treeA, treeB);
    LOG.info("diff = {}", diff);
    LOG.info("cost = {}", diff.cost);
    LOG.info("mapping = {}", diff.mapping);
    LOG.info("humanFriendly = {}", produceHumanFriendlyMapping(diff.mapping, treeA, treeB));
  }
}
