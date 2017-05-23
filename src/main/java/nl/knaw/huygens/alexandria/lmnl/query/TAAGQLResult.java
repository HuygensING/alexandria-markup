package nl.knaw.huygens.alexandria.lmnl.query;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TAAGQLResult {

  List<TAAGQLResult> results = new ArrayList<>();

  private List<Object> values = new ArrayList<>();

  public void addResult(TAAGQLResult subresult) {
    results.add(subresult);
  }

  public void addValue(Object value) {
    values.add(value);
  }

  public List<Object> getValues() {
    if (results.isEmpty()) {
      return values;
    }
    if (results.size() == 1) {
      return results.get(0).getValues();
    }
    return results.stream()//
        .map(TAAGQLResult::getValues)//
        .collect(Collectors.toList());
  }
}