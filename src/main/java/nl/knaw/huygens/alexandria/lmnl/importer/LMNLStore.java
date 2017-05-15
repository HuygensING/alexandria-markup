package nl.knaw.huygens.alexandria.lmnl.importer;

import java.io.File;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;

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
}
