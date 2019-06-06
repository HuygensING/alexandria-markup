package nl.knaw.huygens.alexandria.data_model;

/*
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

import nl.knaw.huygens.alexandria.storage.TAGDocumentDAO;

/**
 * Created by Ronald Haentjens Dekker on 29/12/16.
 * <p>
 * A document contains a Limen.
 *</p>
 * @deprecated use {@link TAGDocumentDAO} instead.
 */
@Deprecated
public class Document {
  private final Limen value;

  public Document() {
    this.value = new Limen();
  }

  public Limen value() {
    return value;
  }
}
