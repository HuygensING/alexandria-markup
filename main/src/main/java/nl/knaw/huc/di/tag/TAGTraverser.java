package nl.knaw.huc.di.tag;

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
import nl.knaw.huc.di.tag.tagml.importer.AnnotationFactory;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNodeDTO;
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static nl.knaw.huc.di.tag.tagml.TAGML.CONVERGENCE;
import static nl.knaw.huc.di.tag.tagml.TAGML.DIVIDER;

public class TAGTraverser {
  private final AnnotationFactory annotationFactory;
  private final TAGDocument document;
  private final Set<TAGTextNode> processedNodes = new HashSet<>();

  public TAGTraverser(final TAGStore store, final TAGView view, final TAGDocument document) {
    this.document = document;
    annotationFactory = new AnnotationFactory(store, document.getDTO().textGraph);
    Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle = new HashMap<>();
    document.getMarkupStream()
        .filter(TAGMarkup::isDiscontinuous)
        .forEach(mw -> discontinuousMarkupTextNodesToHandle.put(mw.getDbId(), new AtomicInteger(mw.getTextNodeCount())));
    Deque<TextVariationState> textVariationStates = new ArrayDeque<>();
    textVariationStates.push(new TextVariationState());
  }

  public void accept(final TAGVisitor tagVisitor) {
    Set<String> openLayers = new HashSet<>();
    openLayers.add(TAGML.DEFAULT_LAYER);
    final AtomicReference<ExporterState> stateRef = new AtomicReference<>(new ExporterState());
    document.getTextNodeStream().forEach(nodeToProcess -> {
//      logTextNode(nodeToProcess);
      if (!processedNodes.contains(nodeToProcess)) {
        ExporterState state = stateRef.get();
        Set<Long> markupIds = new LinkedHashSet<>();
        List<TAGMarkup> markupStreamForTextNode = document.getMarkupStreamForTextNode(nodeToProcess)
            .collect(toList());
        Collections.reverse(markupStreamForTextNode);
        markupStreamForTextNode.forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          state.openTags.computeIfAbsent(id, (k) -> toOpenTag(mw, openLayers));
          state.closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
          openLayers.addAll(mw.getLayers());
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
        TAGTextNodeDTO textNode = nodeToProcess.getDTO();
        String content = nodeToProcess.getText();
        String escapedText = variationState.inVariation()
            ? TAGML.escapeVariantText(content)
            : TAGML.escapeRegularText(content);
        tagmlBuilder.append(escapedText);
        processedNodes.add(nodeToProcess);
        state.lastTextNodeId = nodeToProcess.getDbId();
//        LOG.debug("TAGML={}\n", tagmlBuilder);
      }
    });
    while (!textVariationStates.isEmpty()) {
      TextVariationState textVariationState = textVariationStates.pop();
      if (textVariationState.inVariation()) {
        tagmlBuilder.append(CONVERGENCE);
      }
    }
    final ExporterState state = stateRef.get();
    state.openMarkupIds.descendingIterator()//
        .forEachRemaining(markupId -> tagmlBuilder.append(state.closeTags.get(markupId)));
    return tagmlBuilder.toString()
        .replace("[:branches>[:branch>", TAGML.DIVERGENCE)
        .replace("<:branch][:branch>", TAGML.DIVIDER)
        .replace("<:branch]<:branches]", TAGML.CONVERGENCE)
        ;

  }

  class ExporterState {
    Deque<Long> openMarkupIds = new ArrayDeque<>();
    Map<Long, StringBuilder> openTags = new LinkedHashMap<>();
    Map<Long, StringBuilder> closeTags = new LinkedHashMap<>();
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

}
