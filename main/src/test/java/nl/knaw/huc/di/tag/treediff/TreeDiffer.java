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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import static java.lang.String.format;

/* This file has the implementation of the algorithm described in
"The Tree-to-Tree Correction Problem" by Kuo-Chung Tai published
at the Journal of the ACM , 26(3):422-433, July 1979.

We follow the naming of the variables and functions from the paper
even though sometimes it may be against some Java conventions.
The algorithm is at section 5 in the paper. There is one
missing piece in the algorithm provided in the paper which is
MIN_M(i, 1) and MIN_M(1, j) values. We added the computation
of these in the implementation below.
*/
class TreeDiffer {

  private static final int INFINITE = Integer.MAX_VALUE;

  // Constant used for describing insertions or deletions
  private static final Integer ALPHA = null;

  /*
   * Returns the cost of transforming a to b
   *
   * @parameter a the label of the source node
   *
   * @parameter b the label of the target node
   *
   * @returns integer
   */
  private static int r(Object a, Object b) {
    if (a != ALPHA
        && b != ALPHA
        && Objects.equals(((TreeNode) a).label(), ((TreeNode) b).label())) // No change
    return 0;
    else // Insert, Delete, Change.
    return 1;
  }

  private static String keyForE(int s, int u, int i, int t, int v, int j) {
    return format("%d:%d:%d, %d:%d:%d", s, u, i, t, v, j);
  }

  /*
   * Returns the E mapping. Check the paper to understand what the mapping
   * mean.
   *
   * @parameter sourceTree the source tree (Tree)
   *
   * @parameter targetTree the target tree (Tree)
   *
   * @returns (dict, dict) The first dict is in the format {'i:j:k, p:q:r' =>
   * cost} where i, j, k, p, q, r are integers. The second dict is in the
   * format {'i:j:k, p:q:r' => mapping} where mapping is a list of (x, y)
   * pairs showing which node at the preorder position x in the source tree is
   * mapped to which node at the preorder position y in the target tree. If x
   * is ALPHA, then it shows the node at the preorder position y in the target
   * tree is inserted. If y is ALPHA, then it shows the node at the preorder
   * position x in the source tree is deleted.
   */
  private static Map<String, TreeMapping> computeE(Tree sourceTree, Tree targetTree) {
    Map<String, TreeMapping> treeDiffMapE = new HashMap<>();

    for (int i = 1; i < sourceTree.size() + 1; i++) {
      for (int j = 1; j < targetTree.size() + 1; j++) {
        for (Integer u : sourceTree.ancestorIterator(i)) {
          for (Integer s : sourceTree.ancestorIterator(u)) {
            for (Integer v : targetTree.ancestorIterator(j)) {
              for (Integer t : targetTree.ancestorIterator(v)) {
                String key = keyForE(s, u, i, t, v, j);

                final boolean b = Objects.equals(s, u) && u == i;
                final boolean b1 = Objects.equals(t, v) && v == j;
                if (b && b1) {
                  Integer distance = r(sourceTree.nodeAt(i), targetTree.nodeAt(j));
                  Mapping mapping = new Mapping();
                  mapping.add(pairOf(i, j));
                  treeDiffMapE.put(key, new TreeMapping(distance, mapping));

                } else if (b || (t < v && v == j)) {
                  int f_j = targetTree.fatherOf(j).preorderPosition();
                  String dependentKey = keyForE(s, u, i, t, f_j, j - 1);
                  TreeMapping dependentTreeMapping = treeDiffMapE.get(dependentKey);
                  final Integer distance =
                      dependentTreeMapping.cost + r(ALPHA, targetTree.nodeAt(j));
                  Mapping mapping = new Mapping();
                  mapping.addAll(dependentTreeMapping.mapping);
                  mapping.add(pairOf(ALPHA, j));
                  treeDiffMapE.put(key, new TreeMapping(distance, mapping));

                } else if ((s < u && u == i) || b1) {
                  int f_i = sourceTree.fatherOf(i).preorderPosition();
                  String dependentKey = keyForE(s, f_i, i - 1, t, v, j);
                  TreeMapping dependentTreeMapping = treeDiffMapE.get(dependentKey);
                  final Integer distance =
                      dependentTreeMapping.cost + r(sourceTree.nodeAt(i), ALPHA);
                  Mapping mapping = new Mapping();
                  mapping.addAll(dependentTreeMapping.mapping);
                  mapping.add(pairOf(i, ALPHA));
                  treeDiffMapE.put(key, new TreeMapping(distance, mapping));

                } else {
                  int x = sourceTree.childOnPathFromDescendant(u, i).preorderPosition();
                  int y = targetTree.childOnPathFromDescendant(v, j).preorderPosition();
                  String dependentKey1 = keyForE(s, x, i, t, v, j);
                  String dependentKey2 = keyForE(s, u, i, t, y, j);
                  String dependentKey3 = keyForE(s, u, x - 1, t, v, y - 1);
                  String dependentKey4 = keyForE(x, x, i, y, y, j);

                  TreeMapping dependentTreeMapping1 = treeDiffMapE.get(dependentKey1);
                  TreeMapping dependentTreeMapping2 = treeDiffMapE.get(dependentKey2);
                  TreeMapping dependentTreeMapping3 = treeDiffMapE.get(dependentKey3);
                  TreeMapping dependentTreeMapping4 = treeDiffMapE.get(dependentKey4);
                  final int distance =
                      Math.min(
                          Math.min(dependentTreeMapping1.cost, dependentTreeMapping2.cost),
                          (dependentTreeMapping3.cost + dependentTreeMapping4.cost));

                  // Remember the mapping.
                  final Mapping mapping = new Mapping();
                  if (distance == dependentTreeMapping1.cost) {
                    mapping.addAll(dependentTreeMapping1.mapping);
                  } else if (distance == dependentTreeMapping2.cost) {
                    mapping.addAll(dependentTreeMapping2.mapping);
                  } else {
                    mapping.addAll(dependentTreeMapping3.mapping);
                    mapping.addAll(dependentTreeMapping4.mapping);
                  }
                  treeDiffMapE.put(key, new TreeMapping(distance, mapping));
                }
              }
            }
          }
        }
      }
    }
    return treeDiffMapE;
  }

