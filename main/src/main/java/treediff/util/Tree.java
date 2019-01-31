package treediff.util;

import java.util.ArrayList;
import java.util.List;

/*
 * Defines a tree structure of TreeNode elements
 */
public class Tree {
  private final TreeNode root;
  private final List<TreeNode> preorder_position_to_node;
  private Visitor visitor;

  public Tree(TreeNode root) {
    this.root = root;
    preorder_position_to_node = new ArrayList<>();
  }

  /*
   * Returns the number of nodes in the tree
   */
  public int size() {
    return preorder_position_to_node.size();
  }

  /*
   * Builds the cached preorder positions of the nodes in the tree. Call this
   * method after the tree structure is finalized .
   */
  public void build_caches() {
    visitor = new PreOrderMarkingVisitor(this);
    perform_preorder_traversal(visitor);
  }

  /*
   * Performs a preorder traversal on the tree The visitor is used for taking
   * an action on the visited node ( the visitor must have a visit method)
   */
  public void perform_preorder_traversal(Visitor visitor) {
    root.preorder_traversal(visitor);
  }

	/*
	 * Does a preorder traversal and prints the node labels
	 */
	public void print_preorder_traversal() {
		visitor = new DebugVisitor();
		perform_preorder_traversal(visitor);
	}

  /*
   * Returns the node (TreeNode) at the given preorder position
   */
  public TreeNode node_at(int preorder_position) {
    try {
      return preorder_position_to_node.get(preorder_position - 1);
    } catch (Exception e) {
      return null;
    }
  }

  /*
   * Marks the node to be in the given preorder position in the tree
   */
  public void set_node_at(int preorder_position, TreeNode node) {
    preorder_position_to_node.add(preorder_position - 1, node);
    node.set_preorder_position(preorder_position);// It handles "-1" inside
  }

  /*
   * Returns the father of the node at the given preorder position
   */
  public TreeNode father_of(int preorder_position) {
    try {
      return preorder_position_to_node.get(preorder_position - 1).father();
    } catch (Exception e) {
      throw new IllegalArgumentException("No node at the given position " + preorder_position + ". ");
    }
  }

  /*
   * Produces iteration towards the root starting from the given position
   * Yields preorder positions of the nodes along the path from the node at
   * the given position to the root
   */
  public Generator<Integer> ancestor_iterator(int starting_preorder_position) {
    if (starting_preorder_position > preorder_position_to_node.size()) {
      throw new IllegalArgumentException("No node at the given position " + starting_preorder_position + ". ");
    }
    return new Generator<Integer>() {
      @Override
      protected void run() throws Exception {
        int preorder_position = starting_preorder_position;
        yield(preorder_position);
        TreeNode node;
        while (true) {
          node = father_of(preorder_position);
          if (node != null) {
            preorder_position = node.preorder_position();
            yield(preorder_position);
          } else
            break;
        }
      }
    };
  }

  /*
   * Finds a child node between the parent and the descendant. Returns the
   * child of a node which is on the path from a descendant of the node to the
   * node (there can only be one such child). Returns null if no such node.
   */
  public TreeNode child_on_path_from_descendant(int parent_position, int descendant_position) {
    TreeNode current_child_node = node_at(descendant_position);
    TreeNode father_node = current_child_node.father();
    if (father_node == null) {
      throw new IllegalArgumentException(
          "No father node for the given descendant position " + descendant_position + ". ");
    }
    while (father_node.preorder_position() != parent_position) {
      current_child_node = father_node;
      father_node = current_child_node.father();
      if (father_node == null) {
        return null;
      }
    }
    return current_child_node;
  }
}
