package nl.knaw.huc.di.tag.tagml.exporter;

/*-
 * #%L
 * alexandria-markup
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

import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.wrappers.AnnotationWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static nl.knaw.huc.di.tag.tagml.TAGML.*;
import static nl.knaw.huygens.alexandria.storage.TAGTextNodeType.convergence;

public class TAGMLExporter {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLExporter.class);
  private final TAGView view;

  TAGMLExporter(TAGView view) {
    this.view = view;
  }

  public String asTAGML(DocumentWrapper document) {
    StringBuilder tagmlBuilder = new StringBuilder();
    Deque<Long> openMarkupIds = new ArrayDeque<>();
    Map<Long, StringBuilder> openTags = new HashMap<>();
    Map<Long, StringBuilder> closeTags = new HashMap<>();

    Deque<TextNodeWrapper> unprocessedNodes = new LinkedList<>();
    unprocessedNodes.add(document.getFirstTextNode());
    Set<TextNodeWrapper> processedNodes = new HashSet<>();
    while (!unprocessedNodes.isEmpty()) {
      final TextNodeWrapper nodeToProcess = unprocessedNodes.pop();
      List<TextNodeWrapper> nextTextNodes = nodeToProcess.getNextTextNodes();
      logTextNode(nodeToProcess);
      if (!processedNodes.contains(nodeToProcess)) {
//        LOG.debug("processedNodes:");
//        processedNodes.forEach(this::logTextNode);
//        LOG.debug("");
        Set<Long> markupIds = new HashSet<>();
        document.getMarkupStreamForTextNode(nodeToProcess).forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          openTags.computeIfAbsent(id, (k) -> toOpenTag(mw));
          closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
        });
        Set<Long> relevantMarkupIds = view.filterRelevantMarkup(markupIds);

        List<Long> toClose = new ArrayList<>(openMarkupIds);
        toClose.removeAll(relevantMarkupIds);
        Collections.reverse(toClose);
        toClose.forEach(markupId -> tagmlBuilder.append(closeTags.get(markupId)));

        List<Long> toOpen = new ArrayList<>(relevantMarkupIds);
        toOpen.removeAll(openMarkupIds);
        toOpen.forEach(markupId -> tagmlBuilder.append(openTags.get(markupId)));

        openMarkupIds.removeAll(toClose);
        openMarkupIds.addAll(toOpen);

        TAGTextNode textNode = nodeToProcess.getTextNode();
        String content = nodeToProcess.getText();
        switch (textNode.getType()) {
          case plaintext:
            if (hasPrecedingDivergence(nodeToProcess)) {
              tagmlBuilder.append(DIVIDER);
            }
            tagmlBuilder.append(content);
            break;
          case divergence:
            tagmlBuilder.append(DIVERGENCE_STARTCHAR);
            break;
          case convergence:
            if (closeTextVariation(nodeToProcess, unprocessedNodes)) {
              tagmlBuilder.append(CONVERGENCE);
            }
            break;
        }
        if (!convergence.equals(textNode.getType())) {
          processedNodes.add(nodeToProcess);
        }
        if (!nextTextNodes.isEmpty()) {
          TextNodeWrapper firstNextTextNode = nextTextNodes.get(0);
          switch (textNode.getType()) {
            case plaintext:
              unprocessedNodes.addFirst(firstNextTextNode);
              break;
            case divergence:
              unprocessedNodes.addAll(nextTextNodes);
              break;
            case convergence:
              unprocessedNodes.add(firstNextTextNode);
              break;
          }
        }
      }
    }
    openMarkupIds.descendingIterator()//
        .forEachRemaining(markupId -> tagmlBuilder.append(closeTags.get(markupId)));
    return tagmlBuilder.toString();
  }

  private boolean closeTextVariation(final TextNodeWrapper nodeWrapper, Deque<TextNodeWrapper> unprocessedNodes) {
    List<TextNodeWrapper> nextTextNodes = nodeWrapper.getNextTextNodes();
    if (nextTextNodes.isEmpty() && unprocessedNodes.isEmpty()) {
      return true;
    }
    TextNodeWrapper nextToProcess = unprocessedNodes.peek();
    TextNodeWrapper nextTextNode = nextTextNodes.get(0);
    return nextTextNode.equals(nextToProcess);
  }

  private boolean hasPrecedingDivergence(final TextNodeWrapper nodeWrapper) {
    List<TextNodeWrapper> prevTextNodes = nodeWrapper.getPrevTextNodes();
    return prevTextNodes.size() == 1 && prevTextNodes.get(0).isDivergence();
  }

  private StringBuilder toCloseTag(MarkupWrapper markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder(CLOSE_TAG_STARTCHAR).append(markup.getExtendedTag()).append(CLOSE_TAG_ENDCHAR);
  }

  private StringBuilder toOpenTag(MarkupWrapper markup) {
    StringBuilder tagBuilder = new StringBuilder(OPEN_TAG_STARTCHAR).append(markup.getExtendedTag());
    markup.getAnnotationStream().forEach(a -> tagBuilder.append(" ").append(toTAGML(a)));
    return markup.isAnonymous()//
        ? tagBuilder.append(MILESTONE_TAG_ENDCHAR)//
        : tagBuilder.append(OPEN_TAG_ENDCHAR);
  }

  private StringBuilder toTAGML(final AnnotationWrapper a) {
    return new StringBuilder();// TODO
  }

  private void logTextNode(final TextNodeWrapper nodeWrapper) {
    TAGTextNode textNode = nodeWrapper.getTextNode();
    LOG.debug("TextNode(id={}, type={}, text=<{}>, prev={}, next={})",
        nodeWrapper.getDbId(),
        textNode.getType(),
        textNode.getText(),
        textNode.getPrevTextNodeIds(),
        textNode.getNextTextNodeIds()
    );
  }
}
