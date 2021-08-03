package nl.knaw.huygens.alexandria.storage.bdb;

/*
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2021 HuC DI (KNAW)
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

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;

@Persistent(proxyFor = LinkedHashSet.class)
public class LinkedHashSetProxy implements PersistentProxy<LinkedHashSet> {

  private ArrayList setList = null;

  public LinkedHashSetProxy() {}

  @Override
  public LinkedHashSet convertProxy() {
    LinkedHashSet linkedHashSet = null;
    if (setList != null) {
      linkedHashSet = new LinkedHashSet(setList.size());
      linkedHashSet.addAll(setList);
    }
    return linkedHashSet;
  }

  @Override
  public void initializeProxy(LinkedHashSet linkedHashSet) {
    if (linkedHashSet != null) {
      setList = new ArrayList();
      setList.addAll(linkedHashSet);
    }
  }
}
