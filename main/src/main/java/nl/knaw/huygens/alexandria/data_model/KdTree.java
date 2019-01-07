package nl.knaw.huygens.alexandria.data_model;

/*
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

/**
 * A k-d tree (short for k-dimensional tree) is a space-partitioning data
 * structure for organizing points in a k-dimensional space. k-d trees are a
 * useful data structure for several applications, such as searches involving a
 * multidimensional search key (e.g. range searches and nearest neighbor
 * searches). k-d trees are a special case of binary space partitioning trees.
 * <p>
 * <p>
 * based on https://github.com/phishman3579/java-algorithms-implementation/blob/master/src/com/jwetherell/algorithms/data_structures/KdTree.java by Justin Wetherell
 * <phishman3579@gmail.com></a>
 *
 * @see <a href="http://en.wikipedia.org/wiki/K-d_tree">K-d_tree (Wikipedia)</a>
 */
public class KdTree<T extends IndexPoint> implements Iterable<T> {

  private final int k = 2;

  private KdNode root = null;

  private static final Comparator<IndexPoint> TEXTNODE_INDEX_COMPARATOR = new Comparator<IndexPoint>() {
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(IndexPoint o1, IndexPoint o2) {
      return Integer.compare(o1.getTextNodeIndex(), o2.getTextNodeIndex());
    }
  };

  private static final Comparator<IndexPoint> MARKUP_INDEX_COMPARATOR = new Comparator<IndexPoint>() {
    /**
     * {@inheritDoc}
     */
    @Override
    public int compare(IndexPoint o1, IndexPoint o2) {
      return Integer.compare(o1.getMarkupIndex(), o2.getMarkupIndex());
    }
  };

  private static final int TEXTNODE_AXIS = 0;
  private static final int MARKUP_AXIS = 1;

  /**
   * Default constructor.
   */
  public KdTree() {
  }

  /**
   * Constructor for creating a more balanced tree. It uses the
   * "median of points" algorithm.
   *
   * @param list of IndexPoints.
   */
  public KdTree(List<IndexPoint> list) {
    super();
    root = createNode(list, k, 0);
  }

  /**
   * Constructor for creating a more balanced tree. It uses the
   * "median of points" algorithm.
   *
   * @param list of IndexPoints.
   * @param k    of the tree.
   */
  public KdTree(List<IndexPoint> list, int k) {
    super();
    root = createNode(list, k, 0);
  }

  /**
   * Creates node from list of IndexPoints.
   *
   * @param list  of IndexPoints.
   * @param k     of the tree.
   * @param depth depth of the node.
   * @return node created.
   */
  private static KdNode createNode(List<IndexPoint> list, int k, int depth) {
    if (list == null || list.size() == 0)
      return null;

    int axis = depth % k;
    switch (axis) {
      case TEXTNODE_AXIS:
        list.sort(TEXTNODE_INDEX_COMPARATOR);
        break;
      case MARKUP_AXIS:
        list.sort(MARKUP_INDEX_COMPARATOR);
        break;
    }

    KdNode node = null;
    List<IndexPoint> less = new ArrayList<>(list.size());
    List<IndexPoint> more = new ArrayList<>(list.size());
    if (list.size() > 0) {
      int medianIndex = list.size() / 2;
      node = new KdNode(list.get(medianIndex), k, depth);
      // Process list to see where each non-median point lies
      for (int i = 0; i < list.size(); i++) {
        if (i == medianIndex)
          continue;
        IndexPoint p = list.get(i);
        // Cannot assume points before the median are less since they could be equal
        if (KdNode.compareTo(depth, k, p, node.id) <= 0) {
          less.add(p);
        } else {
          more.add(p);
        }
      }

      if ((medianIndex - 1 >= 0) && less.size() > 0) {
        node.lesser = createNode(less, k, depth + 1);
        node.lesser.parent = node;
      }

      if ((medianIndex <= list.size() - 1) && more.size() > 0) {
        node.greater = createNode(more, k, depth + 1);
        node.greater.parent = node;
      }
    }

    return node;
  }

