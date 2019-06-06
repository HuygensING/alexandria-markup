package nl.knaw.huc.di.tag.tagml.exporter;

/*-
 * #%L
 * alexandria-markup-core
 * =======
 * Copyright (C) 2016 - 2019 HuC DI (KNAW)
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
import nl.knaw.huc.di.tag.TAGExporter;
import nl.knaw.huc.di.tag.tagml.TAGML;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationFactory;
import nl.knaw.huc.di.tag.tagml.importer.AnnotationInfo;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.dto.TAGDocument;
import nl.knaw.huygens.alexandria.storage.dto.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.dto.TAGTextNode;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.stream.Collectors.*;
import static nl.knaw.huc.di.tag.tagml.TAGML.*;
import static nl.knaw.huygens.alexandria.storage.dto.TAGElementWrapper.wrap;

// TODO: only output layer info on end-tag when needed
// TODO: only show layer info as defined in view
public class TAGMLExporter extends TAGExporter {
  private static final Logger LOG = LoggerFactory.getLogger(TAGMLExporter.class);
  private AnnotationFactory annotationFactory;

  public TAGMLExporter(TAGStore store) {
    super(store);
  }

  public TAGMLExporter(TAGStore store, TAGView view) {
    super(store, view);
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

  public String asTAGML(TAGDocument document) {
    annotationFactory = new AnnotationFactory(store, document.textGraph);
    Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle = new HashMap<>();
    store.getMarkupStream(document)
        .filter(TAGMarkup::isDiscontinuous)
        .forEach(mw -> discontinuousMarkupTextNodesToHandle.put(mw.getDbId(), new AtomicInteger(wrap(mw).getTextNodeCount())));

    Deque<TextVariationState> textVariationStates = new ArrayDeque<>();
    textVariationStates.push(new TextVariationState());

    Set<String> openLayers = new HashSet<>();
    openLayers.add(TAGML.DEFAULT_LAYER);

    Set<TAGTextNode> processedNodes = new HashSet<>();
    final AtomicReference<ExporterState> stateRef = new AtomicReference<>(new ExporterState());
    StringBuilder tagmlBuilder = new StringBuilder();
    store.getTextNodeStream(document).forEach(nodeToProcess -> {
//      logTextNode(nodeToProcess);
      if (!processedNodes.contains(nodeToProcess)) {
        ExporterState state = stateRef.get();
        Set<Long> markupIds = new LinkedHashSet<>();
        List<TAGMarkup> markupForTextNode = store.getMarkupStreamForTextNode(document, nodeToProcess)
            .collect(toList());
//        Collections.reverse(markupForTextNode);
        markupForTextNode.forEach(mw -> {
          Long id = mw.getDbId();
          markupIds.add(id);
          state.openTags.computeIfAbsent(id, (k) -> toOpenTag(document, mw, openLayers));
          state.closeTags.computeIfAbsent(id, (k) -> toCloseTag(document, mw));
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
        TAGTextNode textNode = nodeToProcess;
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
        .replace(BRANCHES_START + BRANCH_START, TAGML.DIVERGENCE)
        .replace(BRANCH_END + BRANCH_START, TAGML.DIVIDER)
        .replace(BRANCH_END + BRANCHES_END, TAGML.CONVERGENCE)
        ;
  }

  private String addResumePrefixIfRequired(String openTag, Long markupId,
      final Map<Long, AtomicInteger> discontinuousMarkupTextNodesToHandle) {
    if (discontinuousMarkupTextNodesToHandle.containsKey(markupId)) {
      int textNodesToHandle = discontinuousMarkupTextNodesToHandle.get(markupId).get();
      TAGMarkup markup = store.getMarkup(markupId);
      if (textNodesToHandle < wrap(markup).getTextNodeCount() - 1) {
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

  private boolean needsDivider(final TAGTextNode textNode) {
    // TODO: refactor
//    List<TAGTextNode> prevTextNodes = textNode.getPrevTextNodes();
//    return prevTextNodes.size() == 1
//        && prevTextNodes.get(0).isDivergence()
//        && !prevTextNodes.get(0).getNextTextNodes().get(0).equals(textNode);
    return false;
  }

  private StringBuilder toCloseTag(TAGDocument document, TAGMarkup markup) {
    String suspend = store.isSuspended(document, markup)
        ? TAGML.SUSPEND_PREFIX
        : "";

    return store.isAnonymous(document, markup)//
        ? new StringBuilder()//
        : new StringBuilder(CLOSE_TAG_STARTCHAR).append(suspend).append(wrap(markup).getExtendedTag()).append(CLOSE_TAG_ENDCHAR);
  }

  private StringBuilder toOpenTag(TAGDocument document, TAGMarkup markup, Set<String> openLayers) {
    String resume = store.isResumed(document, markup)
        ? TAGML.RESUME_PREFIX
        : "";

    Set<String> newLayers = new HashSet<>(markup.getLayers());
    newLayers.removeAll(openLayers);
    StringBuilder tagBuilder = new StringBuilder(OPEN_TAG_STARTCHAR)
        .append(resume).append(wrap(markup).getExtendedTag(newLayers));
    store.getAnnotationStream(markup).forEach(a -> tagBuilder.append(" ").append(toTAGML(a)));
    return store.isAnonymous(document, markup)//
        ? tagBuilder.append(MILESTONE_TAG_ENDCHAR)//
        : tagBuilder.append(OPEN_TAG_ENDCHAR);
  }

  public StringBuilder toTAGML(final AnnotationInfo a) {
    StringBuilder stringBuilder = new StringBuilder();
    if (a.hasName()) {
      stringBuilder
          .append(a.getName())
          .append("=");
    }
    switch (a.getType()) {
      case String:
        String stringValue = annotationFactory.getStringValue(a).replace("'", "\\'");
        stringBuilder.append("'")
            .append(stringValue)
            .append("'");
        break;

      case Number:
        Double numberValue = annotationFactory.getNumberValue(a);
        String asString = String.valueOf(numberValue).replaceFirst(".0$", "");
        stringBuilder.append(asString);
        break;

      case Boolean:
        Boolean booleanValue = annotationFactory.getBooleanValue(a);
        stringBuilder.append(booleanValue);
        break;

      case List:
        stringBuilder.append("[");
        List<AnnotationInfo> listValue = annotationFactory.getListValue(a);
        stringBuilder.append(listValue.stream()
            .map(this::toTAGML)
            .collect(joining(",")));
        stringBuilder.append("]");
        break;

      case Map:
        stringBuilder.append("{");
        List<AnnotationInfo> mapValue = annotationFactory.getMapValue(a);
        stringBuilder.append(mapValue.stream()
            .map(this::toTAGML)
            .collect(joining(" ")));
        stringBuilder.append("}");
        break;

      case Reference:
        String refValue = annotationFactory.getReferenceValue(a);
        stringBuilder.append(refValue);
        break;

      default:
        throw new RuntimeException("unhandled annotation type:" + a.getType());

    }
    return stringBuilder;
  }

  private String asValueString(final Object o) {
    if (o instanceof String) {
      return "'" + o + "'";
    }
    return o.toString();
  }

  private void logTextNode(final TAGTextNode textNode) {
    TAGTextNode dto = textNode;
    LOG.debug("\n");
    LOG.debug("TextNode(id={}, text=<{}>)",
        textNode.getDbId(),
        dto.getText()
    );
  }
}
