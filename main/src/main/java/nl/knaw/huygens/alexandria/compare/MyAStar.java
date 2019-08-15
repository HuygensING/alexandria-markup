package nl.knaw.huygens.alexandria.compare;

import eu.interedition.collatex.dekker.astar.AstarAlgorithm;
import nl.knaw.huygens.alexandria.compare.TAGComparison2.MarkupInfo;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.lang.Math.abs;
import static java.util.stream.Collectors.toList;

public class MyAStar extends AstarAlgorithm<MyNode, MyCost> {

  private final List<MarkupInfo> markupInfoListA;
  private final List<MarkupInfo> markupInfoListB;
  private final int maxA;
  private final int maxB;

  public MyAStar(List<MarkupInfo> markupInfoListA, List<MarkupInfo> markupInfoListB) {
    this.markupInfoListA = markupInfoListA;
    this.maxA = markupInfoListA.size() - 1;
    this.markupInfoListB = markupInfoListB;
    this.maxB = markupInfoListB.size() - 1;
    System.out.println("maxA=" + maxA + ",maxB=" + maxB);
  }

  @Override
  protected boolean isGoal(final MyNode node) {
    boolean goal = node.indexA == maxA && node.indexB == maxB;
    System.out.println("(" + node.indexA + "," + node.indexB + "):" + goal);
    return goal;
  }

  @Override
  protected Iterable<MyNode> neighborNodes(final MyNode current) {
    final List<MyNode> neighborNodes = new ArrayList<>();

    if (current.indexA < maxA) {
      final MyNode neighborNodeA = nodeAt(current.indexA + 1, current.indexB, current.matches);
      neighborNodes.add(neighborNodeA);
    }

    if (current.indexA < maxA && current.indexB < maxB) {
      final MyNode neighborNodeAB = nodeAt(current.indexA + 1, current.indexB + 1, current.matches);
      neighborNodes.add(neighborNodeAB);
    }

    if (current.indexB < maxB) {
      final MyNode neighborNodeB = nodeAt(current.indexA, current.indexB + 1, current.matches);
      neighborNodes.add(neighborNodeB);
    }

    return neighborNodes;
  }

  private MyNode nodeAt(final Integer indexA, final Integer indexB, final List<Pair<Integer, Integer>> currentMatches) {
    boolean isMatch = isMatch(markupInfoListA.get(indexA), markupInfoListB.get(indexB));
    final ArrayList matches = new ArrayList(currentMatches);
    if (isMatch) {
      matches.add(new ImmutablePair(indexA, indexB));
    }
    return new MyNode(indexA, indexB, matches, isMatch);
  }

  private boolean isMatch(final MarkupInfo markupInfoA, final MarkupInfo markupInfoB) {
    return Objects.equals(markupInfoA.getMarkup().getTag(), markupInfoB.getMarkup().getTag());
  }

  @Override
  protected MyCost heuristicCostEstimate(final MyNode node) {
    final int cost = node.isMatch ? -1 : 0;
    return new MyCost(cost);
  }

  @Override
  protected MyCost distBetween(final MyNode current, final MyNode neighbor) {
    int dist = abs(current.matches.size() - neighbor.matches.size());
    return new MyCost(dist);
  }

  public List<Pair<MarkupInfo, MarkupInfo>> matches() {
    final MyNode startNode = nodeAt(0, 0, new ArrayList<>());
    final MyCost startCost = new MyCost(0);
    List<MyNode> nodePath = aStar(startNode, startCost);
    final List<Pair<MarkupInfo, MarkupInfo>> matches = nodePath.get(0).matches.stream()
        .map(pair -> new ImmutablePair<>(markupInfoListA.get(pair.getLeft()), markupInfoListB.get(pair.getRight())))
        .collect(toList());
    return matches;
  }
}