  public KdNode getRoot() {
    return root;
  }

  /**
   * Adds getDocumentId to the tree. Tree can contain multiple equal values.
   *
   * @param value T to add to the tree.
   * @return True if successfully added to tree.
   */
  public boolean add(T value) {
    if (value == null)
      return false;

    if (root == null) {
      root = new KdNode(value);
      return true;
    }

    KdNode node = root;
    while (true) {
      if (KdNode.compareTo(node.depth, node.k, value, node.id) <= 0) {
        // Lesser
        if (node.lesser == null) {
          KdNode newNode = new KdNode(value, k, node.depth + 1);
          newNode.parent = node;
          node.lesser = newNode;
          break;
        }
        node = node.lesser;
      } else {
        // Greater
        if (node.greater == null) {
          KdNode newNode = new KdNode(value, k, node.depth + 1);
          newNode.parent = node;
          node.greater = newNode;
          break;
        }
        node = node.greater;
      }
    }

    return true;
  }

  /**
   * Does the tree contain the getDocumentId.
   *
   * @param value T to locate in the tree.
   * @return True if tree contains getDocumentId.
   */
  public boolean contains(T value) {
    if (value == null || root == null)
      return false;

    KdNode node = this.getNode(value);
    return (node != null);
  }

  /**
   * Locates T in the tree.
   *
   * @param value to search for.
   * @return KdNode or NULL if not found
   */
  private <T extends IndexPoint> KdNode getNode(T value) {
    if (root == null || value == null)
      return null;

    KdNode node = root;
    while (true) {
      if (node.id.equals(value)) {
        return node;
      } else if (KdNode.compareTo(node.depth, node.k, value, node.id) <= 0) {
        // Lesser
        if (node.lesser == null) {
          return null;
        }
        node = node.lesser;
      } else {
        // Greater
        if (node.greater == null) {
          return null;
        }
        node = node.greater;
      }
    }
  }

  /**
   * Removes first occurrence of getDocumentId in the tree.
   *
   * @param value T to remove from the tree.
   * @return True if getDocumentId was removed from the tree.
   */
  public boolean remove(T value) {
    if (value == null || root == null)
      return false;

    KdNode node = this.getNode(value);
    if (node == null)
      return false;

    KdNode parent = node.parent;
    if (parent != null) {
      if (node.equals(parent.lesser)) {
        List<IndexPoint> nodes = getTree(node);
        if (nodes.size() > 0) {
          parent.lesser = createNode(nodes, node.k, node.depth);
          if (parent.lesser != null) {
            parent.lesser.parent = parent;
          }
        } else {
          parent.lesser = null;
        }
      } else {
        List<IndexPoint> nodes = getTree(node);
        if (nodes.size() > 0) {
          parent.greater = createNode(nodes, node.k, node.depth);
          if (parent.greater != null) {
            parent.greater.parent = parent;
          }
        } else {
          parent.greater = null;
        }
      }
    } else {
      // root
      List<IndexPoint> nodes = getTree(node);
      if (nodes.size() > 0)
        root = createNode(nodes, node.k, node.depth);
      else
        root = null;
    }

    return true;
  }

  /**
   * Gets the (sub) tree rooted at root.
   *
   * @param root of tree to get nodes for.
   * @return points in (sub) tree, not including root.
   */
  private static List<IndexPoint> getTree(KdNode root) {
    List<IndexPoint> list = new ArrayList<>();
    if (root == null)
      return list;

    if (root.lesser != null) {
      list.add(root.lesser.id);
      list.addAll(getTree(root.lesser));
    }
    if (root.greater != null) {
      list.add(root.greater.id);
      list.addAll(getTree(root.greater));
    }

    return list;
  }

