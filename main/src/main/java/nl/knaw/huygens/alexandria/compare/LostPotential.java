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
import eu.interedition.collatex.dekker.astar.Cost;

public class LostPotential extends Cost<LostPotential> {
  private int cost;

  public LostPotential(final int cost) {
    this.cost = cost;
  }

  @Override
  protected LostPotential plus(final LostPotential other) {
    return new LostPotential(this.cost + other.cost);
  }

  @Override
  public int compareTo(final LostPotential other) {
    return this.cost - other.cost;
  }
}
