package nl.knaw.huygens.alexandria.compare;

/*-
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

import com.google.common.base.Stopwatch;
import eu.interedition.collatex.dekker.astar.AstarAlgorithm;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class MyAStar extends AstarAlgorithm<QuantumMatchList, LostPotential> {
  private static final Logger LOG = LoggerFactory.getLogger(MyAStar.class);

  private static final Comparator<Pair<Integer, Integer>> BY_LEFT = Comparator.comparing(Pair::getLeft);
  private static final Comparator<Pair<Integer, Integer>> BY_RIGHT = Comparator.comparing(Pair::getRight);
  private static final Comparator<Pair<Integer, Integer>> BY_ASCENDING_A_INDEX = BY_LEFT.thenComparing(BY_RIGHT);
  private static final Comparator<Pair<Integer, Integer>> BY_ASCENDING_B_INDEX = BY_RIGHT.thenComparing(BY_LEFT);

  private final List<Pair<Integer, Integer>> potentialMatchesSortedOnA;
  private final List<Pair<Integer, Integer>> potentialMatchesSortedOnB;
  private final int maxPotential;
  private List<Pair<Integer, Integer>> matches;

  public MyAStar(List<Pair<Integer, Integer>> allPotentialMatches) {
    maxPotential = allPotentialMatches.size();
    potentialMatchesSortedOnA = new ArrayList<>(allPotentialMatches);
    potentialMatchesSortedOnA.sort(BY_ASCENDING_A_INDEX);

    potentialMatchesSortedOnB = new ArrayList<>(allPotentialMatches);
    potentialMatchesSortedOnB.sort(BY_ASCENDING_B_INDEX);

    QuantumMatchList startNode = new QuantumMatchList(Collections.EMPTY_LIST, new ArrayList<>(allPotentialMatches));
    LostPotential startCost = new LostPotential(0);
    Stopwatch sw = Stopwatch.createStarted();
    List<QuantumMatchList> winningPath = aStar(startNode, startCost);
    sw.stop();
    LOG.debug("aStar took {} ms", sw.elapsed(TimeUnit.MILLISECONDS));
    QuantumMatchList winningGoal = winningPath.get(winningPath.size() - 1);
    matches = winningGoal.chosenMatches;
  }

  @Override
  protected boolean isGoal(final QuantumMatchList node) {
    return node.potentialMatches.isEmpty();
  }

  @Override
  protected Iterable<QuantumMatchList> neighborNodes(final QuantumMatchList current) {
    final Set<QuantumMatchList> neighborNodes = new HashSet<>();

    Pair<Integer, Integer> firstPotentialMatch1 = getFirstPotentialMatch(this.potentialMatchesSortedOnA, current);
    addNeighborNodes(current, neighborNodes, firstPotentialMatch1);

    Pair<Integer, Integer> firstPotentialMatch2 = getFirstPotentialMatch(this.potentialMatchesSortedOnB, current);
    if (!firstPotentialMatch1.equals(firstPotentialMatch2)) {
      addNeighborNodes(current, neighborNodes, firstPotentialMatch2);
    }

    return neighborNodes;
  }

  private void addNeighborNodes(final QuantumMatchList current, final Set<QuantumMatchList> neighborNodes, final Pair<Integer, Integer> firstPotentialMatch) {
    QuantumMatchList quantumMatchSet1 = current.chooseMatch(firstPotentialMatch);
    QuantumMatchList quantumMatchSet2 = current.discardMatch(firstPotentialMatch);
    neighborNodes.add(quantumMatchSet1);
    neighborNodes.add(quantumMatchSet2);
  }

  private Pair<Integer, Integer> getFirstPotentialMatch(final List<Pair<Integer, Integer>> matches, final QuantumMatchList current) {
    List<Pair<Integer, Integer>> potentialMatches = new ArrayList<>(matches);
    potentialMatches.retainAll(current.getPotentialMatches());
    return potentialMatches.get(0);
  }

  @Override
  protected LostPotential heuristicCostEstimate(final QuantumMatchList node) {
    return new LostPotential(maxPotential - node.totalSize());
  }

  @Override
  protected LostPotential distBetween(final QuantumMatchList current, final QuantumMatchList neighbor) {
    return new LostPotential(Math.abs(current.totalSize() - neighbor.totalSize()));
  }

  public List<Pair<Integer, Integer>> matches() {
    return matches;
  }
}
