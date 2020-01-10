package nl.knaw.huc.di.tag.treediff;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/*
 * Tests the functionality of Tree
 * */
public class TreeTest {

  private Tree treeOne;
  private Tree treeTwo;
  private Tree treeThree;

  @Before
  public void setUp() {
    TreeNode a_node = new TreeNode("A");
    TreeNode b_node = new TreeNode("B");
    a_node.addChild(b_node);
    TreeNode c_node = new TreeNode("C");
    b_node.addChild(c_node);
    treeOne = new Tree(a_node);
    treeOne.buildCaches();

    a_node = new TreeNode("A");
    b_node = new TreeNode("B");
    c_node = new TreeNode("C");
    TreeNode d_node = new TreeNode("D");
    a_node.addChild(b_node);
    a_node.addChild(c_node);
    c_node.addChild(d_node);
    treeTwo = new Tree(a_node);
    treeTwo.buildCaches();

    a_node = new TreeNode("A");
    b_node = new TreeNode("B");
    c_node = new TreeNode("C");
    d_node = new TreeNode("D");
    TreeNode e_node = new TreeNode("E");
    a_node.addChild(b_node);
    a_node.addChild(c_node);
    c_node.addChild(d_node);
    d_node.addChild(e_node);
    treeThree = new Tree(a_node);
    treeThree.buildCaches();
  }

  @Test
  public void testPreorderTraversalSuccess() {
    // Successfully produce the list of visited node labels
    TreeNodeVisitor visitor = new TreeNodeVisitor();
    treeOne.performPreorderTraversal(visitor);
    String[] expString = {"label: A, preorderPosition: 1", "label: B, preorderPosition: 2",
        "label: C, preorderPosition: 3"};
    List<String> expected = new ArrayList<>(Arrays.asList(expString));
    assertEquals(expected, visitor.traversal);

    visitor = new TreeNodeVisitor();
    treeTwo.performPreorderTraversal(visitor);
    String[] expString2 = {"label: A, preorderPosition: 1", "label: B, preorderPosition: 2",
        "label: C, preorderPosition: 3", "label: D, preorderPosition: 4"};
    expected = new ArrayList<>(Arrays.asList(expString2));
    assertEquals(expected.toString(), visitor.traversal.toString());
  }

  /*
   * Tests nodeAt method of Tree class Testing getting a node from the tree
   * by its preorder position
   */
  @Test
  public void testNodeAt() {
    // Tree 1
    assertEquals("A", treeOne.nodeAt(1).label());
    assertEquals("B", treeOne.nodeAt(2).label());
    assertEquals("C", treeOne.nodeAt(3).label());
    assertNull(treeOne.nodeAt(4));

    // Tree 2
    assertEquals("A", treeTwo.nodeAt(1).label());
    assertEquals("B", treeTwo.nodeAt(2).label());
    assertEquals("C", treeTwo.nodeAt(3).label());
    assertEquals("D", treeTwo.nodeAt(4).label());
    assertNull(treeTwo.nodeAt(5));
  }

  /*
   * Usual case when father exists
   */
  @Test
  public void testFatherOf_success() {
    assertEquals(3, treeTwo.fatherOf(4).preorderPosition());
    assertEquals(1, treeTwo.fatherOf(3).preorderPosition());
    assertEquals(1, treeTwo.fatherOf(2).preorderPosition());
  }

  /*
   * Father of root should be null
   */
  @Test
  public void testFatherOf_root() {
    assertNull(treeTwo.fatherOf(1));
  }

  /*
   * Raise error when father of a non-existing node requested
   */
  @Test(expected = IllegalArgumentException.class)
  public void testFatherOf_when_no_such_node() {
    treeTwo.fatherOf(5);
  }

  /*
   * Iterate from intermediate or leaf nodes upwards
   */
  @Test
  public void testAncestorIterator_success() {
    List<Integer> ancestor_preorder_positions = new ArrayList<>();
    for (Integer i : treeTwo.ancestorIterator(4)) {
      try {
        ancestor_preorder_positions.add(i);
      } catch (Exception e) {
        break;
      }
    }
    assertEquals(new ArrayList<>(Arrays.asList(4, 3, 1)), ancestor_preorder_positions);

    ancestor_preorder_positions = new ArrayList<>();
    for (Integer i : treeTwo.ancestorIterator(2)) {
      try {
        ancestor_preorder_positions.add(i);
      } catch (Exception e) {
        break;
      }
    }
    assertEquals(new ArrayList<>(Arrays.asList(2, 1)), ancestor_preorder_positions);
  }

  /*
   * Iterating from the root upwards
   */
  @Test
  public void testAncestorIterator_from_the_root() {
    List<Integer> ancestor_preorder_positions = new ArrayList<>();
    for (Integer i : treeTwo.ancestorIterator(1)) {
      try {
        ancestor_preorder_positions.add(i);
      } catch (Exception e) {
        break;
      }
    }
    assertEquals(new ArrayList<>(Collections.singletonList(1)), ancestor_preorder_positions);
  }

  /*
   * Trying to iterate from non-existing node is erroneous
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAncestorIterator_from_non_existing_position() {
    for (Integer i : treeTwo.ancestorIterator(100)) {
      System.out.println("Should not yield even once");
    }
  }

  /*
   * When there is a node between ancestor and descendant
   */
  @Test
  public void testChildOnPathFromDescendant_success() {
    TreeNode node = treeTwo.childOnPathFromDescendant(1, 4);
    assertEquals("C", node.label());
    assertEquals(3, node.preorderPosition());

    node = treeThree.childOnPathFromDescendant(1, 5);
    assertEquals("C", node.label());
    assertEquals(3, node.preorderPosition());

    node = treeThree.childOnPathFromDescendant(3, 5);
    assertEquals("D", node.label());
    assertEquals(4, node.preorderPosition());
  }

  /*
   * No node between ancestor and descendant
   */
  @Test(expected = IllegalArgumentException.class)
  public void testChildOnPathFromDescendant_when_no_such_node() {
    treeTwo.childOnPathFromDescendant(1, 1);
  }
}
