package nl.knaw.huygens.alexandria.compare;

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

import static java.lang.String.format;
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
      int rank = rankCounter.incrementAndGet();
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
    List<MarkupInfo> listA = new ArrayList<>();
    markupInfoMap1.forEach((k, v) -> {
      v.setMarkup(store.getMarkup(k));
      listA.add(v);
    });
    listA.sort(BY_DESCENDING_SPAN_AND_ASCENDING_STARTRANK);
    results[0] = listA;
    List<MarkupInfo> listB = new ArrayList<>();
    markupInfoMap2.forEach((k, v) -> {
      v.setMarkup(store.getMarkup(k));
      listB.add(v);
    });
    listB.sort(BY_DESCENDING_SPAN_AND_ASCENDING_STARTRANK);
    results[1] = listB;
    markupInfoLists = results;
    diffLines.addAll(diffMarkupInfo(markupInfoLists, HR_DIFFPRINTER));
    mrDiffLines.addAll(diffMarkupInfo(markupInfoLists, MR_DIFFPRINTER));
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

  public class MarkupInfo {
    int startRank;
    int endRank;
    private TAGMarkup markup;

    public MarkupInfo(int startRank, int endRank) {
      this.startRank = startRank;
      this.endRank = endRank;
    }

    public int getStartRank() {
      return startRank;
    }

    public void setEndRank(int endRank) {
      this.endRank = endRank;
    }

    public int getEndRank() {
      return endRank;
    }

    @Override
    public String toString() {
      return format("%s span=%d, startRank=%d, endRank=%d", markup, getSpan(), startRank, endRank);
    }

    private Integer getSpan() {
      return endRank - startRank + 1;
    }

    public void setMarkup(TAGMarkup markup) {
      this.markup = markup;
    }

    public TAGMarkup getMarkup() {
      return markup;
    }
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

  private String serializeTokens(final List<TAGToken> textTokens) {
    return textTokens.stream()
        .map(t -> "[" + t.toString().replaceAll(" ", "_") + "]")
        .collect(joining(", "));
  }

//  private boolean isMarkupToken(final TAGToken tagToken) {
//    return tagToken instanceof MarkupOpenToken
//        || tagToken instanceof MarkupCloseToken;
//  }

  public List<String> diffMarkupInfo(final List<MarkupInfo>[] markupInfoLists, final DiffPrinter diffPrinter) {
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
          }
          if (sameSpan && sameStartRank) {
            potentialReplacements.add(new ImmutablePair(i, j));
          }
        }
      }
    }
    removeDeterminedPairs(determinedInA, determinedInB, potentialReplacements);

    for (int i = 0; i < determinedInA.length; i++) {
      if (!determinedInA[i]) {
        // check for replacement
        final int finalI = i;
        List<Pair<Integer, Integer>> matchingPotentialReplacements = potentialReplacements.stream()
            .filter(p -> p.getLeft() == finalI)
            .collect(toList());
        potentialReplacements.removeAll(matchingPotentialReplacements);
        if (!matchingPotentialReplacements.isEmpty()) {
          Pair<Integer, Integer> replacement = matchingPotentialReplacements.get(0);
          MarkupInfo markupInfoA = markupInfoListA.get(i);
          MarkupInfo markupInfoB = markupInfoListB.get(replacement.getRight());
//          String markupA = toString(markupInfoA);
//          String markupB = toString(markupInfoB);
//          diff.add(String.format("%s (%d-%d) replaced by %s (%d-%d)",
//              markupA, markupInfoA.getStartRank(), markupInfoA.getEndRank(),
//              markupB, markupInfoB.getStartRank(), markupInfoB.getEndRank()
//              )

          diff.add(diffPrinter.modification.apply(markupInfoA, markupInfoB));
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

  static class DiffPrinter {
    Function<MarkupInfo, String> addition;
    Function<MarkupInfo, String> deletion;
    BiFunction<MarkupInfo, MarkupInfo, String> modification;

    DiffPrinter setAddition(Function<MarkupInfo, String> addition) {
      this.addition = addition;
      return this;
    }

    DiffPrinter setDeletion(Function<MarkupInfo, String> deletion) {
      this.deletion = deletion;
      return this;
    }

    DiffPrinter setModification(BiFunction<MarkupInfo, MarkupInfo, String> modification) {
      this.modification = modification;
      return this;
    }
  }

  static DiffPrinter HR_DIFFPRINTER = new DiffPrinter()
      .setAddition(markupInfo -> String.format("[%s](%d-%d) added",
          markupInfo.markup.getExtendedTag(), markupInfo.getStartRank(), markupInfo.getEndRank()))
      .setDeletion(markupInfo -> String.format("[%s](%d-%d) deleted",
          markupInfo.markup.getExtendedTag(), markupInfo.getStartRank(), markupInfo.getEndRank()))
      .setModification((markupInfoA, markupInfoB) -> String.format("[%s](%d-%d) replaced by [%s](%d-%d)",
          markupInfoA.markup.getExtendedTag(), markupInfoA.getStartRank(), markupInfoA.getEndRank(),
          markupInfoB.markup.getExtendedTag(), markupInfoB.getStartRank(), markupInfoB.getEndRank()
      ));

  static DiffPrinter MR_DIFFPRINTER = new DiffPrinter()
      .setAddition(markupInfo -> String.format("+[%s]", markupInfo.markup.getExtendedTag()))
      .setDeletion(markupInfo -> String.format("-[%s]", markupInfo.markup.getDbId()))
      .setModification((markupInfoA, markupInfoB) -> String.format("~[%s,%s]",
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

}
