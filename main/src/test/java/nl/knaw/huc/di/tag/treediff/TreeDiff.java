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
class TreeDiff {

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
  private static Pair<Map<String, Integer>, Map<String, Mapping>> computeE(Tree sourceTree, Tree targetTree) {
    Map<String, Integer> E = new HashMap<>();
    Map<String, Mapping> mappingForE = new HashMap<>();

    for (int i = 1; i < sourceTree.size() + 1; i++) {
      for (int j = 1; j < targetTree.size() + 1; j++) {
        for (Integer u : sourceTree.ancestorIterator(i)) {
          for (Integer s : sourceTree.ancestorIterator(u)) {
            for (Integer v : targetTree.ancestorIterator(j)) {
              for (Integer t : targetTree.ancestorIterator(v)) {
                String key = keyForE(s, u, i, t, v, j);
                if ((Objects.equals(s, u) && u == i) && (Objects.equals(t, v) && v == j)) {
                  E.put(key, r(sourceTree.nodeAt(i), targetTree.nodeAt(j)));

                  Mapping tempArr = new Mapping();
                  tempArr.add(new ImmutablePair(i, j));
                  mappingForE.put(key, tempArr);
                } else if ((Objects.equals(s, u) && u == i) || (t < v && v == j)) {
                  int f_j = targetTree.fatherOf(j).preorderPosition();
                  String dependentKey = keyForE(s, u, i, t, f_j, j - 1);

                  E.put(key, E.get(dependentKey) + r(ALPHA, targetTree.nodeAt(j)));

                  Mapping tempArr = mappingForE.get(dependentKey);
                  tempArr.add(new ImmutablePair(ALPHA, j));
                  mappingForE.put(key, tempArr);
                } else if ((s < u && u == i) || (Objects.equals(t, v) && v == j)) {
                  int f_i = sourceTree.fatherOf(i).preorderPosition();
                  String dependentKey = keyForE(s, f_i, i - 1, t, v, j);

                  E.put(key, E.get(dependentKey) + r(sourceTree.nodeAt(i), ALPHA));

                  Mapping tempArr = mappingForE.get(dependentKey);
                  tempArr.add(new ImmutablePair(i, ALPHA));
                  mappingForE.put(key, tempArr);
                } else {
                  TreeNode xNode = sourceTree.childOnPathFromDescendant(u, i);
                  int x = xNode.preorderPosition();
                  TreeNode yNode = targetTree.childOnPathFromDescendant(v, j);
                  int y = yNode.preorderPosition();
                  String dependentKey1 = keyForE(s, x, i, t, v, j);
                  String dependentKey2 = keyForE(s, u, i, t, y, j);
                  String dependentKey3 = keyForE(s, u, x - 1, t, v, y - 1);
                  String dependentKey4 = keyForE(x, x, i, y, y, j);

                  E.put(key, Math.min(Math.min(E.get(dependentKey1), E.get(dependentKey2)),
                      (E.get(dependentKey3) + E.get(dependentKey4))));
                  // Remember the mapping.
                  if (Objects.equals(E.get(key), E.get(dependentKey1))) {
                    mappingForE.put(key, mappingForE.get(dependentKey1));
                  } else if (Objects.equals(E.get(key), E.get(dependentKey2))) {
                    mappingForE.put(key, mappingForE.get(dependentKey2));
                  } else {
                    Mapping tempArr = mappingForE.get(dependentKey3);
                    tempArr.addAll(mappingForE.get(dependentKey4));
                    mappingForE.put(key, tempArr);
                  }
                }
              }
            }
          }
        }
      }
    }
    return new ImmutablePair(E, mappingForE);
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
   * the node at the preorder position x in the souce tree is deleted.
   */
  private static Pair computeMIN_M(Map<String, Integer> E, Map<String, Mapping> mappingForE,
      Tree sourceTree, Tree targetTree) {
    Map<String, Integer> MIN_M = new HashMap<>();
    MIN_M.put(keyForMIN_M(1, 1), 0);

    Map<String, Mapping> mappingForMinM = new HashMap<>();
    Mapping tempArr = new Mapping();
    tempArr.add(new ImmutablePair(1, 1));
    mappingForMinM.put(keyForMIN_M(1, 1), tempArr);

    // This part is missing in the paper
    for (int j = 2; j < targetTree.size(); j++) {
      MIN_M.put(keyForMIN_M(1, j), MIN_M.get(keyForMIN_M(1, j - 1)) + r(ALPHA, targetTree.nodeAt(j)));

      tempArr = mappingForMinM.get(keyForMIN_M(1, j - 1));
      tempArr.add(new ImmutablePair(ALPHA, j));
      mappingForMinM.put(keyForMIN_M(1, j), tempArr);
    }

    // This part is missing in the paper
    for (int i = 2; i < sourceTree.size(); i++) {
      MIN_M.put(keyForMIN_M(i, 1), MIN_M.get(keyForMIN_M(i - 1, 1)) + r(sourceTree.nodeAt(i), ALPHA));

      tempArr = mappingForMinM.get(keyForMIN_M(i - 1, 1));
      tempArr.add(new ImmutablePair(i, ALPHA));
      mappingForMinM.put(keyForMIN_M(i, 1), tempArr);
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        String keyForMIN_M_i_j = keyForMIN_M(i, j);
        MIN_M.put(keyForMIN_M_i_j, INFINITE);
        int f_i = sourceTree.fatherOf(i).preorderPosition();
        int f_j = targetTree.fatherOf(j).preorderPosition();

        for (Integer s : sourceTree.ancestorIterator(f_i)) {
          for (Integer t : targetTree.ancestorIterator(f_j)) {
            String dependentKeyForE = keyForE(s, f_i, i - 1, t, f_j, j - 1);
            String dependentKeyForM = keyForMIN_M(s, t);
            int temp = MIN_M.get(dependentKeyForM) + E.get(dependentKeyForE)
                - r(sourceTree.nodeAt(s), targetTree.nodeAt(t));
            MIN_M.put(keyForMIN_M_i_j, Math.min(temp, MIN_M.get(keyForMIN_M_i_j)));
            if (temp == MIN_M.get(keyForMIN_M_i_j)) {
              Set<Pair<Integer, Integer>> tempSet = new HashSet<>();
              tempSet.addAll(mappingForMinM.get(dependentKeyForM));
              tempSet.addAll(mappingForE.get(dependentKeyForE));
              tempArr = new Mapping();
              mappingForMinM.put(keyForMIN_M_i_j, tempArr);
            }
          }
        }
        MIN_M.put(keyForMIN_M_i_j,
            MIN_M.get(keyForMIN_M_i_j) + r(sourceTree.nodeAt(i), targetTree.nodeAt(j)));

        tempArr = mappingForMinM.get(keyForMIN_M_i_j);
        tempArr.add(new ImmutablePair(i, j));
        mappingForMinM.put(keyForMIN_M_i_j, tempArr);
      }
    }
    return new ImmutablePair(MIN_M, mappingForMinM);
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
   * the node at the preorder position x in the souce tree is deleted.
   */
  private static Pair computeD(Tree sourceTree, Tree targetTree, Map<String, Integer> MIN_M,
      Map<String, Mapping> mappingForMinM) {
    Map<String, Integer> D = new HashMap<>();
    D.put(keyForD(1, 1), 0);

    Map<String, Mapping> mappingForD = new HashMap<>();
    Mapping tempArr = new Mapping();
    tempArr.add(new ImmutablePair(1, 1));
    mappingForD.put(keyForD(1, 1), tempArr);

    for (int i = 2; i < sourceTree.size() + 1; i++) {
      D.put(keyForD(i, 1), D.get(keyForD(i - 1, 1)) + r(sourceTree.nodeAt(i), ALPHA));

      tempArr = mappingForD.get(keyForD(i - 1, 1));
      tempArr.add(new ImmutablePair(i, ALPHA));
      mappingForD.put(keyForD(i, 1), tempArr);
    }
    for (int j = 2; j < targetTree.size() + 1; j++) {
      D.put(keyForD(1, j), D.get(keyForD(1, j - 1)) + r(ALPHA, sourceTree.nodeAt(j)));

      tempArr = mappingForD.get(keyForD(1, j - 1));
      tempArr.add(new ImmutablePair(ALPHA, j));
      mappingForD.put(keyForD(1, j), tempArr);
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        int option1 = D.get(keyForD(i, j - 1)) + r(ALPHA, targetTree.nodeAt(j));
        int option2 = D.get(keyForD(i - 1, j)) + r(sourceTree.nodeAt(i), ALPHA);
        int option3 = MIN_M.get(keyForMIN_M(i, j));
        D.put(keyForD(i, j), Math.min(option1, Math.min(option2, option3)));

        if (D.get(keyForD(i, j)) == option1) {
          tempArr = mappingForD.get(keyForD(i, j - 1));
          tempArr.add(new ImmutablePair(ALPHA, j));
          mappingForD.put(keyForD(i, j), tempArr);
        } else if (D.get(keyForD(i, j)) == option2) {
          tempArr = mappingForD.get(keyForD(i - 1, j));
          tempArr.add(new ImmutablePair(i, ALPHA));
          mappingForD.put(keyForD(i, j), tempArr);
        } else {
          mappingForD.put(keyForD(i, j), mappingForMinM.get(keyForMIN_M(i, j)));
        }
      }
    }
    return new ImmutablePair(D, mappingForD);

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
  public static List<String> produceHumanFriendlyMapping(Mapping mapping,
      Tree sourceTree, Tree targetTree) {
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
          humanFriendlyMapping.add(format("Change from %s (@%d) to %s @%d)",
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
  public static TreeDiffResult computeDiff(Tree sourceTree, Tree targetTree) {
    Pair<Map<String, Integer>, Map<String, Mapping>> pair = computeE(sourceTree, targetTree);
    Map<String, Integer> E = pair.getLeft();
    Map<String, Mapping> mappingForE = pair.getRight();

    pair = computeMIN_M(E, mappingForE, sourceTree, targetTree);
    Map<String, Integer> MIN_M = pair.getLeft();
    Map<String, Mapping> mappingForMinM = pair.getRight();

    pair = computeD(sourceTree, targetTree, MIN_M, mappingForMinM);
    Map<String, Integer> D = pair.getLeft();
    Map<String, Mapping> mappingForD = pair.getRight();

    String key = keyForD(sourceTree.size(), targetTree.size());
    Mapping mapping = mappingForD.get(key);
    mapping.sort(MAPPING_PAIR_COMPARATOR);

    Integer distance = D.get(key);
    return new TreeDiffResult(distance, mapping);
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

  static class TreeDiffResult {
    public Integer distance;
    public Mapping mapping;

    TreeDiffResult(Integer distance, Mapping mapping) {
      this.distance = distance;
      this.mapping = mapping;
    }
  }

  static class Mapping extends ArrayList<Pair<Integer, Integer>> {
  }

}
