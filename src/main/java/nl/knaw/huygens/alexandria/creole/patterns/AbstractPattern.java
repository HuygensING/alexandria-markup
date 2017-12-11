package nl.knaw.huygens.alexandria.creole.patterns;

    /*-
     * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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

import com.google.common.base.Preconditions;
import nl.knaw.huygens.alexandria.creole.Pattern;

abstract class AbstractPattern implements Pattern {
  Boolean nullable;
  Boolean allowsText;
  Boolean allowsAnnotations;
  Boolean onlyAnnotations;

  abstract void init();

  int hashcode = getClass().hashCode();

  void setHashcode(int hashcode) {
    Preconditions.checkState(hashcode != 0, "hashCode should not be 0!");
    this.hashcode = hashcode;
  }

  @Override
  public boolean isNullable() {
    if (nullable == null) {
      init();
      if (nullable == null) {
        throw new RuntimeException("nullable == null! Make sure nullable is initialized in the init() of " //
            + getClass().getSimpleName());
      }
    }
    return nullable;
  }

  @Override
  public boolean allowsText() {
    if (allowsText == null) {
      init();
      if (allowsText == null) {
        throw new RuntimeException("allowsText == null! Make sure allowsText is initialized in the init() of " //
            + getClass().getSimpleName());
      }
    }
    return allowsText;
  }

  @Override
  public boolean allowsAnnotations() {
    if (allowsAnnotations == null) {
      init();
      if (allowsAnnotations == null) {
        throw new RuntimeException("allowsAnnotations == null! Make sure allowsAnnotations is initialized in the init() of " //
            + getClass().getSimpleName());
      }
    }
    return allowsAnnotations;
  }

  @Override
  public boolean onlyAnnotations() {
    if (onlyAnnotations == null) {
      init();
      if (onlyAnnotations == null) {
        throw new RuntimeException("onlyAnnotations == null! Make sure onlyAnnotations is initialized in the init() of " //
            + getClass().getSimpleName());
      }
    }
    return onlyAnnotations;
  }

  @Override
  public int hashCode() {
    return hashcode;
  }
}
