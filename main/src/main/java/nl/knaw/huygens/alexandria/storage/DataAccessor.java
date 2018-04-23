package nl.knaw.huygens.alexandria.storage;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

class DataAccessor {
  final PrimaryIndex<Long, TAGDocument> documentById;
  final PrimaryIndex<Long, TAGTextNode> textNodeById;
  final PrimaryIndex<Long, TAGMarkup> markupById;
  final PrimaryIndex<Long, TAGAnnotation> annotationById;

  public DataAccessor(EntityStore store) throws DatabaseException {
    documentById = store.getPrimaryIndex(Long.class, TAGDocument.class);
    textNodeById = store.getPrimaryIndex(Long.class, TAGTextNode.class);
    markupById = store.getPrimaryIndex(Long.class, TAGMarkup.class);
    annotationById = store.getPrimaryIndex(Long.class, TAGAnnotation.class);
  }

}
