package nl.knaw.huygens.alexandria.compare;

import eu.interedition.collatex.dekker.astar.Cost;

public class MyCost extends Cost<MyCost> {
  private int cost;

  public MyCost(final int cost) {
    this.cost = cost;
  }

  @Override
  protected MyCost plus(final MyCost other) {
    return new MyCost(this.cost + other.cost);
  }

  @Override
  public int compareTo(final MyCost other) {
    return this.cost - other.cost;
  }
}
