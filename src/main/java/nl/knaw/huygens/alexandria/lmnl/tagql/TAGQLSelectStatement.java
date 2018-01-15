package nl.knaw.huygens.alexandria.lmnl.tagql;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
 * =======
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.data_model.Limen;
import nl.knaw.huygens.alexandria.data_model.Markup;
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
