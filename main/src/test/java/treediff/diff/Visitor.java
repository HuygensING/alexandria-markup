package treediff.diff;

import java.util.ArrayList;
import java.util.List;

import treediff.util.*;

public class Visitor implements treediff.util.Visitor {
	List<String> traversal;

	/*
	 * Builds an array of node labels it visits
	 */
	public Visitor() {
		traversal = new ArrayList<String>();
	}

	/*
	 * Records the label of the node visited
	 */
	public void visit(TreeNode node) {
		traversal.add(node.debug_string());
	}
}
