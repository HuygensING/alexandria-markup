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
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import nl.knaw.huygens.alexandria.storage.TAGAnnotation;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class TAGMLExporter {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLExporter.class);
  private final TAGView view;
  private final TAGStore store;

  public TAGMLExporter(TAGStore store) {
    this(store, TAGViews.getShowAllMarkupView(store));
  }

  public TAGMLExporter(TAGStore store, TAGView view) {
    this.store = store;
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

    public TextVariationState setBranchStartNodes(List<TAGTextNode> branchStartNodes) {
      this.branchStartNodeIds = branchStartNodes.stream()
          .map(TAGTextNode::getDbId).collect(toSet());
      this.branchesToTraverse = branchStartNodes.size();
      return this;
    }

    public boolean isBranchStartNode(TAGTextNode node) {
      return branchStartNodeIds.contains(node.getDbId());
    }

    public TextVariationState setConvergenceSucceedingNodeId(Long convergenceSucceedingNodeId) {
      this.convergenceSucceedingNodeId = convergenceSucceedingNodeId;
      return this;
    }

    public boolean isFirstNodeAfterConvergence(TAGTextNode node) {
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

  public String asTAGML(TAGDocument document) {
    Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle = new HashMap<>();
    document.getMarkupStream()
        .filter(TAGMarkup::isDiscontinuous)
        .forEach(mw -> discontinuousMarkupTextNodesToHandle.put(mw.getDbId(), new AtomicInteger(mw.getTextNodeCount())));

    StringBuilder tagmlBuilder = new StringBuilder();

    Deque<TextVariationState> textVariationStates = new ArrayDeque<>();
    textVariationStates.push(new TextVariationState());

    Deque<TAGTextNode> nodesToProcess = new LinkedList<>();
    nodesToProcess.push(document.getFirstTextNode());
    Set<TAGTextNode> processedNodes = new HashSet<>();
    final AtomicReference<ExporterState> stateRef = new AtomicReference<>(new ExporterState());
    while (!nodesToProcess.isEmpty()) {
      final TAGTextNode nodeToProcess = nodesToProcess.pop();
      List<TAGTextNode> nextTextNodes = nodeToProcess.getNextTextNodes();
      logTextNode(nodeToProcess);
      if (!processedNodes.contains(nodeToProcess)) {
        ExporterState state = stateRef.get();
        Set<Long> markupIds = new HashSet<>();
        document.getMarkupStreamForTextNode(nodeToProcess).forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          state.openTags.computeIfAbsent(id, (k) -> toOpenTag(mw));
          state.closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
          if (discontinuousMarkupTextNodesToHandle.containsKey(id)) {
            discontinuousMarkupTextNodesToHandle.get(id).decrementAndGet();
          }
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
        toClose.forEach(markupId -> {
          String closeTag = state.closeTags.get(markupId).toString();
          closeTag = addSuspendPrefixIfRequired(closeTag, markupId, discontinuousMarkupTextNodesToHandle);
          tagmlBuilder.append(closeTag);
        });

        List<Long> toOpen = new ArrayList<>(relevantMarkupIds);
        toOpen.removeAll(state.openMarkupIds);
        toOpen.forEach(markupId -> {
          String openTag = state.openTags.get(markupId).toString();
          openTag = addResumePrefixIfRequired(openTag, markupId, discontinuousMarkupTextNodesToHandle);
          tagmlBuilder.append(openTag);
        });

        state.openMarkupIds.removeAll(toClose);
        state.openMarkupIds.addAll(toOpen);

        if (variationState.isBranchStartNode(nodeToProcess)) {
          // this node starts a new branch of the current textvariation
          stateRef.set(variationState.getStartState());
          variationState.incrementBranchesStarted();
        }
        TAGTextNodeDTO textNode = nodeToProcess.getTextNode();
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
            List<TAGTextNode> tmp = new ArrayList<>(nextTextNodes);
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
        LOG.info("nodesToProcess:{}", nodesToProcess.stream().map(TAGTextNode::getDbId).collect(toList()));
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

  private String addResumePrefixIfRequired(String openTag, Long markupId,
      final Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle) {
    if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
      int textNodesToHandle = discontinuousMarkupTextNodesToHandle.get(markupId).get();
      TAGMarkup markup = store.getMarkupWrapper(markupId);
      if (textNodesToHandle < markup.getTextNodeCount() - 1) {
        openTag = openTag.replace(OPEN_TAG_STARTCHAR, OPEN_TAG_STARTCHAR + RESUME_PREFIX);
      }
    }
    return openTag;
  }

  private String addSuspendPrefixIfRequired(String closeTag, final Long markupId,
      final Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle) {
    if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
      int textNodesToHandle = discontinuousMarkupTextNodesToHandle.get(markupId).get();
      if (textNodesToHandle > 0) {
        closeTag = closeTag.replace(CLOSE_TAG_STARTCHAR, CLOSE_TAG_STARTCHAR + SUSPEND_PREFIX);
      }
    }
    return closeTag;
  }

  private boolean needsDivider(final TAGTextNode nodeWrapper) {
    List<TAGTextNode> prevTextNodes = nodeWrapper.getPrevTextNodes();
    return prevTextNodes.size() == 1
        && prevTextNodes.get(0).isDivergence()
        && !prevTextNodes.get(0).getNextTextNodes().get(0).equals(nodeWrapper);
  }

  private StringBuilder toCloseTag(TAGMarkup markup) {
    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder(CLOSE_TAG_STARTCHAR).append(markup.getExtendedTag()).append(CLOSE_TAG_ENDCHAR);
  }

  private StringBuilder toOpenTag(TAGMarkup markup) {
    StringBuilder tagBuilder = new StringBuilder(OPEN_TAG_STARTCHAR).append(markup.getExtendedTag());
    markup.getAnnotationStream().forEach(a -> tagBuilder.append(" ").append(toTAGML(a)));
    return markup.isAnonymous()//
        ? tagBuilder.append(MILESTONE_TAG_ENDCHAR)//
        : tagBuilder.append(OPEN_TAG_ENDCHAR);
  }

  public StringBuilder toTAGML(final TAGAnnotation a) {
    return new StringBuilder();// TODO
  }

  private void logTextNode(final TAGTextNode nodeWrapper) {
    TAGTextNodeDTO textNode = nodeWrapper.getTextNode();
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
