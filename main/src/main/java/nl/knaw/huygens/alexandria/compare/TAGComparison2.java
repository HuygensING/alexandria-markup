package nl.knaw.huygens.alexandria.compare;

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

import nl.knaw.huc.di.tag.tagml.MarkupPathFactory;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prioritised_xml_collation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class TAGComparison2 {
  private static final int MAX_MARKEDUP_TEXT_LENGTH = 20;
  Logger LOG = LoggerFactory.getLogger(TAGComparison2.class);
  private final List<String> diffLines = new ArrayList<>();
  private final List<String> mrDiffLines = new ArrayList<>();
  private final List<MarkupInfo>[] markupInfoLists;

  public TAGComparison2(TAGDocument original, TAGView tagView, TAGDocument edited, TAGStore store) {
    List<TAGToken> originalTokens = new Tokenizer(original, tagView)
        .getTAGTokens();
    List<TAGToken> originalTextTokens = originalTokens.stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(toList());
//    LOG.info("originalTextTokens={}", serializeTokens(originalTextTokens));

    List<TAGToken> editedTokens = new Tokenizer(edited, tagView)
        .getTAGTokens();
    List<TAGToken> editedTextTokens = editedTokens
        .stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(toList());
//    LOG.info("editedTextTokens={}", serializeTokens(editedTextTokens));

    SegmenterInterface textSegmenter = new ProvenanceAwareSegmenter(originalTextTokens, editedTextTokens);
    List<Segment> textSegments = new TypeAndContentAligner().alignTokens(originalTextTokens, editedTextTokens, textSegmenter);
    AtomicInteger rankCounter = new AtomicInteger();
    Map<Long, MarkupInfo> markupInfoMap1 = new HashMap<>();
    Map<Long, MarkupInfo> markupInfoMap2 = new HashMap<>();
    textSegments.forEach(segment -> {
      int rank = rankCounter.getAndIncrement();
      getTextNodeIdsForTokens(segment.tokensWa)
          .flatMap(original.getDTO()::getMarkupIdsForTextNodeId)
          .forEach(markupId -> {
            markupInfoMap1.putIfAbsent(markupId, new MarkupInfo(rank, rank));
            markupInfoMap1.get(markupId).setEndRank(rank);
          });
      getTextNodeIdsForTokens(segment.tokensWb)
          .flatMap(edited.getDTO()::getMarkupIdsForTextNodeId)
          .forEach(markupId -> {
            markupInfoMap2.putIfAbsent(markupId, new MarkupInfo(rank, rank));
            markupInfoMap2.get(markupId).setEndRank(rank);
          });
    });

    List<MarkupInfo>[] results = new ArrayList[2];

    MarkupPathFactory markupPathFactoryA = new MarkupPathFactory(original, store);
    List<MarkupInfo> listA = new ArrayList<>();
    markupInfoMap1.forEach((k, v) -> {
      TAGMarkup markup = store.getMarkup(k);
      v.setMarkup(markup);
      v.setMarkupPath(markupPathFactoryA.getPath(markup));
      listA.add(v);
    });
    listA.sort(BY_DESCENDING_SPAN_AND_ASCENDING_STARTRANK);
    results[0] = listA;

    MarkupPathFactory markupPathFactoryB = new MarkupPathFactory(edited, store);
    List<MarkupInfo> listB = new ArrayList<>();
    markupInfoMap2.forEach((k, v) -> {
      TAGMarkup markup = store.getMarkup(k);
      v.setMarkup(markup);
      v.setMarkupPath(markupPathFactoryB.getPath(markup));
      listB.add(v);
    });
    listB.sort(BY_DESCENDING_SPAN_AND_ASCENDING_STARTRANK);
    results[1] = listB;
    markupInfoLists = results;

    List<Pair<Integer, Integer>> potentialMatches = potentialMatches(listA, listB);

    List<Pair<Integer, Integer>> optimalMatches = new MyAStar(potentialMatches).matches();

    List<MarkupEdit> markupEdits = calculateMarkupEdits(listA, listB, optimalMatches);

    diffLines.addAll(toDiffLines(markupEdits, HR_DIFFPRINTER));
    mrDiffLines.addAll(toDiffLines(markupEdits, MR_DIFFPRINTER));
  }

  private List<Pair<Integer, Integer>> potentialMatches(final List<MarkupInfo> listA, final List<MarkupInfo> listB) {
    final List<Pair<Integer, Integer>> potentialMatches = new ArrayList<>();
    for (int i = 0; i < listA.size(); i++) {
      MarkupInfo markupA = listA.get(i);
      for (int j = 0; j < listB.size(); j++) {
        MarkupInfo markupB = listB.get(j);
        if (isMatch(markupA, markupB)) {
          potentialMatches.add(new ImmutablePair<>(i, j));
        }
      }
    }
    return potentialMatches;
  }

  private boolean isMatch(final MarkupInfo markupA, final MarkupInfo markupB) {
    return Objects.equals(markupA.getMarkup().getTag(), markupB.getMarkup().getTag());
  }

  public boolean hasDifferences() {
    return !diffLines.isEmpty();
  }

  public List<String> getDiffLines() {
    return diffLines;
  }

  public List<String> getMRDiffLines() {
    // these difflines should contain all the information needed to change the document graph, so:
    // markup: added, deleted, tagchanges
    // textnodes: added, deleted, content changes
    // edges: added, deleted

    return mrDiffLines;
  }

  private static final Comparator<MarkupInfo> BY_DESCENDING_SPAN_AND_ASCENDING_STARTRANK = Comparator.comparing(MarkupInfo::getSpan)
      .reversed()
      .thenComparing(MarkupInfo::getStartRank)
      .thenComparing(m -> m.getMarkup().getTag());

  public List<MarkupInfo>[] getMarkupInfoLists() {
    return markupInfoLists;
  }

  private Stream<Long> getTextNodeIdsForTokens(List<TAGToken> tokens) {
    return tokens.stream()
        .map(ExtendedTextToken.class::cast)
        .map(ExtendedTextToken::getTextNodeIds)
        .flatMap(List::stream)
        .distinct();
  }

  private static class MarkupSegment {
    List<MarkupInfo> original = new ArrayList<>();
    List<MarkupInfo> modified = new ArrayList<>();
    boolean isMatch = false;

    public boolean isEmpty() {
      return original.isEmpty() && modified.isEmpty();
    }
  }

  private List<MarkupEdit> calculateMarkupEdits(final List<MarkupInfo> listA, final List<MarkupInfo> listB, final List<Pair<Integer, Integer>> optimalMatches) {
    List<MarkupSegment> segments = new ArrayList<>();
    final List<MarkupEdit> markupEdits = new ArrayList<>();
    int indexA = 0;
    int indexB = 0;
    int matchesIndex = 0;

    while (matchesIndex < optimalMatches.size()) {
      Pair<Integer, Integer> pair = optimalMatches.get(matchesIndex);
      int matchIndexA = pair.getLeft();
      int matchIndexB = pair.getRight();
      MarkupSegment segment = new MarkupSegment();
      while (indexA < matchIndexA) {
        segment.original.add(listA.get(indexA++));
      }
      while (indexB < matchIndexB) {
        segment.modified.add(listB.get(indexB++));
      }
      if (!segment.isEmpty()) {
        segments.add(segment);
      }
      MarkupSegment matchingSegment = new MarkupSegment();
      matchingSegment.original.add(listA.get(indexA++));
      matchingSegment.modified.add(listB.get(indexB++));
      matchingSegment.isMatch = true;
      segments.add(matchingSegment);
      matchesIndex++;
    }

    MarkupSegment segment = new MarkupSegment();
    while (indexA < listA.size()) {
      segment.original.add(listA.get(indexA++));
    }
    while (indexB < listB.size()) {
      segment.modified.add(listB.get(indexB++));
    }
    if (!segment.isEmpty()) {
      segments.add(segment);
    }

    for (MarkupSegment s : segments) {
      if (s.isMatch) {
        MarkupInfo original = s.original.get(0);
        Set<String> originalLayers = original.getMarkup().getLayers();
        MarkupInfo modified = s.modified.get(0);
        Set<String> modifiedLayers = modified.getMarkup().getLayers();
        if (!originalLayers.equals(modifiedLayers)) {
          markupEdits.add(new LayerModification(original, modified));
        }
      } else {
//        for (MarkupInfo o : s.original) {
//          Optional<MarkupInfo> replacement = s.modified.stream()
//              .filter(mi -> mi.startRank == o.startRank && mi.endRank == o.endRank)
//              .findAny();
//          if (replacement.isPresent()) {
//            MarkupInfo modified = replacement.get();
//            markupEdits.add(new MarkupModification(o, modified));
//            s.modified.remove(modified);
//          } else {
//            markupEdits.add(new MarkupDeletion(o));
//          }
//        }
//        for (MarkupInfo m : s.modified) {
//          markupEdits.add(new MarkupAddition(m));
//        }
        if (s.original.isEmpty()) {
          for (MarkupInfo m : s.modified) {
            markupEdits.add(new MarkupAddition(m));
          }
        } else if (s.modified.isEmpty()) {
          for (MarkupInfo o : s.original) {
            markupEdits.add(new MarkupDeletion(o));
          }

        } else {
          markupEdits.add(new MarkupModification(s.original, s.modified));
        }
      }
    }

    return markupEdits;
  }

  private Collection<? extends String> toDiffLines(final List<MarkupEdit> markupEdits, final DiffPrinter diffPrinter) {
    return markupEdits.stream()
        .map(me -> {
          if (me instanceof MarkupAddition) {
            return diffPrinter.addition.apply(((MarkupAddition) me).markupInfo);
          } else if (me instanceof MarkupDeletion) {
            return diffPrinter.deletion.apply(((MarkupDeletion) me).markupInfo);
          } else if (me instanceof MarkupModification) {
            MarkupModification mm = (MarkupModification) me;
            return diffPrinter.modification.apply(mm.original, mm.modified);
          } else if (me instanceof LayerModification) {
            LayerModification lm = (LayerModification) me;
            return diffPrinter.layermodification.apply(lm.original, lm.modified);
          }
          return null;
        })
        .collect(toList());
  }

  static class DiffPrinter {
    Function<MarkupInfo, String> addition;
    Function<MarkupInfo, String> deletion;
    BiFunction<List<MarkupInfo>, List<MarkupInfo>, String> modification;
    BiFunction<MarkupInfo, MarkupInfo, String> layermodification;

    DiffPrinter setAddition(Function<MarkupInfo, String> addition) {
      this.addition = addition;
      return this;
    }

    DiffPrinter setDeletion(Function<MarkupInfo, String> deletion) {
      this.deletion = deletion;
      return this;
    }

    DiffPrinter setModification(BiFunction<List<MarkupInfo>, List<MarkupInfo>, String> modification) {
      this.modification = modification;
      return this;
    }

    DiffPrinter setLayerModification(BiFunction<MarkupInfo, MarkupInfo, String> layerModification) {
      this.layermodification = layerModification;
      return this;
    }
  }

  static DiffPrinter HR_DIFFPRINTER = new DiffPrinter()
      .setAddition(markupInfo -> String.format("add %s", markupInfoString(markupInfo)))
      .setDeletion(markupInfo -> String.format("del %s", markupInfoString(markupInfo)))
      .setModification((markupInfoA, markupInfoB) ->
          String.format("replace {%s} -> {%s}",
              markupInfoA.stream().map(TAGComparison2::markupInfoString).collect(joining(",")),
              markupInfoB.stream().map(TAGComparison2::markupInfoString).collect(joining(",")))
      )
      .setLayerModification((markupInfoA, markupInfoB) -> String.format("layeridentifier change %s -> %s",
          markupInfoString(markupInfoA), markupInfoString(markupInfoB)));

  public static String markupInfoString(MarkupInfo markupInfo) {
    return String.format("[%s]", markupInfo.getMarkupPath());
  }

  static DiffPrinter MR_DIFFPRINTER = new DiffPrinter()
      .setAddition(markupInfo -> String.format("+[%s]", markupInfo.markup.getExtendedTag()))
      .setDeletion(markupInfo -> String.format("-[%s]", markupInfo.markup.getDbId()))
      .setModification((markupInfoA, markupInfoB) -> String.format("~[%s,%s]",
          markupInfoA.get(0).markup.getDbId(), markupInfoB.get(0).markup.getExtendedTag()
      ))
      .setLayerModification((markupInfoA, markupInfoB) -> String.format("~[%s,%s]",
          markupInfoA.markup.getDbId(), markupInfoB.markup.getExtendedTag()
      ));

  private String toString(MarkupInfo markupInfo) {
    TAGMarkup markup = markupInfo.markup;
    String markedUpText = markup
        .getTextNodeStream()
        .map(TAGTextNode::getText)
        .collect(joining());
    int length = markedUpText.length();
    if (length > MAX_MARKEDUP_TEXT_LENGTH) {
      int half = (length - 5) / 2;
      markedUpText = markedUpText.substring(0, half) + " ... " + markedUpText.substring(half + 5);
    }
    String extendedTag = markup.getExtendedTag();
    return String.format("[%s>%s<%s]",
        extendedTag,
        markedUpText,
        extendedTag);
  }

  private void removeDeterminedPairs(final boolean[] determinedInA, final boolean[] determinedInB, final List<Pair<Integer, Integer>> potentialReplacements) {
    List<Pair<Integer, Integer>> potentialReplacementsWithDetermined = potentialReplacements.stream()
        .filter(p -> determinedInA[p.getLeft()] || determinedInB[p.getRight()])
        .collect(toList());
    potentialReplacements.removeAll(potentialReplacementsWithDetermined);
  }

  // optimal = minimum number of steps? no: maximum number of matches
  public List<String> diffMarkupInfo(final List<MarkupInfo>[] markupInfoLists, final DiffPrinter diffPrinter) {
    // this current algorithm does not
    final List<String> diff = new ArrayList<>();
    List<MarkupInfo> markupInfoListA = markupInfoLists[0];
    List<MarkupInfo> markupInfoListB = markupInfoLists[1];
    List<Pair<Integer, Integer>> unchanged = new ArrayList<>();
    boolean[] determinedInA = new boolean[markupInfoListA.size()];
    boolean[] determinedInB = new boolean[markupInfoListB.size()];
    final List<Pair<Integer, Integer>> potentialReplacements = new ArrayList<>();

    // determine matches
    for (int i = 0; i < markupInfoListA.size(); i++) {
      MarkupInfo markupInfoA = markupInfoListA.get(i);
      for (int j = 0; j < markupInfoListB.size(); j++) {
        if (!determinedInB[j]) {
          MarkupInfo markupInfoB = markupInfoListB.get(j);
          boolean sameName = markupInfoA.markup.getTag().equals(markupInfoB.markup.getTag());
          boolean sameSpan = markupInfoA.getSpan().equals(markupInfoB.getSpan());
          boolean sameStartRank = markupInfoA.getStartRank() == markupInfoB.getStartRank();
          LOG.info("[{},{}]: {},{},{}", i, j, sameName, sameSpan, sameStartRank);
          if (sameName && sameSpan && sameStartRank) {
            unchanged.add(new ImmutablePair(i, j));
            determinedInA[i] = true;
            determinedInB[j] = true;
            break;
          } else if (sameSpan && sameStartRank) {
            potentialReplacements.add(new ImmutablePair(i, j));
          }
        }
      }
    }
//    removeDeterminedPairs(determinedInA, determinedInB, potentialReplacements);

    for (int i = 0; i < determinedInA.length; i++) {
      if (!determinedInA[i]) {
        // check for replacement
        final int finalI = i;
        List<Pair<Integer, Integer>> matchingPotentialReplacements = potentialReplacements.stream()
            .filter(p -> p.getLeft() == finalI)
            .collect(toList());
        if (!matchingPotentialReplacements.isEmpty()) {
          potentialReplacements.removeAll(matchingPotentialReplacements);
          Pair<Integer, Integer> replacement = matchingPotentialReplacements.get(0);
          MarkupInfo markupInfoA = markupInfoListA.get(i);
          MarkupInfo markupInfoB = markupInfoListB.get(replacement.getRight());
//          String markupA = toString(markupInfoA);
//          String markupB = toString(markupInfoB);
//          diff.add(String.format("%s (%d-%d) replaced by %s (%d-%d)",
//              markupA, markupInfoA.getStartRank(), markupInfoA.getEndRank(),
//              markupB, markupInfoB.getStartRank(), markupInfoB.getEndRank()
//              )

          diff.add(diffPrinter.modification.apply(singletonList(markupInfoA), singletonList(markupInfoB)));
          determinedInA[i] = true;
          determinedInB[replacement.getRight()] = true;

        } else {
          // otherwise, deletion
          MarkupInfo markupInfo = markupInfoListA.get(i);
          diff.add(diffPrinter.deletion.apply(markupInfo));
        }
      }
    }
    for (int i = 0; i < determinedInB.length; i++) {
      if (!determinedInB[i]) {
        MarkupInfo markupInfo = markupInfoListB.get(i);
        diff.add(diffPrinter.addition.apply(markupInfo));
      }
    }
    return diff;
  }

  private String serializeTokens(final List<TAGToken> textTokens) {
    return textTokens.stream()
        .map(t -> "[" + t.toString().replaceAll(" ", "_") + "]")
        .collect(joining(", "));
  }
//  private boolean isMarkupToken(final TAGToken tagToken) {
//    return tagToken instanceof MarkupOpenToken
//        || tagToken instanceof MarkupCloseToken;
//  }

}
