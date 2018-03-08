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

import static java.lang.String.format;

public class MarkupInfo {

  enum State {openStart, openEnd, closed}

  private String tag;
  private State state;

  public MarkupInfo(String tag, State state) {
    this.tag = tag;
    this.state = state;
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }

  public boolean isClosed() {
    return State.closed.equals(state);
  }

  @Override
  public String toString() {
    String open = format("[%s}", tag);
    String close = format("{%s]", tag);
    switch (state) {
      case openStart:
        return close;
      case closed:
        return open + close;
      case openEnd:
        return open;
      default:
        throw new RuntimeException("unexpected state: " + state);
    }
  }
}
