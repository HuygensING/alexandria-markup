package nl.knaw.huc.di.tag.treediff;

import java.util.ArrayList;
import java.util.List;

/*
 * Single node in a tree
 * */
public class TreeNode {
  private String label;
  private final List<TreeNode> children;
  private TreeNode father;
  private int preorderPosition;

  public TreeNode(String label) {
    // Label of the current node
    this.label = label;

    // List of TreeNode instances
    this.children = new ArrayList<>();

    // The father node of the current node
    this.father = null;

    // The cached position of the current node in the preorder traversal of
    // the tree.
    this.preorderPosition = 0;
  }

  /*
   * Sets the label (str) of this node
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /*
   * Returns the label (str) of this node
   */
  public String label() {
    return label;
  }

  /*
   * Sets the father of the node (TreeNode)
   */
  private void setFather(TreeNode node) {
    father = node;
  }

  /*
   * Returns the father node (TreeNode) of the node
   */
  public TreeNode father() {
    return father;
  }

  /*
   * Adds a new child (TreeNode) to the node
   */
  public void addChild(TreeNode node) {
    children.add(node);
    node.setFather(this);
  }

  /*
   * Yields an iteration over the children (TreeNode)
   */
  public Generator<TreeNode> children() {
    return new Generator<TreeNode>() {
      @Override
      protected void run() throws Exception {
        for (TreeNode node : children) {
          yield(node);
        }
      }
    };
  }

  /*
   * Sets the preorder traversal position (int)
   */
  public void setPreorderPosition(int new_position) {
    preorderPosition = new_position - 1;
  }

  /*
   * Returns the position (int) of the node in the preorder traversal
   */
  public int preorderPosition() {
    return preorderPosition + 1;
  }

  /*
   * Does a preorder traversal of the subtree rooted at this node The
   * parameter visitor should have a visit method accepting a TreeNode.
   */
  public void preorderTraversal(Visitor visitor) {
    visitor.visit(this);
    for (TreeNode child : children) {
      child.preorderTraversal(visitor);
    }
  }

  /*
   * Returns a representation (str) for this node for debugging
   */
  public String debug_string() {
    return String.format("label: %s, preorderPosition: %d", label, preorderPosition + 1);
  }

}
