package nl.knaw.huygens.alexandria.lmnl.storage;

import com.google.common.base.Preconditions;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.Transaction;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGMarkup;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.TAGTextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.function.Supplier;

public class TAGStore {
  private static final Logger LOG = LoggerFactory.getLogger(TAGStore.class);

  Environment bdbEnvironment = null;
  private String dbDir;
  private boolean readOnly;
  private DataAccessor da;
  private EntityStore store;
  private ThreadLocal<Boolean> transactionOpen;
  private Transaction tx;

  public TAGStore(String dbDir, boolean readOnly) {
    this.dbDir = dbDir;
    this.readOnly = readOnly;
  }

  public void open() {
    try {
      EnvironmentConfig envConfig = new EnvironmentConfig();
      envConfig.setReadOnly(readOnly);
      envConfig.setAllowCreate(!readOnly);
      envConfig.setTransactional(true);
      bdbEnvironment = new Environment(new File(dbDir), envConfig);

      StoreConfig storeConfig = new StoreConfig();
      storeConfig.setAllowCreate(true);
      storeConfig.setTransactional(true);
      store = new EntityStore(bdbEnvironment, "TAGStore", storeConfig);

//      String databaseName = "Limen";
//      DatabaseConfig dbConfig = new DatabaseConfig();
//      dbConfig.setAllowCreate(!readOnly);
//      dbConfig.setTransactional(true);
//
//      DbEnv dbEnv = new DbEnv();
//      File file = new File(dbDir);
//      dbEnv.setup(file, false);
      da = new DataAccessor(store);

    } catch (DatabaseException dbe) {
      throw new RuntimeException(dbe);
    }
  }

  public void close() {
    try {
      if (store != null) {
        store.close();
      }
      if (bdbEnvironment != null) {
        bdbEnvironment.cleanLog();
        bdbEnvironment.close();
      }
    } catch (DatabaseException dbe) {
      throw new RuntimeException(dbe);
    }
  }

  public Long putLimen(TAGDocument limen) {
    assertInTransaction();
    da.limenById.put(limen);
    return limen.getId();
  }

  public TAGDocument getLimen(Long limenId) {
    assertInTransaction();
    return da.limenById.get(limenId);
  }

  public Long putAnnotation(TAGAnnotation annotation) {
    assertInTransaction();
    da.annotationById.put(tx, annotation);
    return annotation.getId();
  }

  public TAGAnnotation getAnnotation(Long annotationId) {
    assertInTransaction();
    return da.annotationById.get(annotationId);
  }

  public Long putTextNode(TAGTextNode textNode) {
    assertInTransaction();
    da.textNodeById.put(textNode);
    return textNode.getId();
  }

  public TAGTextNode getTextNode(Long textNodeId) {
    assertInTransaction();
    return da.textNodeById.get(textNodeId);
  }

  public TAGMarkup getMarkup(Long markupId) {
    assertInTransaction();
    return da.markupById.get(markupId);
  }

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
      e.printStackTrace();
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

  Boolean getTransactionIsOpen() {
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
    setTransactionIsOpen(true);
  }

  void setTransactionIsOpen(Boolean b) {
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
    Preconditions.checkState(getTransactionIsOpen(), "We should be in an open transaction at this point, use runInTransaction()!");
  }

  private void assertTransactionIsClosed() {
    Preconditions.checkState(!getTransactionIsOpen(), "We're already inside an open transaction!");
  }

  private void assertTransactionIsOpen() {
    Preconditions.checkState(getTransactionIsOpen(), "We're not in an open transaction!");
  }


}
