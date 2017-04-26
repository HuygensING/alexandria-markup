package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.ArrayList;
import java.util.List;

public class LQLResult {

  List<LQLResult> results = new ArrayList<>();

  public List<String> asList() {
    List<String> list = new ArrayList<>();
    return list;
  }

  public void addResult(LQLResult subresult) {
    results.add(subresult);
  }
}
