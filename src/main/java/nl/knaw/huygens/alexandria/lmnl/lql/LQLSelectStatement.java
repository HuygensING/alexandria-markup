package nl.knaw.huygens.alexandria.lmnl.lql;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.query.LQLResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Predicate;

public class LQLSelectStatement implements LQLStatement {

  private Logger LOG = LoggerFactory.getLogger(this.getClass());
  // LQLSelectVariable selectvariable;
  // LQLSource source;
  // LQLWhereStatement whereStatement;

  Predicate<? super TextRange> textRangeFilter = tr -> true;
  Function<? super TextRange, ? super Object> textRangeMapper = a -> a;

  private Integer index = null;

  public void setTextRangeFilter(Predicate<? super TextRange> textRangeFilter) {
    this.textRangeFilter = textRangeFilter;
  }

  // public Predicate<? super TextRange> getTextRangeFilter() {
  // return textRangeFilter;
  // }

  public void setTextRangeMapper(Function<? super TextRange, ? super Object> textRangeMapper) {
    this.textRangeMapper = textRangeMapper;
  }

  // public Function<? super TextRange, ? super Object> getTextRangeMapper() {
  // return textRangeMapper;
  // }

  @Override
  public Function<Limen, LQLResult> getLimenProcessor() {
    return (Limen limen) -> {
      LQLResult result = new LQLResult();
      limen.textRangeList.stream()//
          .filter(textRangeFilter)//
          .map(textRangeMapper)//
//          .map(this::logger)//
          .forEach(result::addValue);
      if (index != null) {
        Object selectedValue = result.getValues().get(index);
        LQLResult result2 = new LQLResult();
        result2.addValue(selectedValue);
        return result2;
      }
      return result;
    };
  }

  private Object logger(Object o) {
    LOG.info("object={}, class={}", o, o.getClass().getName());
    return o;
  }

  public void setIndex(int index) {
    this.index = index;
  }

}
