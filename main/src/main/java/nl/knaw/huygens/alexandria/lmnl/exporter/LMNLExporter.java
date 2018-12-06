package nl.knaw.huygens.alexandria.lmnl.exporter;

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

import com.google.common.base.Preconditions;
import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.view.TAGView;
import nl.knaw.huygens.alexandria.view.TAGViewFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by bramb on 07/02/2017.
 */
public class LMNLExporter {
  private static Logger LOG = LoggerFactory.getLogger(LMNLExporter.class);

  private boolean useShorthand = false;
  private final TAGStore store;
  private final TAGView view;

  public LMNLExporter(TAGStore store, TAGView view) {
    Preconditions.checkNotNull(store);
    this.store = store;
    this.view = view;
  }

  public LMNLExporter(TAGStore store) {
    Preconditions.checkNotNull(store);
    this.store = store;
    this.view = new TAGViewFactory(store).getDefaultView();
  }

  public LMNLExporter useShorthand() {
    useShorthand = true;
    return this;
  }

  public String toLMNL(TAGDocument document) {
    StringBuilder lmnlBuilder = new StringBuilder();
    store.runInTransaction(() -> appendLimen(lmnlBuilder, document));
    // LOG.info("LMNL={}", lmnlBuilder);
    return lmnlBuilder.toString();
  }

  private void appendLimen(StringBuilder lmnlBuilder, TAGDocument document) {
    if (document != null) {
      Deque<Long> openMarkupIds = new ArrayDeque<>();
      Map<Long, StringBuilder> openTags = new HashMap<>();
      Map<Long, StringBuilder> closeTags = new HashMap<>();
      document.getTextNodeStream().forEach(tn -> {
        Set<Long> markupIds = new HashSet<>();
        document.getMarkupStreamForTextNode(tn).forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          openTags.computeIfAbsent(id, (k) -> toOpenTag(mw));
          closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
        });
        Set<Long> relevantMarkupIds = view.filterRelevantMarkup(markupIds);

        List<Long> toClose = new ArrayList<>(openMarkupIds);
        toClose.removeAll(relevantMarkupIds);
        Collections.reverse(toClose);
        toClose.forEach(markupId -> lmnlBuilder.append(closeTags.get(markupId)));

        List<Long> toOpen = new ArrayList<>(relevantMarkupIds);
        toOpen.removeAll(openMarkupIds);
        toOpen.forEach(markupId -> lmnlBuilder.append(openTags.get(markupId)));

        openMarkupIds.removeAll(toClose);
        openMarkupIds.addAll(toOpen);
        lmnlBuilder.append(tn.getText());
      });
      openMarkupIds.descendingIterator()//
          .forEachRemaining(markupId -> lmnlBuilder.append(closeTags.get(markupId)));
    }

  }

  private StringBuilder toCloseTag(TAGMarkup markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder("{").append(markup.getExtendedTag()).append("]");
  }

  private StringBuilder toOpenTag(TAGMarkup markup) {
    return new StringBuilder("TODO");
//    StringBuilder tagBuilder = new StringBuilder("[").append(markup.getExtendedTag());
//    markup.getAnnotationStream().forEach(a -> tagBuilder.append(" ").append(toLMNL(a)));
//    return markup.isAnonymous()//
//        ? tagBuilder.append("]")//
//        : tagBuilder.append("}");
  }

  public StringBuilder toLMNL(TAGAnnotation annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getKey());
//    annotation.getAnnotationStream()
//        .forEach(a1 -> annotationBuilder.append(" ").append(toLMNL(a1)));
//    TAGDocument document = annotation.getDocument();
//    if (document.hasTextNodes()) {
//      annotationBuilder.append("}");
//      appendLimen(annotationBuilder, document);
//      if (useShorthand) {
//        annotationBuilder.append("{]");
//      } else {
//        annotationBuilder.append("{").append(annotation.getKey()).append("]");
//      }
//    } else {
//      annotationBuilder.append("]");
//    }
    return annotationBuilder;
  }

}
