package nl.knaw.huygens.alexandria.lmnl.storage;

import java.io.File;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;

import nl.knaw.huygens.alexandria.lmnl.storage.dao.AnnotationDAO;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.LimenDAO;

public class LMNLStore {

  Environment bdbEnvironment = null;
  private String dbDir;
  private boolean readOnly;

  public LMNLStore(String dbDir, boolean readOnly) {
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

      EntityStore store;

      String databaseName = "Limen";
      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setAllowCreate(!readOnly);
      dbConfig.setTransactional(true);
      // Database database = bdbEnvironment.openDatabase(null, databaseName, dbConfig);

    } catch (DatabaseException dbe) {
      throw new RuntimeException(dbe);
    }
  }

  public void close() {
    try {
      if (bdbEnvironment != null) {
        bdbEnvironment.cleanLog();
        bdbEnvironment.close();
      }
    } catch (DatabaseException dbe) {
      throw new RuntimeException(dbe);
    }
  }

  public Long putLimen(LimenDAO limen) {
    Long createdId = 0L;
    return createdId;
  }

  public LimenDAO getLimen(Long limenId) {
    return null;
  }

  public Long putAnnotation(AnnotationDAO annotation) {
    return 0L;
  }
}
