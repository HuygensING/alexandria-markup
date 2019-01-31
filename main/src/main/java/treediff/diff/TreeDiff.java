package treediff.diff;

import org.javatuples.Pair;
import treediff.util.Tree;
import treediff.util.TreeNode;

import java.util.*;

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
public class TreeDiff {

  private static final int INFINITE = Integer.MAX_VALUE;

  // Constant used for describing insertions or deletions
  private static final String ALPHA = "alpha";

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
    if (a != ALPHA && b != ALPHA && Objects.equals(((TreeNode) a).label(), ((TreeNode) b).label())) // #
      // No
      // change
      return 0;
    else // Insert, Delete, Change.
      return 1;
  }

  private static String keyForE(int s, int u, int i, int t, int v, int j) {
    return String.format("%d:%d:%d, %d:%d:%d", s, u, i, t, v, j);
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
  private static Pair computeE(Tree sourceTree, Tree targetTree) {
    Map<String, Integer> E = new HashMap<>();
    Map<String, ArrayList<Pair<Object, Object>>> mappingForE = new HashMap<>();

    for (int i = 1; i < sourceTree.size() + 1; i++) {
      for (int j = 1; j < targetTree.size() + 1; j++) {
        for (Integer u : sourceTree.ancestor_iterator(i)) {
          for (Integer s : sourceTree.ancestor_iterator(u)) {
            for (Integer v : targetTree.ancestor_iterator(j)) {
              for (Integer t : targetTree.ancestor_iterator(v)) {
                String key = keyForE(s, u, i, t, v, j);
                if ((Objects.equals(s, u) && u == i) && (Objects.equals(t, v) && v == j)) {
                  E.put(key, r(sourceTree.node_at(i), targetTree.node_at(j)));

                  ArrayList<Pair<Object, Object>> tempArr = new ArrayList<>();
                  tempArr.add(new Pair(i, j));
                  mappingForE.put(key, tempArr);
                } else if ((Objects.equals(s, u) && u == i) || (t < v && v == j)) {
                  int f_j = targetTree.father_of(j).preorder_position();
                  String dependentKey = keyForE(s, u, i, t, f_j, j - 1);

                  E.put(key, E.get(dependentKey) + r(ALPHA, targetTree.node_at(j)));

                  ArrayList<Pair<Object, Object>> tempArr = mappingForE.get(dependentKey);
                  tempArr.add(new Pair(ALPHA, j));
                  mappingForE.put(key, tempArr);
                } else if ((s < u && u == i) || (Objects.equals(t, v) && v == j)) {
                  int f_i = sourceTree.father_of(i).preorder_position();
                  String dependentKey = keyForE(s, f_i, i - 1, t, v, j);

                  E.put(key, E.get(dependentKey) + r(sourceTree.node_at(i), ALPHA));

                  ArrayList<Pair<Object, Object>> tempArr = mappingForE.get(dependentKey);
                  tempArr.add(new Pair(i, ALPHA));
                  mappingForE.put(key, tempArr);
                } else {
                  TreeNode xNode = sourceTree.child_on_path_from_descendant(u, i);
                  int x = xNode.preorder_position();
                  TreeNode yNode = targetTree.child_on_path_from_descendant(v, j);
                  int y = yNode.preorder_position();
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
                    ArrayList<Pair<Object, Object>> tempArr = mappingForE.get(dependentKey3);
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
    return new Pair(E, mappingForE);
  }

  // Returns the key for MIN_M map
  private static String keyForMIN_M(int s, int t) {
    return String.format("%d:%d", s, t);
  }

  /*
   * Returns the MIN_M mapping. Check out the article to see what the mapping
   * mean
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
  private static Pair computeMIN_M(Map<String, Integer> E, Map<String, ArrayList<Pair<Object, Object>>> mappingForE,
      Tree sourceTree, Tree targetTree) {
    Map<String, Integer> MIN_M = new HashMap<>();
    MIN_M.put(keyForMIN_M(1, 1), 0);

    Map<String, ArrayList<Pair<Object, Object>>> mappingForMinM = new HashMap<>();
    ArrayList<Pair<Object, Object>> tempArr = new ArrayList<>();
    tempArr.add(new Pair(1, 1));
    mappingForMinM.put(keyForMIN_M(1, 1), tempArr);

    // This part is missing in the paper
    for (int j = 2; j < targetTree.size(); j++) {
      MIN_M.put(keyForMIN_M(1, j), MIN_M.get(keyForMIN_M(1, j - 1)) + r(ALPHA, targetTree.node_at(j)));

      tempArr = mappingForMinM.get(keyForMIN_M(1, j - 1));
      tempArr.add(new Pair(ALPHA, j));
      mappingForMinM.put(keyForMIN_M(1, j), tempArr);
    }

    // This part is missing in the paper
    for (int i = 2; i < sourceTree.size(); i++) {
      MIN_M.put(keyForMIN_M(i, 1), MIN_M.get(keyForMIN_M(i - 1, 1)) + r(sourceTree.node_at(i), ALPHA));

      tempArr = mappingForMinM.get(keyForMIN_M(i - 1, 1));
      tempArr.add(new Pair(i, ALPHA));
      mappingForMinM.put(keyForMIN_M(i, 1), tempArr);
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        String keyForMIN_M_i_j = keyForMIN_M(i, j);
        MIN_M.put(keyForMIN_M_i_j, INFINITE);
        int f_i = sourceTree.father_of(i).preorder_position();
        int f_j = targetTree.father_of(j).preorder_position();

        for (Integer s : sourceTree.ancestor_iterator(f_i)) {
          for (Integer t : targetTree.ancestor_iterator(f_j)) {
            String dependentKeyForE = keyForE(s, f_i, i - 1, t, f_j, j - 1);
            String dependentKeyForM = keyForMIN_M(s, t);
            int temp = MIN_M.get(dependentKeyForM) + E.get(dependentKeyForE)
                - r(sourceTree.node_at(s), targetTree.node_at(t));
            MIN_M.put(keyForMIN_M_i_j, Math.min(temp, MIN_M.get(keyForMIN_M_i_j)));
            if (temp == MIN_M.get(keyForMIN_M_i_j)) {
              Set<Pair<Object, Object>> tempSet = new HashSet<>();
              tempSet.addAll(mappingForMinM.get(dependentKeyForM));
              tempSet.addAll(mappingForE.get(dependentKeyForE));
              tempArr = new ArrayList<>(tempSet);
              mappingForMinM.put(keyForMIN_M_i_j, tempArr);
            }
          }
        }
        MIN_M.put(keyForMIN_M_i_j,
            MIN_M.get(keyForMIN_M_i_j) + r(sourceTree.node_at(i), targetTree.node_at(j)));

        tempArr = mappingForMinM.get(keyForMIN_M_i_j);
        tempArr.add(new Pair(i, j));
        mappingForMinM.put(keyForMIN_M_i_j, tempArr);
      }
    }
    return new Pair(MIN_M, mappingForMinM);
  }

  // Returns the key for D map
  private static String keyForD(int i, int j) {
    return String.format("%d, %d", i, j);
  }

  /*
   * Returns the D mapping. Check out the article to see what the mapping mean
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
  private static Pair computeD(Tree sourceTree, Tree targetTree, Map<String, Integer> MIN_M,
      Map<String, ArrayList<Pair<Object, Object>>> mappingForMinM) {
    Map<String, Integer> D = new HashMap<>();
    D.put(keyForD(1, 1), 0);

    Map<String, ArrayList<Pair<Object, Object>>> mappingForD = new HashMap<>();
    ArrayList<Pair<Object, Object>> tempArr = new ArrayList<>();
    tempArr.add(new Pair(1, 1));
    mappingForD.put(keyForD(1, 1), tempArr);

    for (int i = 2; i < sourceTree.size() + 1; i++) {
      D.put(keyForD(i, 1), D.get(keyForD(i - 1, 1)) + r(sourceTree.node_at(i), ALPHA));

      tempArr = mappingForD.get(keyForD(i - 1, 1));
      tempArr.add(new Pair(i, ALPHA));
      mappingForD.put(keyForD(i, 1), tempArr);
    }
    for (int j = 2; j < targetTree.size() + 1; j++) {
      D.put(keyForD(1, j), D.get(keyForD(1, j - 1)) + r(ALPHA, sourceTree.node_at(j)));

      tempArr = mappingForD.get(keyForD(1, j - 1));
      tempArr.add(new Pair(ALPHA, j));
      mappingForD.put(keyForD(1, j), tempArr);
    }
    for (int i = 2; i < sourceTree.size() + 1; i++) {
      for (int j = 2; j < targetTree.size() + 1; j++) {
        int option1 = D.get(keyForD(i, j - 1)) + r(ALPHA, targetTree.node_at(j));
        int option2 = D.get(keyForD(i - 1, j)) + r(sourceTree.node_at(i), ALPHA);
        int option3 = MIN_M.get(keyForMIN_M(i, j));
        D.put(keyForD(i, j), Math.min(option1, Math.min(option2, option3)));

        if (D.get(keyForD(i, j)) == option1) {
          tempArr = mappingForD.get(keyForD(i, j - 1));
          tempArr.add(new Pair(ALPHA, j));
          mappingForD.put(keyForD(i, j), tempArr);
        } else if (D.get(keyForD(i, j)) == option2) {
          tempArr = mappingForD.get(keyForD(i - 1, j));
          tempArr.add(new Pair(i, ALPHA));
          mappingForD.put(keyForD(i, j), tempArr);
        } else {
          mappingForD.put(keyForD(i, j), mappingForMinM.get(keyForMIN_M(i, j)));
        }
      }
    }
    return new Pair(D, mappingForD);

  }

  /*
   * Produces a list of human friendly descriptions for mapping between two
   * trees Example: ['No change for A (@1)', 'Change from B (@2) to C (@3)',
   * 'No change for D (@3)', 'Insert B (@2)']
   *
   * @returns list of strings
   */
  public static ArrayList<String> produceHumanFriendlyMapping(ArrayList<Pair<Object, Object>> mapping,
      Tree sourceTree, Tree targetTree) {
    ArrayList<String> humanFriendlyMapping = new ArrayList<>();
    for (Pair pair : mapping) {
      Object i = pair.getValue0();
      Object j = pair.getValue1();
      if (i == ALPHA) {
        TreeNode targetNode = targetTree.node_at((int) j);
        humanFriendlyMapping
            .add(String.format("Insert %s (@%d)", targetNode.label(), targetNode.preorder_position()));
      } else if (j == ALPHA) {
        TreeNode sourceNode = sourceTree.node_at((int) i);
        humanFriendlyMapping
            .add(String.format("Delete %s (@%d)", sourceNode.label(), sourceNode.preorder_position()));
      } else {
        TreeNode sourceNode = sourceTree.node_at((int) i);
        TreeNode targetNode = targetTree.node_at((int) j);
        if (Objects.equals(sourceNode.label(), targetNode.label())) {
          humanFriendlyMapping.add(String.format("No change for %s (@%d and @%d)", sourceNode.label(),
              sourceNode.preorder_position(), targetNode.preorder_position()));
        } else {
          humanFriendlyMapping.add(String.format("Change from %s (@%d) to %s @%d)", sourceNode.label(),
              sourceNode.preorder_position(), targetNode.label(), targetNode.preorder_position()));
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
  public static Pair computeDiff(Tree sourceTree, Tree targetTree) {
    Pair<Object, Object> pair = computeE(sourceTree, targetTree);
    Map<String, Integer> E = (Map<String, Integer>) pair.getValue0();
    Map<String, ArrayList<Pair<Object, Object>>> mappingForE = (Map<String, ArrayList<Pair<Object, Object>>>) pair
        .getValue1();

    pair = computeMIN_M(E, mappingForE, sourceTree, targetTree);
    Map<String, Integer> MIN_M = (Map<String, Integer>) pair.getValue0();
    Map<String, ArrayList<Pair<Object, Object>>> mappingForMinM = (Map<String, ArrayList<Pair<Object, Object>>>) pair
        .getValue1();

    pair = computeD(sourceTree, targetTree, MIN_M, mappingForMinM);
    Map<String, Integer> D = (Map<String, Integer>) pair.getValue0();
    Map<String, ArrayList<Pair<Object, Object>>> mappingForD = (Map<String, ArrayList<Pair<Object, Object>>>) pair
        .getValue1();

    ArrayList<Pair<Object, Object>> mapping = mappingForD.get(keyForD(sourceTree.size(), targetTree.size()));
    mapping.sort(new Comparator<Pair<Object, Object>>() {
      @Override
      public int compare(Pair<Object, Object> p1, Pair<Object, Object> p2) {
        if (compareHelper(p1.getValue0(), p2.getValue0()) == 1) {
          return 1;
        } else if (compareHelper(p1.getValue0(), p2.getValue0()) == -1) {
          return -1;
        } else {
          return compareHelper(p1.getValue1(), p2.getValue1());
        }
      }

      int compareHelper(Object o1, Object o2) {
        if (o1 instanceof String) {
          return 1;
        } else // if they are integer
          if (o2 instanceof String) {
            return -1;
          } else return Integer.compare((int) o1, (int) o2);
      }

    });

    return new Pair(D.get(keyForD(sourceTree.size(), targetTree.size())), mapping);
  }
}
