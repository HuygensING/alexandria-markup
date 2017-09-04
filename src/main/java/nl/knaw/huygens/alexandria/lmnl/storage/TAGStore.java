package nl.knaw.huygens.alexandria.lmnl.storage;

import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredAnnotation;
import nl.knaw.huygens.alexandria.lmnl.storage.dao.StoredLimen;

import java.io.File;

public class TAGStore {

  Environment bdbEnvironment = null;
  private String dbDir;
  private boolean readOnly;
  private DataAccessor da;

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

      EntityStore store;

      String databaseName = "Limen";
      DatabaseConfig dbConfig = new DatabaseConfig();
      dbConfig.setAllowCreate(!readOnly);
      dbConfig.setTransactional(true);

      DbEnv dbEnv = new DbEnv();
      File file = new File(dbDir);
      dbEnv.setup(file, false);
      da = new DataAccessor(dbEnv.getEntityStore());

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

  public Long putLimen(StoredLimen limen) {
    da.limenById.put(limen);
    return limen.getId();
  }

  public StoredLimen getLimen(Long limenId) {
    return da.limenById.get(limenId);
  }

  public Long putAnnotation(StoredAnnotation annotation) {
    da.annotationById.put(annotation);
    return annotation.getId();
  }

  public StoredAnnotation getAnnotation(Long annotationId) {
    return da.annotationById.get(annotationId);
  }
}
