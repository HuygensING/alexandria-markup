package nl.knaw.huygens.alexandria.creole;

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
public class LMNLTest {
  private String lmnl;
  private String creole;
  private boolean valid;
  private String title;

  public void setLMNL(String lmnl) {
    this.lmnl = lmnl;
  }

  public String getLMNL() {
    return lmnl;
  }

  public void setCreole(String creole) {
    this.creole = creole;
  }

  public String getCreole() {
    return creole;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public boolean isValid() {
    return valid;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
