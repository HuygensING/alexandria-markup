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

import com.google.common.base.Preconditions;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huygens.alexandria.storage.TAGStore;
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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class TAGMLExporter {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLExporter.class);
  private final TAGView view;

  public TAGMLExporter(TAGView view) {
    this.view = view;
  }

  public TAGMLExporter(TAGStore store) {
    this.view = TAGViews.getShowAllMarkupView(store);
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

  class TextVariationState {
    private ExporterState startState;
    private Set<Long> branchStartNodeIds = new HashSet<>();
    private Long convergenceSucceedingNodeId;
    private Integer branchesToTraverse;

    public TextVariationState setStartState(ExporterState startState) {
      Preconditions.checkNotNull(startState);
      this.startState = startState;
      return this;
    }

    public ExporterState getStartState() {
      Preconditions.checkNotNull(startState);
      return startState;
    }

    public TextVariationState setBranchStartNodes(List<TextNodeWrapper> branchStartNodes) {
      this.branchStartNodeIds = branchStartNodes.stream()
          .map(TextNodeWrapper::getDbId).collect(toSet());
      this.branchesToTraverse = branchStartNodes.size();
      return this;
    }

    public boolean isBranchStartNode(TextNodeWrapper node) {
      return branchStartNodeIds.contains(node.getDbId());
    }

    public TextVariationState setConvergenceSucceedingNodeId(Long convergenceSucceedingNodeId) {
      this.convergenceSucceedingNodeId = convergenceSucceedingNodeId;
      return this;
    }

    public boolean isFirstNodeAfterConvergence(TextNodeWrapper node) {
      return node.getDbId().equals(convergenceSucceedingNodeId);
    }

    public void incrementBranchesStarted() {
      branchesToTraverse--;
    }

    public boolean allBranchesTraversed() {
      return branchesToTraverse == 0;
    }

    public boolean inVariation() {
      return !branchStartNodeIds.isEmpty();
    }
  }

  public String asTAGML(DocumentWrapper document) {
    StringBuilder tagmlBuilder = new StringBuilder();

    Deque<TextVariationState> textVariationStates = new ArrayDeque<>();
    textVariationStates.push(new TextVariationState());

    Deque<TextNodeWrapper> nodesToProcess = new LinkedList<>();
    nodesToProcess.push(document.getFirstTextNode());
    Set<TextNodeWrapper> processedNodes = new HashSet<>();
    final AtomicReference<ExporterState> stateRef = new AtomicReference<>(new ExporterState());
    while (!nodesToProcess.isEmpty()) {
      final TextNodeWrapper nodeToProcess = nodesToProcess.pop();
      List<TextNodeWrapper> nextTextNodes = nodeToProcess.getNextTextNodes();
      logTextNode(nodeToProcess);
      if (!processedNodes.contains(nodeToProcess)) {
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

        TextVariationState variationState = textVariationStates.peek();
        if (variationState.isFirstNodeAfterConvergence(nodeToProcess)) {
          tagmlBuilder.append(CONVERGENCE);
          textVariationStates.pop();
          variationState = textVariationStates.peek();
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

        if (variationState.isBranchStartNode(nodeToProcess)) {
          // this node starts a new branch of the current textvariation
          stateRef.set(variationState.getStartState());
          variationState.incrementBranchesStarted();
        }
        TAGTextNode textNode = nodeToProcess.getTextNode();
        String content = nodeToProcess.getText();
        switch (textNode.getType()) {
          case plaintext:
            String escapedText = variationState.inVariation()
                ? TAGML.escapeVariantText(content)
                : TAGML.escapeRegularText(content);
            tagmlBuilder.append(escapedText);
            if (!nextTextNodes.isEmpty()) {
              nodesToProcess.push(nextTextNodes.get(0));
            }
            break;

          case divergence: // This node will be visited only once
            tagmlBuilder.append(DIVERGENCE);
            textVariationStates.push(new TextVariationState());
            TextVariationState textVariationState = textVariationStates.peek();
            textVariationState.setStartState(state.copy())
                .setBranchStartNodes(nextTextNodes);
            List<TextNodeWrapper> tmp = new ArrayList<>(nextTextNodes);
            Collections.reverse(tmp);
            tmp.forEach(nodesToProcess::push);
            break;

          case convergence: // this node will be visited for every textvariation branch
            textVariationState = textVariationStates.peek();
            if (!nextTextNodes.isEmpty()) {
              textVariationState.setConvergenceSucceedingNodeId(nextTextNodes.get(0).getDbId());
              if (textVariationState.allBranchesTraversed()) {
                nodesToProcess.push(nextTextNodes.get(0));
              }
            }
            break;
        }
        if (!nodeToProcess.isConvergence()) {
          processedNodes.add(nodeToProcess);
        }
        state.lastTextNodeId = nodeToProcess.getDbId();
        LOG.info("nodesToProcess:{}", nodesToProcess.stream().map(TextNodeWrapper::getDbId).collect(toList()));
        LOG.info("TAGML={}\n", tagmlBuilder);
      }
    }
    while (!textVariationStates.isEmpty()) {
      TextVariationState textVariationState = textVariationStates.pop();
      if (textVariationState.inVariation()) {
        tagmlBuilder.append(CONVERGENCE);
      }
    }
    final ExporterState state = stateRef.get();
    state.openMarkupIds.descendingIterator()//
        .forEachRemaining(markupId -> tagmlBuilder.append(state.closeTags.get(markupId)));
    return tagmlBuilder.toString();
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

  public StringBuilder toTAGML(final AnnotationWrapper a) {
    return new StringBuilder();// TODO
  }

  private void logTextNode(final TextNodeWrapper nodeWrapper) {
    TAGTextNode textNode = nodeWrapper.getTextNode();
    LOG.debug("\n");
    LOG.debug("TextNode(id={}, type={}, text=<{}>, prev={}, next={})",
        nodeWrapper.getDbId(),
        textNode.getType(),
        textNode.getText(),
        textNode.getPrevTextNodeIds(),
        textNode.getNextTextNodeIds()
    );
  }
}
