package nl.knaw.huygens.alexandria.storage.bdb;

import com.sleepycat.persist.model.Persistent;
import com.sleepycat.persist.model.PersistentProxy;

import java.util.ArrayList;
import java.util.Iterator;
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
      for (int i=0; i<setList.size(); i++){
        linkedHashSet.add(setList.get(i));
      }
    }
    return linkedHashSet;
  }

  @Override
  public  void initializeProxy(LinkedHashSet linkedHashSet) {
    if (linkedHashSet != null){
      setList = new ArrayList();
      Iterator iterator = linkedHashSet.iterator();
      while (iterator.hasNext()){
        setList.add(iterator.next());
      }
    }
  }

}