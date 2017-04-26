package nl.knaw.huygens.alexandria.lmnl.lql;

import java.util.function.Function;
import java.util.function.Predicate;

import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;

public class LQLSelectStatement implements LQLStatement {

  LQLSelectVariable selectvariable;
  LQLSource source;
  LQLWhereStatement whereStatement;

  public Predicate<? super TextRange> getTextRangeFilter() {
    // TODO
    return tr -> true;
  }

  public Function getTextRangeMapper() {
    // TODO
    return a -> a;
  }

}
