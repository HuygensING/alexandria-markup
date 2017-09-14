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


import com.google.common.base.Preconditions;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.dao.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.dao.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dao.TAGMarkup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by bramb on 07/02/2017.
 */
public class LMNLExporter2 {
  private static Logger LOG = LoggerFactory.getLogger(LMNLExporter2.class);
  boolean useShorthand = false;
  private TAGStore store;

  public LMNLExporter2(TAGStore store) {
    Preconditions.checkNotNull(store);
    this.store = store;
  }

  public LMNLExporter2 useShorthand() {
    useShorthand = true;
    return this;
  }

  public String toLMNL(TAGDocument document) {
    StringBuilder lmnlBuilder = new StringBuilder();
    store.runInTransaction(() -> {
      appendLimen(lmnlBuilder, document);
    });
    // LOG.info("LMNL={}", lmnlBuilder);
    return lmnlBuilder.toString();
  }

  private void appendLimen(StringBuilder lmnlBuilder, TAGDocument document) {
    if (document != null) {
      Deque<TAGMarkup> openMarkups = new ArrayDeque<>();
      document.getTextNodeIds().stream()//
          .map(store::getTextNode).forEach(tn -> {
        Set<TAGMarkup> markups = store.getMarkupsForTextNode(tn);

        List<TAGMarkup> toClose = new ArrayList<>();
        toClose.addAll(openMarkups);
        toClose.removeAll(markups);
        Collections.reverse(toClose);
        toClose.forEach(markup -> lmnlBuilder.append(toCloseTag(markup)));

        List<TAGMarkup> toOpen = new ArrayList<>();
        toOpen.addAll(markups);
        toOpen.removeAll(openMarkups);
        toOpen.forEach(markup -> lmnlBuilder.append(toOpenTag(markup)));

        openMarkups.removeAll(toClose);
        openMarkups.addAll(toOpen);
        lmnlBuilder.append(tn.getText());
      });
      openMarkups.descendingIterator()//
          .forEachRemaining(markup -> lmnlBuilder.append(toCloseTag(markup)));
    }
  }

  private StringBuilder toCloseTag(TAGMarkup markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder("{").append(markup.getExtendedTag()).append("]");
  }

  private StringBuilder toOpenTag(TAGMarkup markup) {
    StringBuilder tagBuilder = new StringBuilder("[").append(markup.getExtendedTag());
    markup.getAnnotationIds().stream().map(store::getAnnotation).forEach(a -> tagBuilder.append(" ").append(toLMNL(a)));
    return markup.isAnonymous()//
        ? tagBuilder.append("]")//
        : tagBuilder.append("}");
  }

  public StringBuilder toLMNL(TAGAnnotation annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getTag());
    annotation.getAnnotationIds().stream().map(store::getAnnotation)
        .forEach(a1 -> annotationBuilder.append(" ").append(toLMNL(a1)));
    TAGDocument document = store.getDocument(annotation.getDocumentId());
    if (document.hasTextNodes()) {
      annotationBuilder.append("}");
      appendLimen(annotationBuilder, document);
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
