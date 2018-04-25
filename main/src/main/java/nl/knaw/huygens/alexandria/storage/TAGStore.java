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
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

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

  public Long persist(TAGObject tagObject) {
    assertInTransaction();
    if (tagObject instanceof TAGDocument) {
      da.documentById.put(tx, (TAGDocument) tagObject);

    } else if (tagObject instanceof TAGTextNode) {
      da.textNodeById.put(tx, (TAGTextNode) tagObject);

    } else if (tagObject instanceof TAGMarkup) {
      da.markupById.put(tx, (TAGMarkup) tagObject);

    } else if (tagObject instanceof TAGAnnotation) {
      da.annotationById.put(tx, (TAGAnnotation) tagObject);

    } else {
      throw new RuntimeException("unhandled class: " + tagObject.getClass());
    }
    return tagObject.getDbId();
  }

  // Document
  public TAGDocument getDocument(Long documentId) {
    assertInTransaction();
    return da.documentById.get(tx, documentId, LOCK_MODE);
  }

  public DocumentWrapper getDocumentWrapper(Long documentId) {
    return new DocumentWrapper(this, getDocument(documentId));
  }

  public DocumentWrapper createDocumentWrapper() {
    TAGDocument document = new TAGDocument();
    persist(document);
    return new DocumentWrapper(this, document);
  }

  // TextNode
  public TAGTextNode getTextNode(Long textNodeId) {
    assertInTransaction();
    return da.textNodeById.get(tx, textNodeId, LOCK_MODE);
  }

  public TextNodeWrapper createTextNodeWrapper(String content) {
    TAGTextNode textNode = new TAGTextNode(content);
    persist(textNode);
    return new TextNodeWrapper(this, textNode);
  }

  public TextNodeWrapper createTextNodeWrapper(TAGTextNodeType type) {
    TAGTextNode textNode = new TAGTextNode(type);
    persist(textNode);
    return new TextNodeWrapper(this, textNode);
  }

  public TextNodeWrapper getTextNodeWrapper(Long textNodeId) {
    return new TextNodeWrapper(this, getTextNode(textNodeId));
  }

  // Markup
  public TAGMarkup getMarkup(Long markupId) {
    assertInTransaction();
    return da.markupById.get(tx, markupId, LOCK_MODE);
  }

  public MarkupWrapper createMarkupWrapper(DocumentWrapper document, String tagName) {
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

    TAGMarkup markup = new TAGMarkup(document.getDbId(), tag);
    markup.setMarkupId(id);
    markup.setSuffix(suffix);
    persist(markup);
    // document.addMarkup(markup);
    return new MarkupWrapper(this, markup);
  }

  public MarkupWrapper getMarkupWrapper(Long markupId) {
    return new MarkupWrapper(this, getMarkup(markupId));
  }

  // Annotation
  public TAGAnnotation getAnnotation(Long annotationId) {
    assertInTransaction();
    return da.annotationById.get(tx, annotationId, LOCK_MODE);
  }

  public TAGAnnotation createAnnotation(String tag) {
    TAGDocument document = new TAGDocument();
    persist(document);
    TAGAnnotation annotation = new TAGAnnotation(tag);
    annotation.setDocumentId(document.getDbId());
    persist(annotation);
    return annotation;
  }

  public AnnotationWrapper createAnnotationWrapper(String tag) {
    TAGAnnotation annotation = createAnnotation(tag);
    return new AnnotationWrapper(this, annotation);
  }

  public AnnotationWrapper createAnnotationWrapper(String tag, String value) {
    AnnotationWrapper annotationWrapper = createAnnotationWrapper(tag);
    TextNodeWrapper textNodeWrapper = createTextNodeWrapper(value);
    annotationWrapper.getDocument().addTextNode(textNodeWrapper);
    return annotationWrapper;
  }

  public AnnotationWrapper getAnnotationWrapper(Long annotationId) {
    return new AnnotationWrapper(this, getAnnotation(annotationId));
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
