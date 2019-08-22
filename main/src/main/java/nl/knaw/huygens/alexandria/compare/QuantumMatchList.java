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

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;

public class QuantumMatchList {

  final List<Pair<Integer, Integer>> chosenMatches;
  final List<Pair<Integer, Integer>> potentialMatches;

  public QuantumMatchList(List<Pair<Integer, Integer>> chosenMatches, List<Pair<Integer, Integer>> potentialMatches) {
    this.chosenMatches = chosenMatches;
    this.potentialMatches = potentialMatches;
  }

  public QuantumMatchList chooseMatch(Pair<Integer, Integer> match) {
    checkState(potentialMatches.contains(match));

    List<Pair<Integer, Integer>> newChosen = cloneChosenMatches();
    newChosen.add(match);

    List<Pair<Integer, Integer>> newPotential = calculateNewPotential(potentialMatches, match);

    return new QuantumMatchList(newChosen, newPotential);
  }

  public QuantumMatchList discardMatch(Pair<Integer, Integer> match) {
    checkState(potentialMatches.contains(match));

    List<Pair<Integer, Integer>> newChosen = cloneChosenMatches();

    List<Pair<Integer, Integer>> newPotential = new ArrayList<>(potentialMatches);
    newPotential.remove(match);

    return new QuantumMatchList(newChosen, newPotential);
  }

  private List<Pair<Integer, Integer>> cloneChosenMatches() {
    return new ArrayList<>(chosenMatches);
  }

  private List<Pair<Integer, Integer>> calculateNewPotential(List<Pair<Integer, Integer>> potentialMatches, Pair<Integer, Integer> match) {
    List<Pair<Integer, Integer>> newPotential = new ArrayList<>(potentialMatches);
    List<Pair<Integer, Integer>> invalidatedMatches = calculateInvalidatedMatches(potentialMatches, match);
    newPotential.removeAll(invalidatedMatches);
    return newPotential;
  }

  public boolean isDetermined() {
    return potentialMatches.isEmpty();
  }

  public int totalSize() {
    return chosenMatches.size() + potentialMatches.size();
  }

  private List<Pair<Integer, Integer>> calculateInvalidatedMatches(List<Pair<Integer, Integer>> potentialMatches, Pair<Integer, Integer> match) {
    Integer leftIndex = match.getLeft();
    Integer rightIndex = match.getRight();

    return potentialMatches.stream()//
        .filter(m -> m.getLeft().equals(leftIndex) || m.getRight().equals(rightIndex)) //
        .collect(toList());
  }

  public List<Pair<Integer, Integer>> getChosenMatches() {
    return chosenMatches;
  }

  @Override
  public String toString() {
    return "(" + chosenMatches + " | " + potentialMatches + ")";
  }

  public List<Pair<Integer, Integer>> getPotentialMatches() {
    return potentialMatches;
  }

  @Override
  public int hashCode() {
    return chosenMatches.hashCode() + potentialMatches.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof QuantumMatchList)) {
      return false;
    }
    QuantumMatchList other = (QuantumMatchList) obj;
    return chosenMatches.equals(other.chosenMatches) && potentialMatches.equals(other.potentialMatches);
  }

}
