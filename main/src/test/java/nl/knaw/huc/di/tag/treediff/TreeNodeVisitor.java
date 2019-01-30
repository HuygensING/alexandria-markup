package nl.knaw.huc.di.tag.treediff;

import java.util.ArrayList;
import java.util.List;

public class TreeNodeVisitor implements Visitor {

  final List<String> traversal;

  /*
   * Builds an array of node labels it visits
   */
  public TreeNodeVisitor() {
    traversal = new ArrayList<>();
  }

  /*
   * Records the label of the node visited
   */
  public void visit(TreeNode node) {
    traversal.add(node.debug_string());
  }
}