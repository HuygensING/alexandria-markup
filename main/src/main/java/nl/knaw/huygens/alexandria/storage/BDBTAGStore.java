package nl.knaw.huygens.alexandria.storage;

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

import com.google.common.base.Preconditions;
import com.sleepycat.je.*;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;
import nl.knaw.huygens.alexandria.storage.bdb.LinkedHashSetProxy;
import nl.knaw.huygens.alexandria.storage.dto.TAGDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocumentDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkupDTO;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class BDBTAGStore implements TAGStore {
  private static final Logger LOG = LoggerFactory.getLogger(BDBTAGStore.class);
  private static final LockMode LOCK_MODE = LockMode.READ_UNCOMMITTED_ALL;

  private final String dbDir;
  private final boolean readOnly;

  private Environment bdbEnvironment;
  public DataAccessor da;
  private EntityStore store;
  private ThreadLocal<Boolean> transactionOpen;
  private Transaction tx;

  public BDBTAGStore(String dbDir, boolean readOnly) {
//    LOG.debug("db dir={}", dbDir);
    this.dbDir = dbDir;
    this.readOnly = readOnly;
    open();
  }

  @Override
  public void open() {
    try {
      EnvironmentConfig envConfig = new EnvironmentConfig()
          .setReadOnly(readOnly)
          .setAllowCreate(!readOnly)
          .setTransactional(true)
          .setConfigParam(EnvironmentConfig.FREE_DISK, "0");

      bdbEnvironment = new Environment(new File(dbDir), envConfig);

      EntityModel model = new AnnotationModel();
      model.registerClass(LinkedHashSetProxy.class);
      StoreConfig storeConfig = new StoreConfig()
          .setAllowCreate(true)
          .setTransactional(true)
          .setModel(model);
      store = new EntityStore(bdbEnvironment, "TAGStore", storeConfig);

      da = new DataAccessor(store);
      tx = null;

    } catch (DatabaseException dbe) {
      throw new RuntimeException(dbe);
    }
  }

  @Override
  public void close() {
    try {
      if (tx != null && tx.isValid()) {
        tx.abort(); // close it or lose it
      }
      if (store != null) {
        store.close();
      }
      if (bdbEnvironment != null && !bdbEnvironment.isClosed()) {
        bdbEnvironment.cleanLog();
        bdbEnvironment.close();
      }
    } catch (DatabaseException dbe) {
      throw new RuntimeException(dbe);
    }
  }

  @Override
  public Long persist(TAGDTO tagdto) {
    checkNotNull(tagdto);
    assertInTransaction();
    Class<? extends TAGDTO> dtoClass = tagdto.getClass();
    final PrimaryIndex index = da.getPrimaryIndexForClass(dtoClass);
    if (index != null) {
      index.put(tx, tagdto);

    } else {
      throw new RuntimeException("unhandled class: " + tagdto.getClass());
    }
    return tagdto.getDbId();
  }

  @Override
  public void remove(TAGDTO tagdto) {
    checkNotNull(tagdto);
    assertInTransaction();
    Class<? extends TAGDTO> dtoClass = tagdto.getClass();
    final PrimaryIndex index = da.getPrimaryIndexForClass(dtoClass);
    if (index != null) {
      index.delete(tx, tagdto.getDbId());

    } else {
      throw new RuntimeException("unhandled class: " + tagdto.getClass());
    }
  }

  // Document
  @Override
  public TAGDocumentDTO getDocumentDTO(Long documentId) {
    assertInTransaction();
    return da.documentById.get(tx, documentId, LOCK_MODE);
  }

  @Override
  public TAGDocument getDocument(Long documentId) {
    return new TAGDocument(this, getDocumentDTO(documentId));
  }

  @Override
  public TAGDocument createDocument() {
    TAGDocumentDTO documentDTO = new TAGDocumentDTO();
    persist(documentDTO);
    documentDTO.initialize();
    return new TAGDocument(this, documentDTO);
  }

  // TextNode
  @Override
  public TAGTextNodeDTO getTextNodeDTO(Long textNodeId) {
    assertInTransaction();
    return da.textNodeById.get(tx, textNodeId, LOCK_MODE);
  }

  @Override
  public TAGTextNode createTextNode(String content) {
    TAGTextNodeDTO tagTextNodeDTO = new TAGTextNodeDTO(content);
    persist(tagTextNodeDTO);
    return new TAGTextNode(this, tagTextNodeDTO);
  }

  @Override
  public TAGTextNode createTextNode() {
    return createTextNode("");
  }

  @Override
  public TAGTextNode getTextNode(Long textNodeId) {
    return new TAGTextNode(this, getTextNodeDTO(textNodeId));
  }

  // Markup
  @Override
  public TAGMarkupDTO getMarkupDTO(Long markupId) {
    assertInTransaction();
    return da.markupById.get(tx, markupId, LOCK_MODE);
  }

  @Override
  public TAGMarkup createMarkup(TAGDocument document, String tagName) {
    String tag;
    String suffix = null;
    String id = null;
    if (tagName == null) {
      tag = "";

    } else if (tagName.contains("~")) {
      String[] parts = tagName.split("~");
      tag = parts[0];
      suffix = parts[1];

    } else if (tagName.contains("=")) {
      String[] parts = tagName.split("=");
      tag = parts[0];
      id = parts[1];

    } else if (tagName.contains("@")) {
      String[] parts = tagName.split("@");
      tag = parts[0];
      id = parts[1];

    } else {
      tag = tagName;
    }

    TAGMarkupDTO markupDTO = new TAGMarkupDTO(document.getDbId(), tag);
    markupDTO.setMarkupId(id);
    markupDTO.setSuffix(suffix);
    persist(markupDTO);
    // document.addMarkup(markup);
    return new TAGMarkup(this, markupDTO);
  }

  @Override
  public TAGMarkup getMarkup(Long markupId) {
    return new TAGMarkup(this, getMarkupDTO(markupId));
  }

  // Annotation
//  public TAGAnnotationDTO getAnnotationDTO(Long annotationId) {
//    assertInTransaction();
//    return da.annotationById.get(tx, annotationId, LOCK_MODE);
//  }
//
//  public TAGAnnotationDTO createAnnotationDTO(String tag) {
////    TAGDocumentDTO document = new TAGDocumentDTO();
////    persist(document);
//    TAGAnnotationDTO annotation = new TAGAnnotationDTO(tag);
////    annotation.setDocumentId(document.getResourceId());
//    persist(annotation);
//    return annotation;
//  }

//  public TAGAnnotation createStringAnnotation(String key, String value) {
//    return createAnnotation(key, value, AnnotationType.String);
//  }
//
//  public TAGAnnotation createBooleanAnnotation(String key, Boolean value) {
//    return createAnnotation(key, value, AnnotationType.Boolean);
//  }
//
//  public TAGAnnotation createNumberAnnotation(String key, Double value) {
//    return createAnnotation(key, value, AnnotationType.Number);
//  }
//
//  public TAGAnnotation createListAnnotation(final String key, final List<?> value) {
//    return createAnnotation(key, value, AnnotationType.List);
//  }
//
//  public TAGAnnotation createObjectAnnotation(String key, Object value) {
//    return createAnnotation(key, value, AnnotationType.Object);
//  }
//
//  public TAGAnnotation createRefAnnotation(String aName, String refId) {
//    return createAnnotation(aName, refId, AnnotationType.Reference);
//  }

//  private TAGAnnotation createAnnotation(String aName, Object value, AnnotationType type) {
//    TAGAnnotationDTO dto = createAnnotationDTO(aName);
//    dto.setType(type);
//    dto.setValue(value);
//    persist(dto);
//    return new TAGAnnotation(this, dto);
//  }
//
//  public TAGAnnotation createAnnotation(String tag) {
//    assertInTransaction();
//    TAGAnnotationDTO annotation = createAnnotationDTO(tag);
//    return new TAGAnnotation(this, annotation);
//  }
//
//  public TAGAnnotation getAnnotation(Long annotationId) {
//    return new TAGAnnotation(this, getAnnotationDTO(annotationId));
//  }

  // transaction
  @Override
  public void runInTransaction(Runnable runner) {
    boolean startedInOpenTransaction = getTransactionIsOpen();
    if (!startedInOpenTransaction) {
      startTransaction();
    }
    try {
      runner.run();
      if (!startedInOpenTransaction) {
        commitTransaction();
      }

    } catch (Throwable e) {
      if (getTransactionIsOpen()) {
        rollbackTransaction();
      }
      throw e;
    }
  }

  @Override
  public <A> A runInTransaction(Supplier<A> supplier) {
    boolean inOpenTransaction = getTransactionIsOpen();
    if (!inOpenTransaction) {
      startTransaction();
    }
    try {
      A result = supplier.get();
      if (!inOpenTransaction) {
        commitTransaction();
      }
      return result;

    } catch (Throwable e) {
      if (getTransactionIsOpen()) {
        rollbackTransaction();
      }
      throw e;
    }
  }

  private Boolean getTransactionIsOpen() {
    return getTransactionOpen().get();
  }

  private ThreadLocal<Boolean> getTransactionOpen() {
    if (transactionOpen == null) {
      transactionOpen = ThreadLocal.withInitial(() -> false);
    }
    return transactionOpen;
  }

  private void startTransaction() {
    assertTransactionIsClosed();
    tx = bdbEnvironment.beginTransaction(null, null);
    tx.setLockTimeout(1L, TimeUnit.MINUTES);
    setTransactionIsOpen(true);
  }

  private void setTransactionIsOpen(Boolean b) {
    getTransactionOpen().set(b);
  }

  private void commitTransaction() {
    assertTransactionIsOpen();
    tryCommitting(10);
    setTransactionIsOpen(false);
  }

  private void tryCommitting(int count) {
    if (count > 1) {
      try {
        tx.commit();
      } catch (Exception e) {
        // wait
        try {
          LOG.error("exception={}", e);
          Thread.sleep(500);
        } catch (InterruptedException ie) {
          ie.printStackTrace();
        }
        // try again
        tryCommitting(count - 1);
      }
    } else {
      tx.commit();
    }
  }

  private void rollbackTransaction() {
    assertTransactionIsOpen();
    tx.abort();
    setTransactionIsOpen(false);
  }

  private void assertInTransaction() {
    Preconditions.checkState(getTransactionIsOpen(),
        "We should be in an open transaction at this point, use runInTransaction()!");
  }

  private void assertTransactionIsClosed() {
    Preconditions.checkState(!getTransactionIsOpen(),
        "We're already inside an open transaction!");
  }

  private void assertTransactionIsOpen() {
    Preconditions.checkState(getTransactionIsOpen(),
        "We're not in an open transaction!");
  }

  @Override
  public Long createStringAnnotationValue(final String value) {
    assertInTransaction();
    StringAnnotationValue av = new StringAnnotationValue(value);
    return persist(av);
  }

  @Override
  public Long createBooleanAnnotationValue(final Boolean value) {
    assertInTransaction();
    BooleanAnnotationValue av = new BooleanAnnotationValue(value);
    return persist(av);
  }

  @Override
  public Long createNumberAnnotationValue(Double value) {
    assertInTransaction();
    NumberAnnotationValue av = new NumberAnnotationValue(value);
    return persist(av);
  }

  @Override
  public Long createListAnnotationValue() {
    assertInTransaction();
    ListAnnotationValue av = new ListAnnotationValue();
    return persist(av);
  }

  @Override
  public Long createMapAnnotationValue() {
    assertInTransaction();
    MapAnnotationValue av = new MapAnnotationValue();
    return persist(av);
  }

  @Override
  public Long createReferenceValue(String value) {
    assertInTransaction();
    ReferenceValue av = new ReferenceValue(value);
    return persist(av);
  }

  @Override
  public StringAnnotationValue getStringAnnotationValue(final Long id) {
    return da.stringAnnotationValueById.get(id);
  }

  @Override
  public NumberAnnotationValue getNumberAnnotationValue(final Long id) {
    return da.numberAnnotationValueById.get(id);
  }

  @Override
  public BooleanAnnotationValue getBooleanAnnotationValue(final Long id) {
    return da.booleanAnnotationValueById.get(id);
  }

  @Override
  public ReferenceValue getReferenceValue(final Long id) {
    return da.referenceValueById.get(id);
  }

}
