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
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
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

  public String toLMNL(DocumentWrapper document) {
    StringBuilder lmnlBuilder = new StringBuilder();
    store.runInTransaction(() -> appendLimen(lmnlBuilder, document));
    // LOG.info("LMNL={}", lmnlBuilder);
    return lmnlBuilder.toString();
  }

  private void appendLimen(StringBuilder lmnlBuilder, DocumentWrapper document) {
    if (document != null) {
      Deque<Long> openMarkupIds = new ArrayDeque<>();
      Map<Long, StringBuilder> openTags = new HashMap<>();
      Map<Long, StringBuilder> closeTags = new HashMap<>();
      document.getTextNodeStream().forEach(tn -> {
        Set<Long> markupIds = new HashSet<>();
        document.getMarkupStreamForTextNode(tn).forEach(mw -> {
          Long id = mw.getId();
          markupIds.add(id);
          openTags.computeIfAbsent(id, (k) -> toOpenTag(mw));
          closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
        });

        List<Long> toClose = new ArrayList<>();
        toClose.addAll(openMarkupIds);
        toClose.removeAll(markupIds);
        Collections.reverse(toClose);
        toClose.forEach(markupId -> lmnlBuilder.append(closeTags.get(markupId)));

        List<Long> toOpen = new ArrayList<>();
        toOpen.addAll(markupIds);
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

  private StringBuilder toCloseTag(MarkupWrapper markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder("{").append(markup.getExtendedTag()).append("]");
  }

  private StringBuilder toOpenTag(MarkupWrapper markup) {
    StringBuilder tagBuilder = new StringBuilder("[").append(markup.getExtendedTag());
    markup.getAnnotationStream().forEach(a -> tagBuilder.append(" ").append(toLMNL(a)));
    return markup.isAnonymous()//
        ? tagBuilder.append("]")//
        : tagBuilder.append("}");
  }

  public StringBuilder toLMNL(AnnotationWrapper annotation) {
    StringBuilder annotationBuilder = new StringBuilder("[").append(annotation.getTag());
    annotation.getAnnotationStream()
        .forEach(a1 -> annotationBuilder.append(" ").append(toLMNL(a1)));
    DocumentWrapper document = annotation.getDocument();
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
