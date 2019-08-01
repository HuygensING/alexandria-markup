package nl.knaw.huygens.alexandria.compare;

import eu.interedition.collatex.dekker.astar.AstarAlgorithm;

public class MyAStar extends AstarAlgorithm<MyNode, MyCost> {
  @Override
  protected boolean isGoal(final MyNode node) {
    return false;
  }

  @Override
  protected Iterable<MyNode> neighborNodes(final MyNode current) {
    return null;
  }

  @Override
  protected MyCost heuristicCostEstimate(final MyNode node) {
    return null;
  }

  @Override
  protected MyCost distBetween(final MyNode current, final MyNode neighbor) {
    return null;
  }
}
