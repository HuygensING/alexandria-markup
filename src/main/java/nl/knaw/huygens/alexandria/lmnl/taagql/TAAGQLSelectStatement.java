package nl.knaw.huygens.alexandria.lmnl.taagql;

import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.TextRange;
import nl.knaw.huygens.alexandria.lmnl.query.TAAGQLResult;

public class TAAGQLSelectStatement implements TAAGQLStatement {

  private Logger LOG = LoggerFactory.getLogger(this.getClass());

  Predicate<? super TextRange> textRangeFilter = tr -> true;
  Function<? super TextRange, ? super Object> textRangeMapper = a -> a;

  private Integer index = null;

  public void setTextRangeFilter(Predicate<? super TextRange> textRangeFilter) {
    this.textRangeFilter = textRangeFilter;
  }

  public void setTextRangeMapper(Function<? super TextRange, ? super Object> textRangeMapper) {
    this.textRangeMapper = textRangeMapper;
  }

  @Override
  public Function<Limen, TAAGQLResult> getLimenProcessor() {
    return (Limen limen) -> {
      TAAGQLResult result = new TAAGQLResult();
      limen.textRangeList.stream()//
          .filter(textRangeFilter)//
          .map(textRangeMapper)//
          // .map(this::logger)//
          .forEach(result::addValue);
      if (index != null) {
        Object selectedValue = result.getValues().get(index);
        TAAGQLResult result2 = new TAAGQLResult();
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
