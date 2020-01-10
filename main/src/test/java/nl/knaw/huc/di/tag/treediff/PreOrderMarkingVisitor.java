package nl.knaw.huc.di.tag.treediff;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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
/*
 * A visitor to be used in marking preorder positions of each tree node
 */
public class PreOrderMarkingVisitor implements Visitor {
  private int position;
  private final Tree tree;

  public PreOrderMarkingVisitor(Tree tree) {
    position = 1;
    this.tree = tree;
  }

  @Override
  public void visit(TreeNode node) {
    tree.setNodeAt(position, node);
    position += 1;
  }
}
