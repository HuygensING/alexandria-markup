package nl.knaw.huygens.alexandria.compare;

import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class MyNode {

  final int indexA;
  final int indexB;
  final List<Pair<Integer, Integer>> matches;
  boolean isMatch;

  public MyNode(int indexA, int indexB, List<Pair<Integer, Integer>> matches, boolean isMatch) {
    this.indexA = indexA;
    this.indexB = indexB;
    this.matches = matches;
    this.isMatch = isMatch;
  }

}