  // /**
  // * Searches the K nearest neighbor.
  // *
  // * @param K Number of neighbors to retrieve. Can return more than K, if
  // * last nodes are equal distances.
  // * @param getDocumentId to find neighbors of.
  // * @return Collection of T neighbors.
  // */
  // @SuppressWarnings("unchecked")
  // public <T extends IndexPoint> Collection<T> nearestNeighbourSearch(int K, T getDocumentId) {
  // if (getDocumentId == null || root == null)
  // return Collections.EMPTY_LIST;
  //
  // // Map used for results
  // TreeSet<KdNode> results = new TreeSet<KdNode>(new EuclideanComparator(getDocumentId));
  //
  // // Find the closest leaf node
  // KdNode prev = null;
  // KdNode node = root;
  // while (node != null) {
  // if (KdNode.compareTo(node.depth, node.k, getDocumentId, node.id) <= 0) {
  // // Lesser
  // prev = node;
  // node = node.lesser;
  // } else {
  // // Greater
  // prev = node;
  // node = node.greater;
  // }
  // }
  // KdNode leaf = prev;
  //
  // if (leaf != null) {
  // // Used to not re-examine nodes
  // Set<KdNode> examined = new HashSet<KdNode>();
  //
  // // Go up the tree, looking for better solutions
  // node = leaf;
  // while (node != null) {
  // // Search node
  // searchNode(getDocumentId, node, K, results, examined);
  // node = node.parent;
  // }
  // }
  //
  // // Load up the collection of the results
  // Collection<T> collection = new ArrayList<T>(K);
  // for (KdNode kdNode : results)
  // collection.add((T) kdNode.id);
  // return collection;
  // }

  // private static final <T extends IndexPoint> void searchNode(T getDocumentId, KdNode node, int K, TreeSet<KdNode> results, Set<KdNode> examined) {
  // examined.add(node);
  //
  // // Search node
  // KdNode lastNode = null;
  // Double lastDistance = Double.MAX_VALUE;
  // if (results.size() > 0) {
  // lastNode = results.last();
  //// lastDistance = lastNode.id.euclideanDistance(getDocumentId);
  // }
  //// Double nodeDistance = node.id.euclideanDistance(getDocumentId);
  // if (nodeDistance.compareTo(lastDistance) < 0) {
  // if (results.size() == K && lastNode != null)
  // results.remove(lastNode);
  // results.add(node);
  // } else if (nodeDistance.equals(lastDistance)) {
  // results.add(node);
  // } else if (results.size() < K) {
  // results.add(node);
  // }
  // lastNode = results.last();
  //// lastDistance = lastNode.id.euclideanDistance(getDocumentId);
  //
  // int axis = node.depth % node.k;
  // KdNode lesser = node.lesser;
  // KdNode greater = node.greater;
  //
  // // Search children branches, if axis aligned distance is less than
  // // current distance
  // if (lesser != null && !examined.contains(lesser)) {
  // examined.add(lesser);
  //
  // double nodePoint = Double.MIN_VALUE;
  // double valuePlusDistance = Double.MIN_VALUE;
  // if (axis == TEXTNODE_AXIS) {
  // nodePoint = node.id.getTextNodeIndex();
  // valuePlusDistance = getDocumentId.getTextNodeIndex() - lastDistance;
  // } else if (axis == MARKUP_AXIS) {
  // nodePoint = node.id.getMarkupIndex();
  // valuePlusDistance = getDocumentId.getMarkupIndex() - lastDistance;
  //// } else {
  //// nodePoint = node.id.z;
  //// valuePlusDistance = getDocumentId.z - lastDistance;
  // }
  // boolean lineIntersectsCube = ((valuePlusDistance <= nodePoint) ? true : false);
  //
  // // Continue down lesser branch
  // if (lineIntersectsCube)
  // searchNode(getDocumentId, lesser, K, results, examined);
  // }
  // if (greater != null && !examined.contains(greater)) {
  // examined.add(greater);
  //
  // double nodePoint = Double.MIN_VALUE;
  // double valuePlusDistance = Double.MIN_VALUE;
  // if (axis == TEXTNODE_AXIS) {
  // nodePoint = node.id.getTextNodeIndex();
  // valuePlusDistance = getDocumentId.getTextNodeIndex() + lastDistance;
  // } else if (axis == MARKUP_AXIS) {
  // nodePoint = node.id.getMarkupIndex();
  // valuePlusDistance = getDocumentId.getMarkupIndex() + lastDistance;
  //// } else {
  //// nodePoint = node.id.z;
  //// valuePlusDistance = getDocumentId.z + lastDistance;
  // }
  // boolean lineIntersectsCube = ((valuePlusDistance >= nodePoint) ? true : false);
  //
  // // Continue down greater branch
  // if (lineIntersectsCube)
  // searchNode(getDocumentId, greater, K, results, examined);
  // }
  // }

