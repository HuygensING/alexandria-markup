package nl.knaw.huygens.alexandria.storage;

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

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;

import java.util.HashMap;
import java.util.Map;

class DataAccessor {
  final PrimaryIndex<Long, TAGDocument> documentById;
  final PrimaryIndex<Long, TAGTextNode> textNodeById;
  final PrimaryIndex<Long, TAGMarkup> markupById;

  final PrimaryIndex<Long, StringAnnotationValue> stringAnnotationValueById;
  final PrimaryIndex<Long, BooleanAnnotationValue> booleanAnnotationValueById;
  final PrimaryIndex<Long, NumberAnnotationValue> numberAnnotationValueById;
  final PrimaryIndex<Long, ListAnnotationValue> listAnnotationValueById;
  final PrimaryIndex<Long, MapAnnotationValue> mapAnnotationValueById;
  final PrimaryIndex<Long, ReferenceValue> referenceValueById;

  Map<Class, PrimaryIndex> indexMap = new HashMap<>();

  public DataAccessor(EntityStore store) throws DatabaseException {
    documentById = initIndex(store, TAGDocument.class);
    textNodeById = initIndex(store, TAGTextNode.class);
    markupById = initIndex(store, TAGMarkup.class);
    stringAnnotationValueById = initIndex(store, StringAnnotationValue.class);
    booleanAnnotationValueById = initIndex(store, BooleanAnnotationValue.class);
    numberAnnotationValueById = initIndex(store, NumberAnnotationValue.class);
    listAnnotationValueById = initIndex(store, ListAnnotationValue.class);
    mapAnnotationValueById = initIndex(store, MapAnnotationValue.class);
    referenceValueById = initIndex(store, ReferenceValue.class);
  }

  public <T> PrimaryIndex<Long, T> getPrimaryIndexForClass(final Class<T> dtoClass) {
    return indexMap.get(dtoClass);
  }

  public PrimaryIndex initIndex(EntityStore store, Class dtoClass) {
    PrimaryIndex primaryIndex = store.getPrimaryIndex(Long.class, dtoClass);
    indexMap.put(dtoClass, primaryIndex);
    return primaryIndex;
  }
}
