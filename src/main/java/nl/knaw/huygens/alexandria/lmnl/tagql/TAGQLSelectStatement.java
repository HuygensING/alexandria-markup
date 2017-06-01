package nl.knaw.huygens.alexandria.lmnl.tagql;

import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.lmnl.data_model.Limen;
import nl.knaw.huygens.alexandria.lmnl.data_model.Markup;
import nl.knaw.huygens.alexandria.lmnl.query.TAGQLResult;

public class TAGQLSelectStatement implements TAGQLStatement {

  private Logger LOG = LoggerFactory.getLogger(this.getClass());

  Predicate<? super Markup> markupFilter = tr -> true;
  Function<? super Markup, ? super Object> markupMapper = a -> a;

  private Integer index = null;

  public void setMarkupFilter(Predicate<? super Markup> markupFilter) {
    this.markupFilter = markupFilter;
  }

  public void setMarkupMapper(Function<? super Markup, ? super Object> markupMapper) {
    this.markupMapper = markupMapper;
  }

  @Override
  public Function<Limen, TAGQLResult> getLimenProcessor() {
    return (Limen limen) -> {
      TAGQLResult result = new TAGQLResult();
      limen.markupList.stream()//
          .filter(markupFilter)//
          .map(markupMapper)//
          // .map(this::logger)//
          .forEach(result::addValue);
      if (index != null) {
        Object selectedValue = result.getValues().get(index);
        TAGQLResult result2 = new TAGQLResult();
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
