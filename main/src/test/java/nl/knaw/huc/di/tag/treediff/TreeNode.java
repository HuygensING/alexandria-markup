package nl.knaw.huc.di.tag.treediff;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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
          provide(node);
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
  public String debugString() {
    return String.format("label: %s, preorderPosition: %d", label, preorderPosition + 1);
  }
}
