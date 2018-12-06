package nl.knaw.huc.di.tag.tagql;

/*
 * #%L
 * main
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huygens.alexandria.query.TAGQLResult;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;
import java.util.function.Predicate;

public class TAGQLSelectStatement implements TAGQLStatement {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  private Predicate<? super TAGMarkup> markupFilter = tr -> true;
  private Function<? super TAGMarkup, ? super Object> markupMapper = a -> a;

  private Integer index = null;

  public void setMarkupFilter(Predicate<? super TAGMarkup> markupFilter) {
    this.markupFilter = markupFilter;
  }

  public void setMarkupMapper(Function<? super TAGMarkup, ? super Object> markupMapper) {
    this.markupMapper = markupMapper;
  }

  @Override
  public Function<TAGDocument, TAGQLResult> getLimenProcessor() {
    return (TAGDocument document) -> {
      TAGQLResult result = new TAGQLResult();
      document.getMarkupStream()//
          .filter(markupFilter)//
          .map(markupMapper)//
          // .peek(this::logger)//
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

  private void logger(Object o) {
    LOG.debug("object={}, class={}", o, o.getClass().getName());
  }

  public void setIndex(int index) {
    this.index = index;
  }

}
