package treediff.util;

import java.util.ArrayList;
import java.util.List;

/*
 * Single node in a tree
 * */
public class TreeNode {
	private String label;
	private List<TreeNode> children;
	private TreeNode father;
	private int preorder_position;

	public TreeNode(String label) {
		// Label of the current node
		this.label = label;

		// List of TreeNode instances
		this.children = new ArrayList<TreeNode>();

		// The father node of the current node
		this.father = null;

		// The cached position of the current node in the preorder traversal of
		// the tree.
		this.preorder_position = 0;
	}

	/*
	 * Sets the label (str) of this node
	 */
	public void set_label(String label) {
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
	public void set_father(TreeNode node) {
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
	public void add_child(TreeNode node) {
		children.add(node);
		node.set_father(this);
	}

	/*
	 * Yields an iteration over the children (TreeNode)
	 */
	public Generator<TreeNode> children() {
		Generator<TreeNode> gen = new Generator<TreeNode>() {
			@Override
			protected void run() throws GeneratorExit, Exception {
				for (TreeNode node : children) {
					yield(node);
				}
			}
		};
		return gen;
	}

	/*
	 * Sets the preorder traversal position (int)
	 */
	public void set_preorder_position(int new_position) {
		preorder_position = new_position - 1;
	}

	/*
	 * Returns the position (int) of the node in the preorder traversal
	 */
	public int preorder_position() {
		return preorder_position + 1;
	}

	/*
	 * Does a preorder traversal of the subtree rooted at this node The
	 * parameter visitor should have a visit method accepting a TreeNode.
	 */
	public void preorder_traversal(Visitor visitor) {
		visitor.visit(this);
		for (TreeNode child : children) {
			child.preorder_traversal(visitor);
		}
	}

	/*
	 * Returns a representation (str) for this node for debugging
	 */
	public String debug_string() {
		return String.format("label: %s, preorder_position: %d", label, preorder_position + 1);
	}

}
