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
import nl.knaw.huygens.alexandria.storage.TAGMarkup;

import static java.lang.String.format;

public class MarkupInfo {
  int startRank;
  int endRank;
  TAGMarkup markup;
  private String markupPath;

  public MarkupInfo(int startRank, int endRank) {
    this.startRank = startRank;
    this.endRank = endRank;
  }

  public int getStartRank() {
    return startRank;
  }

  public void setEndRank(int endRank) {
    this.endRank = endRank;
  }

  public int getEndRank() {
    return endRank;
  }

  @Override
  public String toString() {
    return format("%s span=%d, startRank=%d, endRank=%d", markup, getSpan(), startRank, endRank);
  }

  Integer getSpan() {
    return endRank - startRank + 1;
  }

  public void setMarkup(TAGMarkup markup) {
    this.markup = markup;
  }

  public TAGMarkup getMarkup() {
    return markup;
  }

  public void setMarkupPath(final String markupPath) {
    this.markupPath = markupPath;
  }

  public String getMarkupPath() {
    return markupPath;
  }
}
