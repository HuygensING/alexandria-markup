package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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
public class Score {
  private final Score parent;
  public int globalScore = 0;
  final int x;
  final int y;
  final int previousX;
  final int previousY;
  final Boolean isMatch;

  public Score(Boolean isMatch, int x, int y, Score parent, int i) {
    this.isMatch = isMatch;
    this.x = x;
    this.y = y;
    this.parent = parent;
    this.previousX = parent == null ? 0 : parent.x;
    this.previousY = parent == null ? 0 : parent.y;
    this.globalScore = i;
  }

  public Score(Boolean isMatch, int x, int y, Score parent) {
    this.isMatch = isMatch;
    this.x = x;
    this.y = y;
    this.parent = parent;
    this.previousX = parent.x;
    this.previousY = parent.y;
    this.globalScore = parent.globalScore;
  }

  public int getGlobalScore() {
    return this.globalScore;
  }

  public void setGlobalScore(int globalScore) {
    this.globalScore = globalScore;
  }

  @Override
  public String toString() {
    return "[" + this.y + "," + this.x + "]:" + this.globalScore;
  }

  public enum Type {
    aligned, replacement, addition, omission, empty, semanticVariation
  }

}
