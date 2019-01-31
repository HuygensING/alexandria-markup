package treediff.util;

/*
 * A visitor interface to apply the visitor pattern for navigating the tree
 * */
public interface Visitor {

  void visit(TreeNode node);

}
