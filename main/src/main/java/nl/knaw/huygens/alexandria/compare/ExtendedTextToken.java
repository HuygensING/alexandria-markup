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

import prioritised_xml_collation.TextToken;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ExtendedTextToken extends TextToken {
  private final List<Long> textNodeIds = new ArrayList<>();

  public ExtendedTextToken(String content) {
    super(content);
  }

  public ExtendedTextToken addTextNodeId(Long textNodeId) {
    textNodeIds.add(textNodeId);
    return this;
  }

  public ExtendedTextToken addTextNodeIds(Collection<Long> textNodeIds) {
    this.textNodeIds.addAll(textNodeIds);
    return this;
  }

  public ExtendedTextToken addTextNodeIds(Long... textNodeIds) {
    Collections.addAll(this.textNodeIds, textNodeIds);
    return this;
  }

  public List<Long> getTextNodeIds() {
    return textNodeIds;
  }
}
