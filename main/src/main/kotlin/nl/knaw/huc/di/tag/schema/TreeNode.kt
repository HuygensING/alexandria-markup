package nl.knaw.huc.di.tag.schema

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

import java.util.*
import java.util.function.Consumer

class TreeNode<T>(var data: T) : Iterable<TreeNode<T>> {
    var parent: TreeNode<T>? = null

    @JvmField
    var children: MutableList<TreeNode<T>> = ArrayList()

    fun addChild(child: T): TreeNode<T> {
        val childNode = TreeNode(child)
        childNode.parent = this
        children.add(childNode)
        return childNode
    }

    fun addChild(childNode: TreeNode<T>): TreeNode<T> {
        childNode.parent = this
        children.add(childNode)
        return childNode
    }

    override fun iterator(): MutableIterator<TreeNode<T>> {
        return children.iterator()
    }

    override fun forEach(action: Consumer<in TreeNode<T>>) {
        children.forEach(action)
    }

    override fun spliterator(): Spliterator<TreeNode<T>> {
        return children.spliterator()
    }

    init {
        children = LinkedList()
    }
}
