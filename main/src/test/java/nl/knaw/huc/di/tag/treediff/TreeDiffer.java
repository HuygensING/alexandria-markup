package nl.knaw.huc.di.tag.treediff;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

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
  private static Map<String, TreeDiff> computeE(Tree sourceTree, Tree targetTree) {
    Map<String, TreeDiff> treeDiffMapE = new HashMap<>();

    for (int i = 1; i < sourceTree.size() + 1; i++) {
      for (int j = 1; j < targetTree.size() + 1; j++) {
        for (Integer u : sourceTree.ancestorIterator(i)) {
          for (Integer s : sourceTree.ancestorIterator(u)) {
            for (Integer v : targetTree.ancestorIterator(j)) {
              for (Integer t : targetTree.ancestorIterator(v)) {
                String key = keyForE(s, u, i, t, v, j);

                if ((Objects.equals(s, u) && u == i) && (Objects.equals(t, v) && v == j)) {
                  Integer distance = r(sourceTree.nodeAt(i), targetTree.nodeAt(j));
                  Mapping mapping = new Mapping();
                  mapping.add(new ImmutablePair(i, j));
                  treeDiffMapE.put(key, new TreeDiff(distance, mapping));

                } else if ((Objects.equals(s, u) && u == i) || (t < v && v == j)) {
                  int f_j = targetTree.fatherOf(j).preorderPosition();
                  String dependentKey = keyForE(s, u, i, t, f_j, j - 1);
                  TreeDiff dependentTreeDiff = treeDiffMapE.get(dependentKey);
                  final Integer distance = dependentTreeDiff.distance + r(ALPHA, targetTree.nodeAt(j));
                  Mapping mapping = new Mapping();
                  mapping.addAll(dependentTreeDiff.mapping);
                  mapping.add(new ImmutablePair(ALPHA, j));
                  treeDiffMapE.put(key, new TreeDiff(distance, mapping));

                } else if ((s < u && u == i) || (Objects.equals(t, v) && v == j)) {
                  int f_i = sourceTree.fatherOf(i).preorderPosition();
                  String dependentKey = keyForE(s, f_i, i - 1, t, v, j);
                  TreeDiff dependentTreeDiff = treeDiffMapE.get(dependentKey);
                  final Integer distance = dependentTreeDiff.distance + r(sourceTree.nodeAt(i), ALPHA);
                  Mapping mapping = new Mapping();
                  mapping.addAll(dependentTreeDiff.mapping);
                  mapping.add(new ImmutablePair(i, ALPHA));
                  treeDiffMapE.put(key, new TreeDiff(distance, mapping));

                } else {
                  int x = sourceTree.childOnPathFromDescendant(u, i).preorderPosition();
                  int y = targetTree.childOnPathFromDescendant(v, j).preorderPosition();
                  String dependentKey1 = keyForE(s, x, i, t, v, j);
                  String dependentKey2 = keyForE(s, u, i, t, y, j);
                  String dependentKey3 = keyForE(s, u, x - 1, t, v, y - 1);
                  String dependentKey4 = keyForE(x, x, i, y, y, j);

                  TreeDiff dependentTreeDiff1 = treeDiffMapE.get(dependentKey1);
                  TreeDiff dependentTreeDiff2 = treeDiffMapE.get(dependentKey2);
                  TreeDiff dependentTreeDiff3 = treeDiffMapE.get(dependentKey3);
                  TreeDiff dependentTreeDiff4 = treeDiffMapE.get(dependentKey4);
                  final Integer distance = Math.min(
                      Math.min(dependentTreeDiff1.distance, dependentTreeDiff2.distance),
                      (dependentTreeDiff3.distance + dependentTreeDiff4.distance)
                  );

                  // Remember the mapping.
                  final Mapping mapping = new Mapping();
                  if (distance == dependentTreeDiff1.distance) {
                    mapping.addAll(dependentTreeDiff1.mapping);
                  } else if (distance == dependentTreeDiff1.distance) {
                    mapping.addAll(dependentTreeDiff2.mapping);
                  } else {
                    mapping.addAll(dependentTreeDiff3.mapping);
                    mapping.addAll(dependentTreeDiff4.mapping);
                  }
                  treeDiffMapE.put(key, new TreeDiff(distance, mapping));
                }
              }
            }
          }
        }
      }
    }
    return treeDiffMapE;
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
  private static Map<String, TreeDiff> computeMIN_M(Tree sourceTree, Tree targetTree, Map<String, TreeDiff> treeDiffMapE) {
    final Map<String, TreeDiff> treeDiffMapMinM = new HashMap<>();

    String key = keyForMIN_M(1, 1);
    Mapping mapping = new Mapping();
    mapping.add(new ImmutablePair(1, 1));
    treeDiffMapMinM.put(key, new TreeDiff(0, mapping));

    // This part is missing in the paper
    for (int j = 2; j < targetTree.size(); j++) {
      String key_1_j = keyForMIN_M(1, j);
      String key_1_j1 = keyForMIN_M(1, j - 1);
      TreeDiff treeDiff_1_j1 = treeDiffMapMinM.get(key_1_j1);
      int distance_1_j = treeDiff_1_j1.distance + r(ALPHA, targetTree.nodeAt(j));

      final Mapping mapping_1_j = new Mapping();
      mapping_1_j.addAll(treeDiff_1_j1.mapping);
      mapping_1_j.add(new ImmutablePair(ALPHA, j));
      treeDiffMapMinM.put(key_1_j, new TreeDiff(distance_1_j, mapping_1_j));
    }

    // This part is missing in the paper
    for (int i = 2; i < sourceTree.size(); i++) {
      String key_i_1 = keyForMIN_M(i, 1);
      String key_i1_1 = keyForMIN_M(i - 1, 1);
      TreeDiff treeDiff_i1_1 = treeDiffMapMinM.get(key_i1_1);
      int distance_i_1 = treeDiff_i1_1.distance + r(sourceTree.nodeAt(i), ALPHA);

      final Mapping mapping_i_1 = new Mapping();
      mapping_i_1.addAll(treeDiff_i1_1.mapping);
      mapping_i_1.add(new ImmutablePair(i, ALPHA));
      treeDiffMapMinM.put(key_i_1, new TreeDiff(distance_i_1, mapping_i_1));
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
            TreeDiff dependentTreeDiffE = treeDiffMapE.get(dependentKeyForE);
            TreeDiff dependentTreeDiffM = treeDiffMapMinM.get(dependentKeyForM);
            int temp = dependentTreeDiffM.distance + dependentTreeDiffE.distance
                - r(sourceTree.nodeAt(s), targetTree.nodeAt(t));
            distance_i_j = Math.min(temp, distance_i_j);
            if (temp == distance_i_j) {
              Set<Pair<Integer, Integer>> tempSet = new HashSet<>();
              tempSet.addAll(dependentTreeDiffM.mapping);
              tempSet.addAll(dependentTreeDiffE.mapping);
              mapping_i_j.addAll(tempSet);
            }
          }
        }
        distance_i_j = distance_i_j + r(sourceTree.nodeAt(i), targetTree.nodeAt(j));
        mapping_i_j.add(new ImmutablePair(i, j));
        treeDiffMapMinM.put(key_i_j, new TreeDiff(distance_i_j, mapping_i_j));
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
  private static Map<String, TreeDiff> computeD(Tree sourceTree, Tree targetTree, Map<String, TreeDiff> treeDiffMapMinM) {
    final Map<String, TreeDiff> treeDiffMapD = new HashMap<>();

    String key = keyForD(1, 1);
    Mapping mapping = new Mapping();
    mapping.add(new ImmutablePair(1, 1));
    treeDiffMapD.put(key, new TreeDiff(0, mapping));

    for (int i = 2; i < sourceTree.size() + 1; i++) {
      String key_i_1 = keyForD(i, 1);
      String key_i1_1 = keyForD(i - 1, 1);
      TreeDiff treeDiff_i1_1 = treeDiffMapD.get(key_i1_1);
      int distance_i_1 = treeDiff_i1_1.distance + r(sourceTree.nodeAt(i), ALPHA);

      Mapping mapping_i_1 = new Mapping();
      mapping_i_1.addAll(treeDiff_i1_1.mapping);
      mapping_i_1.add(new ImmutablePair(i, ALPHA));
      treeDiffMapD.put(key_i_1, new TreeDiff(distance_i_1, mapping_i_1));
    }
    for (int j = 2; j < targetTree.size() + 1; j++) {
      String key_1_j = keyForD(1, j);
      String key_1_j1 = keyForD(1, j - 1);
      TreeDiff treeDiff_1_j1 = treeDiffMapD.get(key_1_j1);
      int distance_1_j = treeDiff_1_j1.distance + r(ALPHA, sourceTree.nodeAt(j));

      Mapping mapping_1_j = new Mapping();
      mapping_1_j.addAll(treeDiff_1_j1.mapping);
      mapping_1_j.add(new ImmutablePair(ALPHA, j));
      treeDiffMapD.put(key_1_j, new TreeDiff(distance_1_j, mapping_1_j));
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        String keyD_i_j1 = keyForD(i, j - 1);
        TreeDiff treeDiff_i_j1 = treeDiffMapD.get(keyD_i_j1);
        int option1 = treeDiff_i_j1.distance + r(ALPHA, targetTree.nodeAt(j));

        String keyD_i1_j = keyForD(i - 1, j);
        TreeDiff treeDiff_i1_j = treeDiffMapD.get(keyD_i1_j);
        int option2 = treeDiff_i1_j.distance + r(sourceTree.nodeAt(i), ALPHA);

        String keyM_i_j = keyForMIN_M(i, j);
        TreeDiff treeDiffM_i_j = treeDiffMapMinM.get(keyM_i_j);
        int option3 = treeDiffM_i_j.distance;

        String keyD_i_j = keyForD(i, j);
        int distance_i_j = Math.min(option1, Math.min(option2, option3));

        Mapping mapping_i_j = new Mapping();
        if (distance_i_j == option1) {
          mapping_i_j.addAll(treeDiff_i_j1.mapping);
          mapping_i_j.add(new ImmutablePair(ALPHA, j));
        } else if (distance_i_j == option2) {
          mapping_i_j.addAll(treeDiff_i1_j.mapping);
          mapping_i_j.add(new ImmutablePair(i, ALPHA));
        } else {
          mapping_i_j.addAll(treeDiffM_i_j.mapping);
        }

        treeDiffMapD.put(keyD_i_j, new TreeDiff(distance_i_j, mapping_i_j));
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
  public static List<String> produceHumanFriendlyMapping(Mapping mapping, Tree sourceTree, Tree targetTree) {
    List<String> humanFriendlyMapping = new ArrayList<>();
    for (Pair pair : mapping) {
      Object i = pair.getLeft();
      Object j = pair.getRight();
      if (i == ALPHA) {
        TreeNode targetNode = targetTree.nodeAt((int) j);
        humanFriendlyMapping.add(format("Insert %s (@%d)",
            targetNode.label(),
            targetNode.preorderPosition()
        ));
      } else if (j == ALPHA) {
        TreeNode sourceNode = sourceTree.nodeAt((int) i);
        humanFriendlyMapping.add(format("Delete %s (@%d)",
            sourceNode.label(),
            sourceNode.preorderPosition()
        ));
      } else {
        TreeNode sourceNode = sourceTree.nodeAt((int) i);
        TreeNode targetNode = targetTree.nodeAt((int) j);
        if (Objects.equals(sourceNode.label(), targetNode.label())) {
          humanFriendlyMapping.add(format("No change for %s (@%d and @%d)",
              sourceNode.label(),
              sourceNode.preorderPosition(),
              targetNode.preorderPosition()
          ));
        } else {
          humanFriendlyMapping.add(format("Change from %s (@%d) to %s (@%d)",
              sourceNode.label(),
              sourceNode.preorderPosition(),
              targetNode.label(),
              targetNode.preorderPosition()
          ));
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
  public static TreeDiff computeDiff(Tree sourceTree, Tree targetTree) {
    Map<String, TreeDiff> treeDiffMapE = computeE(sourceTree, targetTree);
    Map<String, TreeDiff> treeDiffMapMinM = computeMIN_M(sourceTree, targetTree, treeDiffMapE);
    Map<String, TreeDiff> treeDiffMapD = computeD(sourceTree, targetTree, treeDiffMapMinM);

    String key = keyForD(sourceTree.size(), targetTree.size());
    TreeDiff treeDiff = treeDiffMapD.get(key);
    treeDiff.mapping.sort(MAPPING_PAIR_COMPARATOR);

    return treeDiff;
  }

  static Comparator<Pair<Integer, Integer>> MAPPING_PAIR_COMPARATOR = new Comparator<Pair<Integer, Integer>>() {
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

  static class TreeDiff {
    public Integer distance;
    public Mapping mapping;

    TreeDiff(Integer distance, Mapping mapping) {
      this.distance = distance;
      this.mapping = mapping;
    }
  }

  static class Mapping extends ArrayList<Pair<Integer, Integer>> {
  }

}