  /**
   * Adds, in a specified queue, a given node and its related nodes (lesser, greater).
   *
   * @param node    Node to check. May be null.
   * @param results Queue containing all found entries. Must not be null.
   */
  @SuppressWarnings("unchecked")
  private static <T extends IndexPoint> void search(final KdNode node, final Deque<T> results) {
    if (node != null) {
      results.add((T) node.id);
      search(node.greater, results);
      search(node.lesser, results);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return TreePrinter.getString(this);
  }

  // protected static class EuclideanComparator implements Comparator<KdNode> {
  //
  // private final IndexPoint point;
  //
  // public EuclideanComparator(IndexPoint point) {
  // this.point = point;
  // }
  //
  //// /**
  //// * {@inheritDoc}
  //// */
  //// @Override
  //// public int compare(KdNode o1, KdNode o2) {
  //// Double d1 = point.euclideanDistance(o1.id);
  //// Double d2 = point.euclideanDistance(o2.id);
  //// if (d1.compareTo(d2) < 0)
  //// return -1;
  //// else if (d2.compareTo(d1) < 0)
  //// return 1;
  //// return o1.id.compareTo(o2.id);
  //// }
  // }

  /**
   * Searches all entries from the first to the last entry.
   *
   * @return Iterator
   * allowing to iterate through a collection containing all found entries.
   */
  public Iterator<T> iterator() {
    final Deque<T> results = new ArrayDeque<>();
    search(root, results);
    return results.iterator();
  }

  /**
   * Searches all entries from the last to the first entry.
   *
   * @return Iterator
   * allowing to iterate through a collection containing all found entries.
   */
  public Iterator<T> reverse_iterator() {
    final Deque<T> results = new ArrayDeque<>();
    search(root, results);
    return results.descendingIterator();
  }

  public List<IndexPoint> indexpointsForTextNode(int textNodeIndex) {
    List<IndexPoint> list = new ArrayList<>();
    KdNode node = root;
    addMarkupIndexPointsToList(list, node, textNodeIndex);
    return list;
  }

  private void addMarkupIndexPointsToList(List<IndexPoint> list, KdNode node, int textNodeIndex) {
    if (node == null) {
      return;
    }
    IndexPoint ip = node.id;
    if (ip.getTextNodeIndex() == textNodeIndex) {
      list.add(ip);
    }
    if (node.depth == TEXTNODE_AXIS) {
      int currentTextNodeIndex = ip.getTextNodeIndex();
      if (textNodeIndex < currentTextNodeIndex) {
        addMarkupIndexPointsToList(list, node.getLesser(), textNodeIndex);

      } else if (textNodeIndex == currentTextNodeIndex) {
        addMarkupIndexPointsToList(list, node.getLesser(), textNodeIndex);
        addMarkupIndexPointsToList(list, node.getGreater(), textNodeIndex);

      } else {
        addMarkupIndexPointsToList(list, node.getGreater(), textNodeIndex);
      }

    } else {
      addMarkupIndexPointsToList(list, node.getLesser(), textNodeIndex);
      addMarkupIndexPointsToList(list, node.getGreater(), textNodeIndex);
    }

  }

  public List<IndexPoint> indexpointsForMarkup(int markupIndex) {
    List<IndexPoint> list = new ArrayList<>();
    KdNode node = root;
    addTextNodeIndexPointsToList(list, node, markupIndex);
    return list;
  }

  private void addTextNodeIndexPointsToList(List<IndexPoint> list, KdNode node, int markupIndex) {
    if (node == null) {
      return;
    }
    IndexPoint ip = node.id;
    if (ip.getMarkupIndex() == markupIndex) {
      list.add(ip);
    }
    if (node.depth == MARKUP_AXIS) {
      int currentMarkupIndex = ip.getMarkupIndex();
      if (markupIndex < currentMarkupIndex) {
        addTextNodeIndexPointsToList(list, node.getLesser(), markupIndex);

      } else if (markupIndex == currentMarkupIndex) {
        addTextNodeIndexPointsToList(list, node.getLesser(), markupIndex);
        addTextNodeIndexPointsToList(list, node.getGreater(), markupIndex);

      } else {
        addTextNodeIndexPointsToList(list, node.getGreater(), markupIndex);
      }

    } else {
      addTextNodeIndexPointsToList(list, node.getLesser(), markupIndex);
      addTextNodeIndexPointsToList(list, node.getGreater(), markupIndex);
    }

  }

  public static class KdNode implements Comparable<KdNode> {

    private final IndexPoint id;
    private final int k;
    private final int depth;

    private KdNode parent = null;
    private KdNode lesser = null;
    private KdNode greater = null;

    KdNode(IndexPoint id) {
      this.id = id;
      this.k = 3;
      this.depth = 0;
    }

    KdNode(IndexPoint id, int k, int depth) {
      this.id = id;
      this.k = k;
      this.depth = depth;
    }

    static int compareTo(int depth, int k, IndexPoint o1, IndexPoint o2) {
      int axis = depth % k;
      if (axis == TEXTNODE_AXIS)
        return TEXTNODE_INDEX_COMPARATOR.compare(o1, o2);
      // if (axis == MARKUP_AXIS)
      return MARKUP_INDEX_COMPARATOR.compare(o1, o2);
      // return Z_COMPARATOR.compare(o1, o2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
      return 31 * (this.k + this.depth + this.id.hashCode());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
      if (obj == null)
        return false;
      if (!(obj instanceof KdNode))
        return false;

      KdNode kdNode = (KdNode) obj;
      return this.compareTo(kdNode) == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(KdNode o) {
      return compareTo(depth, k, this.id, o.id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
      return "k=" + k + " depth=" + depth + " id=" + id.toString();
    }

    public IndexPoint getContent() {
      return id;
    }

    public KdNode getLesser() {
      return lesser;
    }

    public KdNode getGreater() {
      return greater;
    }

  }

  static class TreePrinter {

    static <T extends IndexPoint> String getString(KdTree<T> tree) {
      if (tree.root == null)
        return "Tree has no nodes.";
      return getString(tree.root, "", true);
    }

    private static String getString(KdNode node, String prefix, boolean isTail) {
      StringBuilder builder = new StringBuilder();
      if (node.parent != null) {
        boolean isRight = node.parent.greater != null && node.id.equals(node.parent.greater.id);
        String side = isRight ? "right" : "left";
        builder.append(prefix).append(isTail ? "└── " : "├── ").append("[").append(side).append("] ").append("depth=").append(node.depth).append(" id=").append(node.id).append("\n");
      } else {
        builder.append(prefix).append(isTail ? "└── " : "├── ").append("depth=").append(node.depth).append(" id=").append(node.id).append("\n");
      }
      List<KdNode> children = null;
      if (node.lesser != null || node.greater != null) {
        children = new ArrayList<>(2);
        if (node.lesser != null)
          children.add(node.lesser);
        if (node.greater != null)
          children.add(node.greater);
      }
      if (children != null) {
        for (int i = 0; i < children.size() - 1; i++) {
          builder.append(getString(children.get(i), prefix + (isTail ? "    " : "│   "), false));
        }
        if (children.size() >= 1) {
          builder.append(getString(children.get(children.size() - 1), prefix + (isTail ? "    " : "│   "), true));
        }
      }

      return builder.toString();
    }
  }
}
