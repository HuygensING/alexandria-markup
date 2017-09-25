package nl.knaw.huygens.alexandria.lmnl.exporter;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import nl.knaw.huygens.alexandria.data_model.Annotation;
import nl.knaw.huygens.alexandria.data_model.Document;
import nl.knaw.huygens.alexandria.data_model.Limen;
import nl.knaw.huygens.alexandria.data_model.Markup;

/**
 * Created by bramb on 07/02/2017.
 */
public class LMNLExporterInMemory {
  private static Logger LOG = LoggerFactory.getLogger(LMNLExporterInMemory.class);
  boolean useShorthand = false;

  public LMNLExporterInMemory useShorthand() {
    useShorthand = true;
    return this;
  }

  public String toLMNL(Document document) {
    StringBuilder lmnlBuilder = new StringBuilder();
    Limen limen = document.value();
    appendLimen(lmnlBuilder, limen);
    // LOG.info("LMNL={}", lmnlBuilder);
    return lmnlBuilder.toString();
  }

  private void appendLimen(StringBuilder lmnlBuilder, Limen limen) {
    if (limen != null) {
      Deque<Markup> openMarkups = new ArrayDeque<>();
      limen.getTextNodeIterator().forEachRemaining(tn -> {
        Set<Markup> markups = limen.getMarkups(tn);

        List<Markup> toClose = new ArrayList<>();
        toClose.addAll(openMarkups);
        toClose.removeAll(markups);
        Collections.reverse(toClose);
        toClose.forEach(tr -> lmnlBuilder.append(toCloseTag(tr)));

        List<Markup> toOpen = new ArrayList<>();
        toOpen.addAll(markups);
        toOpen.removeAll(openMarkups);
        toOpen.forEach(tr -> lmnlBuilder.append(toOpenTag(tr)));

        openMarkups.removeAll(toClose);
        openMarkups.addAll(toOpen);
        lmnlBuilder.append(tn.getContent());
      });
      openMarkups.descendingIterator()//
          .forEachRemaining(tr -> lmnlBuilder.append(toCloseTag(tr)));
    }
  }

  private StringBuilder toCloseTag(Markup markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder("{").append(markup.getExtendedTag()).append("]");
  }

  private StringBuilder toOpenTag(Markup markup) {
    StringBuilder tagBuilder = new StringBuilder("[").append(markup.getExtendedTag());
    markup.getAnnotations().forEach(a -> tagBuilder.append(" ").append(toLMNL(a)));
    return markup.isAnonymous()//
        ? tagBuilder.append("]")//
        : tagBuilder.append("}");
  }

  public StringBuilder toLMNL(Annotation annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getTag());
    annotation.getAnnotations().forEach(a1 -> annotationBuilder.append(" ").append(toLMNL(a1)));
    Limen limen = annotation.value();
    if (limen.hasTextNodes()) {
      annotationBuilder.append("}");
      appendLimen(annotationBuilder, limen);
      if (useShorthand) {
        annotationBuilder.append("{]");
      } else {
        annotationBuilder.append("{").append(annotation.getTag()).append("]");
      }
    } else {
      annotationBuilder.append("]");
    }
    return annotationBuilder;
  }

}
