package nl.knaw.huc.di.tag.schema;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import java.util.*;
import java.util.function.Consumer;

public class TreeNode<T> implements Iterable<TreeNode<T>> {

  T data;
  TreeNode<T> parent;
  List<TreeNode<T>> children = new ArrayList<>();

  public TreeNode(T data) {
    this.data = data;
    this.children = new LinkedList<>();
  }

  public TreeNode<T> addChild(T child) {
    TreeNode<T> childNode = new TreeNode<>(child);
    childNode.parent = this;
    this.children.add(childNode);
    return childNode;
  }

  public TreeNode<T> addChild(TreeNode<T> childNode) {
    childNode.parent = this;
    this.children.add(childNode);
    return childNode;
  }

  @Override
  public Iterator<TreeNode<T>> iterator() {
    return children.iterator();
  }

  @Override
  public void forEach(Consumer<? super TreeNode<T>> action) {
    children.forEach(action);
  }

  @Override
  public Spliterator<TreeNode<T>> spliterator() {
    return children.spliterator();
  }

  public T getData() {
    return data;
  }
}
