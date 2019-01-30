package nl.knaw.huc.di.tag.treediff;

/*
 * A visitor to be used in printing each node's debug information
 */
public class DebugVisitor implements Visitor {
  @Override
  public void visit(TreeNode node) {
    System.out.println(node.debug_string());
  }
}