package nl.knaw.huc.di.tag.treediff;

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
public class TestTree {

  Tree tree_one, tree_two, tree_three;

  @Before
  public void setUp() {
    TreeNode a_node = new TreeNode("A");
    TreeNode b_node = new TreeNode("B");
    a_node.add_child(b_node);
    TreeNode c_node = new TreeNode("C");
    b_node.add_child(c_node);
    tree_one = new Tree(a_node);
    tree_one.build_caches();

    a_node = new TreeNode("A");
    b_node = new TreeNode("B");
    c_node = new TreeNode("C");
    TreeNode d_node = new TreeNode("D");
    a_node.add_child(b_node);
    a_node.add_child(c_node);
    c_node.add_child(d_node);
    tree_two = new Tree(a_node);
    tree_two.build_caches();

    a_node = new TreeNode("A");
    b_node = new TreeNode("B");
    c_node = new TreeNode("C");
    d_node = new TreeNode("D");
    TreeNode e_node = new TreeNode("E");
    a_node.add_child(b_node);
    a_node.add_child(c_node);
    c_node.add_child(d_node);
    d_node.add_child(e_node);
    tree_three = new Tree(a_node);
    tree_three.build_caches();
  }

  @Test
  public void testPreorderTraversal_success() {
    // Successfully produce the list of visited node labels
    TreeNodeVisitor visitor = new TreeNodeVisitor();
    tree_one.perform_preorder_traversal(visitor);
    String[] expString = {"label: A, preorder_position: 1", "label: B, preorder_position: 2",
        "label: C, preorder_position: 3"};
    List<String> expected = new ArrayList<>(Arrays.asList(expString));
    assertEquals(expected, visitor.traversal);

    visitor = new TreeNodeVisitor();
    tree_two.perform_preorder_traversal(visitor);
    String[] expString2 = {"label: A, preorder_position: 1", "label: B, preorder_position: 2",
        "label: C, preorder_position: 3", "label: D, preorder_position: 4"};
    expected = new ArrayList<>(Arrays.asList(expString2));
    assertEquals(expected.toString(), visitor.traversal.toString());
  }

  /*
   * Tests node_at method of Tree class Testing getting a node from the tree
   * by its preorder position
   */
  @Test
  public void testNodeAt() {
    // Tree 1
    assertEquals("A", tree_one.node_at(1).label());
    assertEquals("B", tree_one.node_at(2).label());
    assertEquals("C", tree_one.node_at(3).label());
    assertNull(tree_one.node_at(4));

    // Tree 2
    assertEquals("A", tree_two.node_at(1).label());
    assertEquals("B", tree_two.node_at(2).label());
    assertEquals("C", tree_two.node_at(3).label());
    assertEquals("D", tree_two.node_at(4).label());
    assertNull(tree_two.node_at(5));
  }

  /*
   * Usual case when father exists
   */
  @Test
  public void testFatherOf_success() {
    assertEquals(3, tree_two.father_of(4).preorder_position());
    assertEquals(1, tree_two.father_of(3).preorder_position());
    assertEquals(1, tree_two.father_of(2).preorder_position());
  }

  /*
   * Father of root should be null
   */
  @Test
  public void testFatherOf_root() {
    assertNull(tree_two.father_of(1));
  }

  /*
   * Raise error when father of a non-existing node requested
   */
  @Test(expected = IllegalArgumentException.class)
  public void testFatherOf_when_no_such_node() {
    tree_two.father_of(5);
  }

  /*
   * Iterate from intermediate or leaf nodes upwards
   */
  @Test
  public void testAncestorIterator_success() {
    List<Integer> ancestor_preorder_positions = new ArrayList<>();
    for (Integer i : tree_two.ancestor_iterator(4)) {
      try {
        ancestor_preorder_positions.add(i);
      } catch (Exception e) {
        break;
      }
    }
    assertEquals(new ArrayList<>(Arrays.asList(4, 3, 1)), ancestor_preorder_positions);

    ancestor_preorder_positions = new ArrayList<>();
    for (Integer i : tree_two.ancestor_iterator(2)) {
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
    for (Integer i : tree_two.ancestor_iterator(1)) {
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
    for (Integer i : tree_two.ancestor_iterator(100)) {
      System.out.println("Should not yield even once");
    }
  }

  /*
   * When there is a node between ancestor and descendant
   */
  @Test
  public void testChildOnPathFromDescendant_success() {
    TreeNode node = tree_two.child_on_path_from_descendant(1, 4);
    assertEquals("C", node.label());
    assertEquals(3, node.preorder_position());

    node = tree_three.child_on_path_from_descendant(1, 5);
    assertEquals("C", node.label());
    assertEquals(3, node.preorder_position());

    node = tree_three.child_on_path_from_descendant(3, 5);
    assertEquals("D", node.label());
    assertEquals(4, node.preorder_position());
  }

  /*
   * No node between ancestor and descendant
   */
  @Test(expected = IllegalArgumentException.class)
  public void testChildOnPathFromDescendant_when_no_such_node() {
    tree_two.child_on_path_from_descendant(1, 1);
  }
}
