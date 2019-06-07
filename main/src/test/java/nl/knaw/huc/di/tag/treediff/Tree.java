package nl.knaw.huc.di.tag.treediff;

import java.util.ArrayList;
import java.util.List;

/*
 * Defines a tree structure of TreeNode elements
 */
class Tree {
  private final TreeNode root;
  private final List<TreeNode> preorderPositionToNode;
  private Visitor visitor;

  public Tree(TreeNode root) {
    this.root = root;
    preorderPositionToNode = new ArrayList<>();
  }

  /*
   * Returns the number of nodes in the tree
   */
  public int size() {
    return preorderPositionToNode.size();
  }

  /*
   * Builds the cached preorder positions of the nodes in the tree. Call this
   * method after the tree structure is finalized .
   */
  public void buildCaches() {
    visitor = new PreOrderMarkingVisitor(this);
    performPreorderTraversal(visitor);
  }

  /*
   * Performs a preorder traversal on the tree The visitor is used for taking
   * an action on the visited node ( the visitor must have a visit method)
   */
  public void performPreorderTraversal(Visitor visitor) {
    root.preorderTraversal(visitor);
  }

  /*
   * Does a preorder traversal and prints the node labels
   */
  public void printPreorderTraversal() {
    visitor = new DebugVisitor();
    performPreorderTraversal(visitor);
  }

  /*
   * Returns the node (TreeNode) at the given preorder position
   */
  public TreeNode nodeAt(int preorderPosition) {
    try {
      return preorderPositionToNode.get(preorderPosition - 1);
    } catch (Exception e) {
      return null;
    }
  }

  /*
   * Marks the node to be in the given preorder position in the tree
   */
  public void setNodeAt(int preorderPosition, TreeNode node) {
    preorderPositionToNode.add(preorderPosition - 1, node);
    node.setPreorderPosition(preorderPosition);// It handles "-1" inside
  }

  /*
   * Returns the father of the node at the given preorder position
   */
  public TreeNode fatherOf(int preorderPosition) {
    try {
      return preorderPositionToNode.get(preorderPosition - 1).father();
    } catch (Exception e) {
      throw new IllegalArgumentException("No node at the given position " + preorderPosition + ". ");
    }
  }

  /*
   * Produces iteration towards the root starting from the given position
   * Yields preorder positions of the nodes along the path from the node at
   * the given position to the root
   */
  public Generator<Integer> ancestorIterator(int startingPreorderPosition) {
    if (startingPreorderPosition > preorderPositionToNode.size()) {
      throw new IllegalArgumentException("No node at the given position " + startingPreorderPosition + ". ");
    }
    return new Generator<Integer>() {
      @Override
      protected void run() throws Exception {
        int preorderPosition = startingPreorderPosition;
        yield(preorderPosition);
        TreeNode node;
        while (true) {
          node = fatherOf(preorderPosition);
          if (node != null) {
            preorderPosition = node.preorderPosition();
            yield(preorderPosition);
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
  public TreeNode childOnPathFromDescendant(int parentPosition, int descendantPosition) {
    TreeNode currentChildNode = nodeAt(descendantPosition);
    TreeNode fatherNode = currentChildNode.father();
    if (fatherNode == null) {
      throw new IllegalArgumentException(
          "No father node for the given descendant position " + descendantPosition + ". ");
    }
    while (fatherNode.preorderPosition() != parentPosition) {
      currentChildNode = fatherNode;
      fatherNode = currentChildNode.father();
      if (fatherNode == null) {
        return null;
      }
    }
    return currentChildNode;
  }
}
