package nl.knaw.huc.di.tag;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2020 HuC DI (KNAW)
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
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
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
import static nl.knaw.huc.di.tag.tagml.TAGML.*;

public class TAGTraverser {
  private final Set<String> relevantLayers;
  private final TAGStore store;
  private final TAGView view;
  private final TAGDocument document;
  private final Set<TAGTextNode> processedNodes = new HashSet<>();
  private final HashMap<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle = new HashMap<>();
  private final Deque<TextVariationState> textVariationStates = new ArrayDeque<>();

  public TAGTraverser(final TAGStore store, final TAGView view, final TAGDocument document) {
    this.store = store;
    this.view = view;
    this.document = document;
//    final AnnotationFactory annotationFactory = new AnnotationFactory(store, document.getDTO().textGraph);
    document.getMarkupStream()
        .filter(TAGMarkup::isDiscontinuous)
        .forEach(mw -> discontinuousMarkupTextNodesToHandle.put(mw.getDbId(), new AtomicInteger(mw.getTextNodeCount())));
    textVariationStates.push(new TextVariationState());
    Set<String> layerNames = document.getLayerNames();
    relevantLayers = view.filterRelevantLayers(layerNames);
  }

  public void accept(final TAGVisitor tagVisitor) {
    AnnotationFactory annotationFactory = new AnnotationFactory(store, document.getDTO().textGraph);
    tagVisitor.setRelevantLayers(relevantLayers);
    tagVisitor.enterDocument(document);
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
//        Collections.reverse(markupStreamForTextNode);
        markupStreamForTextNode.forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          state.openTags.computeIfAbsent(id, (k) -> toOpenTag(mw, openLayers, tagVisitor));
          state.closeTags.computeIfAbsent(id, (k) -> toCloseTag(mw));
          openLayers.addAll(mw.getLayers());
          if (discontinuousMarkupTextNodesToHandle.containsKey(id)) {
            discontinuousMarkupTextNodesToHandle.get(id).decrementAndGet();
          }
        });
        Set<Long> relevantMarkupIds = view.filterRelevantMarkup(markupIds);

//        if (needsDivider(nodeToProcess)) {
//          tagmlBuilder.append(DIVIDER);
//        }

        TextVariationState variationState = textVariationStates.peek();
        if (variationState.isFirstNodeAfterConvergence(nodeToProcess)) {
          tagVisitor.exitTextVariation();
//          tagmlBuilder.append(CONVERGENCE);
          textVariationStates.pop();
          variationState = textVariationStates.peek();
        }

        List<Long> toClose = new ArrayList<>(state.openMarkupIds);
        toClose.removeAll(relevantMarkupIds);
        Collections.reverse(toClose);
        toClose.forEach(markupId -> {
          String closeTag = state.closeTags.get(markupId).toString();
          closeTag = addSuspendPrefixIfRequired(closeTag, markupId, discontinuousMarkupTextNodesToHandle);
          final TAGMarkup markup = store.getMarkup(markupId);
          tagVisitor.exitCloseTag(markup);
//          tagmlBuilder.append(closeTag);
        });

        List<Long> toOpen = new ArrayList<>(relevantMarkupIds);
        toOpen.removeAll(state.openMarkupIds);
        toOpen.forEach(markupId -> {
          final TAGMarkup markup = store.getMarkup(markupId);
          tagVisitor.enterOpenTag(markup);
          String openTag = state.openTags.get(markupId).toString();
          openTag = addResumePrefixIfRequired(openTag, markupId, discontinuousMarkupTextNodesToHandle);
          markup.getAnnotationStream().forEach(a -> {
            String value = serializeAnnotation(annotationFactory, a, tagVisitor);
            tagVisitor.addAnnotation(value);
          });
          tagVisitor.exitOpenTag(markup);
//          tagmlBuilder.append(openTag);
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
//        String escapedText = variationState.inVariation()
//            ? TAGML.escapeVariantText(content)
//            : TAGML.escapeRegularText(content);
        tagVisitor.exitText(content, variationState.inVariation());
        processedNodes.add(nodeToProcess);
        state.lastTextNodeId = nodeToProcess.getDbId();
//        LOG.debug("TAGML={}\n", tagmlBuilder);
      }
    });
    while (!textVariationStates.isEmpty()) {
      TextVariationState textVariationState = textVariationStates.pop();
      if (textVariationState.inVariation()) {
        tagVisitor.enterTextVariation();
//        tagmlBuilder.append(CONVERGENCE);
      }
    }
    final ExporterState state = stateRef.get();
    state.openMarkupIds.descendingIterator()//
        .forEachRemaining(markupId -> tagVisitor.exitCloseTag(store.getMarkup(markupId)));
