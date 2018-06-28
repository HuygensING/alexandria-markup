package nl.knaw.huygens.alexandria.storage;

/*-
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

import com.google.common.base.Preconditions;
import com.sleepycat.je.*;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import com.sleepycat.persist.model.AnnotationModel;
import com.sleepycat.persist.model.EntityModel;
import nl.knaw.huygens.alexandria.storage.bdb.LinkedHashSetProxy;
import nl.knaw.huygens.alexandria.storage.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.google.common.base.Preconditions.checkNotNull;

public class TAGStore implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(TAGStore.class);
  private static final LockMode LOCK_MODE = LockMode.READ_UNCOMMITTED_ALL;

  private final String dbDir;
  private final boolean readOnly;

  private Environment bdbEnvironment;
  private DataAccessor da;
  private EntityStore store;
  private ThreadLocal<Boolean> transactionOpen;
  private Transaction tx;

  public TAGStore(String dbDir, boolean readOnly) {
    this.dbDir = dbDir;
    this.readOnly = readOnly;
    open();
  }

  public void open() {
    try {
      EnvironmentConfig envConfig = new EnvironmentConfig()
          .setReadOnly(readOnly)
          .setAllowCreate(!readOnly)
          .setTransactional(true);
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

  public Long persist(TAGDTO tagdto) {
    checkNotNull(tagdto);
    assertInTransaction();
    if (tagdto instanceof TAGDocumentDTO) {
      da.documentById.put(tx, (TAGDocumentDTO) tagdto);

    } else if (tagdto instanceof TAGTextNodeDTO) {
      da.textNodeById.put(tx, (TAGTextNodeDTO) tagdto);

    } else if (tagdto instanceof TAGMarkupDTO) {
      da.markupById.put(tx, (TAGMarkupDTO) tagdto);

    } else if (tagdto instanceof TAGAnnotationDTO) {
      da.annotationById.put(tx, (TAGAnnotationDTO) tagdto);

    } else {
      throw new RuntimeException("unhandled class: " + tagdto.getClass());
    }
    return tagdto.getDbId();
  }

  public void remove(TAGDTO tagdto) {
    assertInTransaction();
    if (tagdto instanceof TAGDocumentDTO) {
      da.documentById.delete(tx, tagdto.getDbId());

    } else if (tagdto instanceof TAGTextNodeDTO) {
      da.textNodeById.delete(tx, tagdto.getDbId());

    } else if (tagdto instanceof TAGMarkupDTO) {
      da.markupById.delete(tx, tagdto.getDbId());

    } else if (tagdto instanceof TAGAnnotationDTO) {
      da.annotationById.delete(tx, tagdto.getDbId());

    } else {
      throw new RuntimeException("unhandled class: " + tagdto.getClass());
    }
  }

  // Document
  public TAGDocumentDTO getDocumentDTO(Long documentId) {
    assertInTransaction();
    return da.documentById.get(tx, documentId, LOCK_MODE);
  }

  public TAGDocument getDocument(Long documentId) {
    return new TAGDocument(this, getDocumentDTO(documentId));
  }

  public TAGDocument createDocument() {
    TAGDocumentDTO documentDTO = new TAGDocumentDTO();
    persist(documentDTO);
    documentDTO.initialize();
    return new TAGDocument(this, documentDTO);
  }

  // TextNode
  public TAGTextNodeDTO getTextNodeDTO(Long textNodeId) {
    assertInTransaction();
    return da.textNodeById.get(tx, textNodeId, LOCK_MODE);
  }

  public TAGTextNode createTextNode(String content) {
    TAGTextNodeDTO tagTextNodeDTO = new TAGTextNodeDTO(content);
    persist(tagTextNodeDTO);
    return new TAGTextNode(this, tagTextNodeDTO);
  }

  public TAGTextNode createTextNode() {
    return createTextNode("");
  }

  public TAGTextNode getTextNode(Long textNodeId) {
    return new TAGTextNode(this, getTextNodeDTO(textNodeId));
  }

  // Markup
  public TAGMarkupDTO getMarkupDTO(Long markupId) {
    assertInTransaction();
    return da.markupById.get(tx, markupId, LOCK_MODE);
  }

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

  public TAGMarkup getMarkup(Long markupId) {
    return new TAGMarkup(this, getMarkupDTO(markupId));
  }

  // Annotation
  public TAGAnnotationDTO getAnnotationDTO(Long annotationId) {
    assertInTransaction();
    return da.annotationById.get(tx, annotationId, LOCK_MODE);
  }

  public TAGAnnotationDTO createAnnotationDTO(String tag) {
//    TAGDocumentDTO document = new TAGDocumentDTO();
//    persist(document);
    TAGAnnotationDTO annotation = new TAGAnnotationDTO(tag);
//    annotation.setDocumentId(document.getDbId());
    persist(annotation);
    return annotation;
  }

  public TAGAnnotation createStringAnnotation(String aName, String value) {
    return createAnnotation(aName, value, AnnotationType.String);
  }

  public TAGAnnotation createBooleanAnnotation(String aName, Boolean value) {
    return createAnnotation(aName, value, AnnotationType.Boolean);
  }

  public TAGAnnotation createNumberAnnotation(String aName, Float value) {
    return createAnnotation(aName, value, AnnotationType.Number);
  }
  public TAGAnnotation createRefAnxnotation(String aName, String refId) {
    return createAnnotation(aName, refId, AnnotationType.Reference);
  }

  private TAGAnnotation createAnnotation(String aName, Object value, AnnotationType type) {
    TAGAnnotationDTO dto = createAnnotationDTO(aName);
    dto.setType(type);
    dto.setValue(value);
    persist(dto);
    return new TAGAnnotation(this, dto);
  }

  public TAGAnnotation createAnnotation(String tag) {
    assertInTransaction();
    TAGAnnotationDTO annotation = createAnnotationDTO(tag);
    return new TAGAnnotation(this, annotation);
  }

  public TAGAnnotation getAnnotation(Long annotationId) {
    return new TAGAnnotation(this, getAnnotationDTO(annotationId));
  }

  // transaction
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

    } catch (Exception e) {
      if (getTransactionIsOpen()) {
        rollbackTransaction();
      }
      throw e;
    }
  }

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

    } catch (Exception e) {
      e.printStackTrace();
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

}