  private static ImmutablePair<Integer, Integer> pairOf(final Integer i, final Integer j) {
    return new ImmutablePair<>(i, j);
  }

  // Returns the key for MIN_M map
  private static String keyForMIN_M(int s, int t) {
    return format("%d:%d", s, t);
  }

  /*
   * Returns the MIN_M mapping. Check out the article to see what the mapping
   * means
   *
   * @parameter E computed by computeE (dict)
   *
   * @parameter sourceTree the source tree (Tree)
   *
   * @parameter targetTree the target tree (Tree)
   *
   * @returns (dict, dict) The first dict is the MIN_M map (key to cost). The
   * second dict is (key to list of integer pairs) the transformation mapping
   * where a pair (x, y) shows which node at the preorder position x in the
   * source tree is mapped to which node at the preorder position y in the
   * target tree. If x is ALPHA, then it shows the node at the preorder
   * position y in the target tree is inserted. If y is ALPHA, then it shows
   * the node at the preorder position x in the source tree is deleted.
   */
  private static Map<String, TreeMapping> computeMIN_M(
      Tree sourceTree, Tree targetTree, Map<String, TreeMapping> treeDiffMapE) {
    final Map<String, TreeMapping> treeDiffMapMinM = new HashMap<>();

    String key = keyForMIN_M(1, 1);
    Mapping mapping = new Mapping();
    mapping.add(pairOf(1, 1));
    treeDiffMapMinM.put(key, new TreeMapping(0, mapping));

    // This part is missing in the paper
    for (int j = 2; j < targetTree.size(); j++) {
      String key_1_j = keyForMIN_M(1, j);
      String key_1_j1 = keyForMIN_M(1, j - 1);
      TreeMapping treeMapping_1_j1 = treeDiffMapMinM.get(key_1_j1);
      int distance_1_j = treeMapping_1_j1.cost + r(ALPHA, targetTree.nodeAt(j));

      final Mapping mapping_1_j = new Mapping();
      mapping_1_j.addAll(treeMapping_1_j1.mapping);
      mapping_1_j.add(pairOf(ALPHA, j));
      treeDiffMapMinM.put(key_1_j, new TreeMapping(distance_1_j, mapping_1_j));
    }

    // This part is missing in the paper
    for (int i = 2; i < sourceTree.size(); i++) {
      String key_i_1 = keyForMIN_M(i, 1);
      String key_i1_1 = keyForMIN_M(i - 1, 1);
      TreeMapping treeMapping_i1_1 = treeDiffMapMinM.get(key_i1_1);
      int distance_i_1 = treeMapping_i1_1.cost + r(sourceTree.nodeAt(i), ALPHA);

      final Mapping mapping_i_1 = new Mapping();
      mapping_i_1.addAll(treeMapping_i1_1.mapping);
      mapping_i_1.add(pairOf(i, ALPHA));
      treeDiffMapMinM.put(key_i_1, new TreeMapping(distance_i_1, mapping_i_1));
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        String key_i_j = keyForMIN_M(i, j);
        int distance_i_j = INFINITE;
        Mapping mapping_i_j = new Mapping();
        int f_i = sourceTree.fatherOf(i).preorderPosition();
        int f_j = targetTree.fatherOf(j).preorderPosition();

        for (Integer s : sourceTree.ancestorIterator(f_i)) {
          for (Integer t : targetTree.ancestorIterator(f_j)) {
            String dependentKeyForE = keyForE(s, f_i, i - 1, t, f_j, j - 1);
            String dependentKeyForM = keyForMIN_M(s, t);
            TreeMapping dependentTreeMappingE = treeDiffMapE.get(dependentKeyForE);
            TreeMapping dependentTreeMappingM = treeDiffMapMinM.get(dependentKeyForM);
            int temp =
                dependentTreeMappingM.cost
                    + dependentTreeMappingE.cost
                    - r(sourceTree.nodeAt(s), targetTree.nodeAt(t));
            distance_i_j = Math.min(temp, distance_i_j);
            if (temp == distance_i_j) {
              Set<Pair<Integer, Integer>> tempSet = new HashSet<>();
              tempSet.addAll(dependentTreeMappingM.mapping);
              tempSet.addAll(dependentTreeMappingE.mapping);
              mapping_i_j.addAll(tempSet);
            }
          }
        }
        distance_i_j = distance_i_j + r(sourceTree.nodeAt(i), targetTree.nodeAt(j));
        mapping_i_j.add(pairOf(i, j));
        treeDiffMapMinM.put(key_i_j, new TreeMapping(distance_i_j, mapping_i_j));
      }
    }
    return treeDiffMapMinM;
  }

  // Returns the key for D map
  private static String keyForD(int i, int j) {
    return format("%d, %d", i, j);
  }

  /*
   * Returns the D mapping. Check out the article to see what the mapping means
   *
   * @parameter sourceTree the source tree (Tree)
   *
   * @parameter targetTree the target tree (Tree)
   *
   * @parameter MIN_M the MIN_M map (dict)
   *
   * @parameter mappingForM the transformation details for MIN_M
   *
   * @returns (dict, dict) The first dict is the D mapping (key to cost). The
   * second dict is (key to list of integer pairs) the transformation mapping
   * where a pair (x, y) shows which node at the preorder position x in the
   * source tree is mapped to which node at the preorder position y in the
   * target tree. If x is ALPHA, then it shows the node at the preorder
   * position y in the target tree is inserted. If y is ALPHA, then it shows
   * the node at the preorder position x in the source tree is deleted.
   */
  private static Map<String, TreeMapping> computeD(
      Tree sourceTree, Tree targetTree, Map<String, TreeMapping> treeDiffMapMinM) {
    final Map<String, TreeMapping> treeDiffMapD = new HashMap<>();

    String key = keyForD(1, 1);
    Mapping mapping = new Mapping();
    mapping.add(pairOf(1, 1));
    treeDiffMapD.put(key, new TreeMapping(0, mapping));

    for (int i = 2; i < sourceTree.size() + 1; i++) {
      String key_i_1 = keyForD(i, 1);
      String key_i1_1 = keyForD(i - 1, 1);
      TreeMapping treeMapping_i1_1 = treeDiffMapD.get(key_i1_1);
      int distance_i_1 = treeMapping_i1_1.cost + r(sourceTree.nodeAt(i), ALPHA);

      Mapping mapping_i_1 = new Mapping();
      mapping_i_1.addAll(treeMapping_i1_1.mapping);
      mapping_i_1.add(pairOf(i, ALPHA));
      treeDiffMapD.put(key_i_1, new TreeMapping(distance_i_1, mapping_i_1));
    }
    for (int j = 2; j < targetTree.size() + 1; j++) {
      String key_1_j = keyForD(1, j);
      String key_1_j1 = keyForD(1, j - 1);
      TreeMapping treeMapping_1_j1 = treeDiffMapD.get(key_1_j1);
      int distance_1_j = treeMapping_1_j1.cost + r(ALPHA, sourceTree.nodeAt(j));

      Mapping mapping_1_j = new Mapping();
      mapping_1_j.addAll(treeMapping_1_j1.mapping);
      mapping_1_j.add(pairOf(ALPHA, j));
      treeDiffMapD.put(key_1_j, new TreeMapping(distance_1_j, mapping_1_j));
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        String keyD_i_j1 = keyForD(i, j - 1);
        TreeMapping treeMapping_i_j1 = treeDiffMapD.get(keyD_i_j1);
        int option1 = treeMapping_i_j1.cost + r(ALPHA, targetTree.nodeAt(j));

        String keyD_i1_j = keyForD(i - 1, j);
        TreeMapping treeMapping_i1_j = treeDiffMapD.get(keyD_i1_j);
        int option2 = treeMapping_i1_j.cost + r(sourceTree.nodeAt(i), ALPHA);

        String keyM_i_j = keyForMIN_M(i, j);
        TreeMapping treeMappingM_i_j = treeDiffMapMinM.get(keyM_i_j);
        int option3 = treeMappingM_i_j.cost;

        String keyD_i_j = keyForD(i, j);
        int distance_i_j = Math.min(option1, Math.min(option2, option3));

        Mapping mapping_i_j = new Mapping();
        if (distance_i_j == option1) {
          mapping_i_j.addAll(treeMapping_i_j1.mapping);
          mapping_i_j.add(pairOf(ALPHA, j));
        } else if (distance_i_j == option2) {
          mapping_i_j.addAll(treeMapping_i1_j.mapping);
          mapping_i_j.add(pairOf(i, ALPHA));
        } else {
          mapping_i_j.addAll(treeMappingM_i_j.mapping);
        }

        treeDiffMapD.put(keyD_i_j, new TreeMapping(distance_i_j, mapping_i_j));
      }
    }
    return treeDiffMapD;
  }

  /*
   * Produces a list of human friendly descriptions for mapping between two trees
   * Example: [
   *   'No change for A (@1)',
   *   'Change from B (@2) to C (@3)',
   *   'No change for D (@3)',
   *   'Insert B (@2)'
   * ]
   *
   * @returns list of strings
   */
  public static List<String> produceHumanFriendlyMapping(
      Mapping mapping, Tree sourceTree, Tree targetTree) {
    List<String> humanFriendlyMapping = new ArrayList<>();
    for (Pair pair : mapping) {
      Object i = pair.getLeft();
      Object j = pair.getRight();
      if (i == ALPHA) {
        TreeNode targetNode = targetTree.nodeAt((int) j);
        humanFriendlyMapping.add(
            format("Insert %s (@%d)", targetNode.label(), targetNode.preorderPosition()));
      } else if (j == ALPHA) {
        TreeNode sourceNode = sourceTree.nodeAt((int) i);
        humanFriendlyMapping.add(
            format("Delete %s (@%d)", sourceNode.label(), sourceNode.preorderPosition()));
      } else {
        TreeNode sourceNode = sourceTree.nodeAt((int) i);
        TreeNode targetNode = targetTree.nodeAt((int) j);
        if (Objects.equals(sourceNode.label(), targetNode.label())) {
          humanFriendlyMapping.add(
              format(
                  "No change for %s (@%d and @%d)",
                  sourceNode.label(),
                  sourceNode.preorderPosition(),
                  targetNode.preorderPosition()));
        } else {
          humanFriendlyMapping.add(
              format(
                  "Change from %s (@%d) to %s (@%d)",
                  sourceNode.label(),
                  sourceNode.preorderPosition(),
                  targetNode.label(),
                  targetNode.preorderPosition()));
        }
      }
    }
    return humanFriendlyMapping;
  }

  /*
   * Returns the distance between the given trees and the list of pairs where
   * each pair (x, y) shows which node at the preorder position x in the
   * source tree is mapped to which node at the preorder position y in the
   * target tree. If x is ALPHA, then it shows the node at the preorder
   * position y in the target tree is inserted. If y is ALPHA, then it shows
   * the node at the preorder position x in the source tree is deleted.
   *
   * @parameter sourceTree the source tree (Tree)
   *
   * @parameter targetTree the target tree (Tree)
   *
   * @returns (int, [(int, int)])
   */
  public static TreeMapping computeDiff(Tree sourceTree, Tree targetTree) {
    Map<String, TreeMapping> treeDiffMapE = computeE(sourceTree, targetTree);
    Map<String, TreeMapping> treeDiffMapMinM = computeMIN_M(sourceTree, targetTree, treeDiffMapE);
    Map<String, TreeMapping> treeDiffMapD = computeD(sourceTree, targetTree, treeDiffMapMinM);

    String key = keyForD(sourceTree.size(), targetTree.size());
    TreeMapping treeMapping = treeDiffMapD.get(key);
    treeMapping.mapping.sort(MAPPING_PAIR_COMPARATOR);

    return treeMapping;
  }

  private static final Comparator<Pair<Integer, Integer>> MAPPING_PAIR_COMPARATOR =
      new Comparator<Pair<Integer, Integer>>() {
        @Override
        public int compare(Pair<Integer, Integer> p1, Pair<Integer, Integer> p2) {
          if (compareHelper(p1.getLeft(), p2.getLeft()) == 1) {
            return 1;
          } else if (compareHelper(p1.getLeft(), p2.getLeft()) == -1) {
            return -1;
          } else {
            return compareHelper(p1.getRight(), p2.getRight());
          }
        }

        int compareHelper(Object o1, Object o2) {
          if (o1 == ALPHA) {
            return 1;
          } else // if they are integer
          if (o2 == ALPHA) {
            return -1;
          } else return Integer.compare((int) o1, (int) o2);
        }
      };

  static class TreeMapping {
    public final int cost;
    public final Mapping mapping;

    TreeMapping(Integer cost, Mapping mapping) {
      this.cost = cost;
      this.mapping = mapping;
    }
  }

  static class Mapping extends ArrayList<Pair<Integer, Integer>> {}
}
