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
import java.util.concurrent.atomic.AtomicReference;

import static javax.swing.JSplitPane.DIVIDER;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class TAGMLExporter {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLExporter.class);
  private final TAGView view;

  TAGMLExporter(TAGView view) {
    this.view = view;
  }

  class ExporterState {
    Deque<Long> openMarkupIds = new ArrayDeque<>();
    Map<Long, StringBuilder> openTags = new HashMap<>();
    Map<Long, StringBuilder> closeTags = new HashMap<>();
    Long lastTextNodeId = null;

    public ExporterState copy() {
      final ExporterState copy = new ExporterState();
      copy.openMarkupIds = openMarkupIds;
      copy.openTags = openTags;
      copy.closeTags = closeTags;
      copy.lastTextNodeId = lastTextNodeId;
      return copy;
    }
  }

  public String asTAGML(DocumentWrapper document) {
    StringBuilder tagmlBuilder = new StringBuilder();

    Deque<ExporterState> stateStack = new ArrayDeque<>();
    stateStack.push(new ExporterState());

    Deque<TextNodeWrapper> unprocessedNodes = new LinkedList<>();
    unprocessedNodes.add(document.getFirstTextNode());
    Set<TextNodeWrapper> processedNodes = new HashSet<>();
    while (!unprocessedNodes.isEmpty()) {
      final AtomicReference<ExporterState> stateRef = new AtomicReference<>(stateStack.peek());
      final TextNodeWrapper nodeToProcess = unprocessedNodes.pop();
      List<TextNodeWrapper> nextTextNodes = nodeToProcess.getNextTextNodes();
      logTextNode(nodeToProcess);
      if (!processedNodes.contains(nodeToProcess) || nodeToProcess.isConvergence()) {
//        LOG.debug("processedNodes:");
//        processedNodes.forEach(this::logTextNode);
//        LOG.debug("");
//        ExporterContext context = contextStack.peek();
        ExporterState state = stateRef.get();
        Set<Long> markupIds = new HashSet<>();
        document.getMarkupStreamForTextNode(nodeToProcess).forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          state.openTags.computeIfAbsent(id, (k) -> toOpenTag(mw));
          state.closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
        });
        Set<Long> relevantMarkupIds = view.filterRelevantMarkup(markupIds);

        if (needsDivider(nodeToProcess)) {
          tagmlBuilder.append(DIVIDER);
        }

        List<Long> toClose = new ArrayList<>(state.openMarkupIds);
        toClose.removeAll(relevantMarkupIds);
        Collections.reverse(toClose);
        toClose.forEach(markupId -> tagmlBuilder.append(state.closeTags.get(markupId)));

        List<Long> toOpen = new ArrayList<>(relevantMarkupIds);
        toOpen.removeAll(state.openMarkupIds);
        toOpen.forEach(markupId -> tagmlBuilder.append(state.openTags.get(markupId)));

        state.openMarkupIds.removeAll(toClose);
        state.openMarkupIds.addAll(toOpen);

        TAGTextNode textNode = nodeToProcess.getTextNode();
        String content = nodeToProcess.getText();
        switch (textNode.getType()) {
          case plaintext:
            tagmlBuilder.append(content);
            if (!nextTextNodes.isEmpty()) {
              unprocessedNodes.addFirst(nextTextNodes.get(0));
            }
            break;
          case divergence: // This node will be visited only once
            tagmlBuilder.append(DIVERGENCE);
            stateStack.push(state.copy());
            unprocessedNodes.addAll(nextTextNodes);
            break;
          case convergence: // this node will be visisted for every textvariation branch
            if (closeTextVariation(nodeToProcess, state.lastTextNodeId)) { // have we visited all branches of this textvariation?
              stateStack.pop();
              tagmlBuilder.append(CONVERGENCE);
            }else{
              // go back to state saved at start of corresponding divergence
              stateRef.set(stateStack.peek());
            }
            if (!nextTextNodes.isEmpty()) {
              unprocessedNodes.add(nextTextNodes.get(0));
            }
            break;
        }
        processedNodes.add(nodeToProcess);
        state.lastTextNodeId = nodeToProcess.getDbId();
      }
    }
    final ExporterState state = stateStack.pop();
    state.openMarkupIds.descendingIterator()//
        .forEachRemaining(markupId -> tagmlBuilder.append(state.closeTags.get(markupId)));
    return tagmlBuilder.toString();
  }

  private boolean closeTextVariation(final TextNodeWrapper convergenceNode, final Long lastTextNodeId) {
    List<Long> prevTextNodeIds = convergenceNode.getTextNode().getPrevTextNodeIds();
    final Long lastPrevNodeId = prevTextNodeIds.get(prevTextNodeIds.size() - 1);
    return (lastTextNodeId.equals(lastPrevNodeId));
  }

  private boolean needsDivider(final TextNodeWrapper nodeWrapper) {
    List<TextNodeWrapper> prevTextNodes = nodeWrapper.getPrevTextNodes();
    return prevTextNodes.size() == 1
        && prevTextNodes.get(0).isDivergence()
        && !prevTextNodes.get(0).getNextTextNodes().get(0).equals(nodeWrapper);
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