//        .forEachRemaining(markupId -> tagmlBuilder.append(state.closeTags.get(markupId)));
    tagVisitor.exitDocument(document);
  }

  private String serializeAnnotation(AnnotationFactory annotationFactory, AnnotationInfo a, TAGVisitor tagVisitor) {
    StringBuilder stringBuilder = new StringBuilder();
    if (a.hasName()) {
      String annotationAssigner = tagVisitor.serializeAnnotationAssigner(a.getName());
      stringBuilder.append(annotationAssigner);
    }
    String value;
    switch (a.getType()) {
      case String:
        String stringValue = annotationFactory.getStringValue(a);
        value = tagVisitor.serializeStringAnnotationValue(stringValue);
        break;

      case Number:
        Double numberValue = annotationFactory.getNumberValue(a);
        value = tagVisitor.serializeNumberAnnotationValue(numberValue);
        break;

      case Boolean:
        Boolean booleanValue = annotationFactory.getBooleanValue(a);
        value = tagVisitor.serializeBooleanAnnotationValue(booleanValue);
        break;

      case List:
        List<AnnotationInfo> listValue = annotationFactory.getListValue(a);
        List<String> serializedItems = listValue.stream()
            .map(ai -> serializeAnnotation(annotationFactory, ai, tagVisitor))
            .collect(toList());
        value = tagVisitor.serializeListAnnotationValue(serializedItems);
        break;

      case Map:
        List<AnnotationInfo> mapValue = annotationFactory.getMapValue(a);
        List<String> serializedMapItems = mapValue.stream()
            .map(ai -> serializeAnnotation(annotationFactory, ai, tagVisitor))
            .collect(toList());
        value = tagVisitor.serializeMapAnnotationValue(serializedMapItems);
        break;

      default:
        throw new RuntimeException("unhandled annotation type:" + a.getType());

    }
    return stringBuilder.append(value).toString();
  }

  static class ExporterState {
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

  private StringBuilder toCloseTag(TAGMarkup markup) {
    String suspend = markup.isSuspended()
        ? TAGML.SUSPEND_PREFIX
        : "";

    return markup.isAnonymous()//
        ? new StringBuilder()//
        : new StringBuilder(CLOSE_TAG_STARTCHAR).append(suspend).append(markup.getExtendedTag()).append(CLOSE_TAG_ENDCHAR);
  }

  private StringBuilder toOpenTag(TAGMarkup markup, Set<String> openLayers, final TAGVisitor tagVisitor) {
    String resume = markup.isResumed()
        ? TAGML.RESUME_PREFIX
        : "";

    Set<String> newLayers = new HashSet<>(markup.getLayers());
    newLayers.removeAll(openLayers);
    StringBuilder tagBuilder = new StringBuilder(OPEN_TAG_STARTCHAR)
        .append(resume).append(markup.getExtendedTag(newLayers));
    return markup.isAnonymous()//
        ? tagBuilder.append(MILESTONE_TAG_ENDCHAR)//
        : tagBuilder.append(OPEN_TAG_ENDCHAR);
  }

  private String addResumePrefixIfRequired(String openTag, Long markupId,
      final Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle) {
    if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
      int textNodesToHandle = discontinuousMarkupTextNodesToHandle.get(markupId).get();
      TAGMarkup markup = store.getMarkup(markupId);
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

}
