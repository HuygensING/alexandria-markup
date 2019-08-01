package nl.knaw.huygens.alexandria.compare;

import eu.interedition.collatex.dekker.astar.Cost;

public class MyCost extends Cost<MyCost> {
  @Override
  protected MyCost plus(final MyCost other) {
    return null;
  }

  @Override
  public int compareTo(final MyCost o) {
    return 0;
  }
}
