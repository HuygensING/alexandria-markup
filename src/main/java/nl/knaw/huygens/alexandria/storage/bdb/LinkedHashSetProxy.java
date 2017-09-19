package nl.knaw.huygens.alexandria.storage.bdb;

import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;

import java.util.ArrayList;
import java.util.LinkedHashSet;

@Persistent(proxyFor=LinkedHashSet.class)
public class LinkedHashSetProxy implements PersistentProxy<LinkedHashSet> {

  protected ArrayList setList = null;

  public  LinkedHashSetProxy() {}

  @Override
  public  LinkedHashSet convertProxy() {
    LinkedHashSet linkedHashSet = null;
    if (setList != null){
      linkedHashSet = new LinkedHashSet(setList.size());
      for (Object aSetList : setList) {
        linkedHashSet.add(aSetList);
      }
    }
    return linkedHashSet;
  }

  @Override
  public  void initializeProxy(LinkedHashSet linkedHashSet) {
    if (linkedHashSet != null){
      setList = new ArrayList();
      setList.addAll(linkedHashSet);
    }
  }

}