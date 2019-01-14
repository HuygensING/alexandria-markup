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
import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prioritised_xml_collation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class MarkupAwareDiffer {
  Logger LOG = LoggerFactory.getLogger(MarkupAwareDiffer.class);

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

  public List<MarkupInfo>[] doDiff(TAGStore store, String tagmlA, String tagmlB) {
    TAGMLImporter importer = new TAGMLImporter(store);
    TAGDocument original = importer.importTAGML(tagmlA.replace("\n", ""));
    TAGDocument edited = importer.importTAGML(tagmlB.replace("\n", ""));
    Set<String> none = Collections.EMPTY_SET;
    TAGView allTags = new TAGView(store).setMarkupToExclude(none);
    List<TAGToken> originalTokens = new Tokenizer(original, allTags)
        .getTAGTokens();
    List<TAGToken> originalTextTokens = originalTokens.stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(toList());
    LOG.info("originalTextTokens={}", serializeTokens(originalTextTokens));

    List<TAGToken> editedTokens = new Tokenizer(edited, allTags)
        .getTAGTokens();
    List<TAGToken> editedTextTokens = editedTokens
        .stream()
        .filter(ExtendedTextToken.class::isInstance)
        .collect(toList());
    LOG.info("editedTextTokens={}", serializeTokens(editedTextTokens));

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
    return results;
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

  private boolean isMarkupToken(final TAGToken tagToken) {
    return tagToken instanceof MarkupOpenToken
        || tagToken instanceof MarkupCloseToken;
  }

  public List<String> diffMarkupInfo(final List<MarkupInfo>[] markupInfoLists) {
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
      Pair<Integer, Integer> tentativeMatch = null;
      Integer tentativeDeletion = i;

      for (int j = 0; j < markupInfoListB.size(); j++) {
        Integer tentativeAddition = j;
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
          diff.add(String.format("[%s](%d-%d) replaced by [%s](%d-%d)",
              markupInfoA.markup.getExtendedTag(), markupInfoA.getStartRank(), markupInfoA.getEndRank(),
              markupInfoB.markup.getExtendedTag(), markupInfoB.getStartRank(), markupInfoB.getEndRank()
              )
          );
          determinedInA[i] = true;
          determinedInB[replacement.getRight()] = true;

        } else {
          // otherwise, deletion
          MarkupInfo markupInfo = markupInfoListA.get(i);
          diff.add(String.format("[%s](%d-%d) deleted", markupInfo.markup.getExtendedTag(), markupInfo.getStartRank(), markupInfo.getEndRank()));
        }
      }
    }
    for (int i = 0; i < determinedInB.length; i++) {
      if (!determinedInB[i]) {
        MarkupInfo markupInfo = markupInfoListB.get(i);
        diff.add(String.format("[%s](%d-%d) added", markupInfo.markup.getExtendedTag(), markupInfo.getStartRank(), markupInfo.getEndRank()));
      }
    }
    return diff;
  }

  private void removeDeterminedPairs(final boolean[] determinedInA, final boolean[] determinedInB, final List<Pair<Integer, Integer>> potentialReplacements) {
    List<Pair<Integer, Integer>> potentialReplacementsWithDetermined = potentialReplacements.stream()
        .filter(p -> determinedInA[p.getLeft()] || determinedInB[p.getRight()])
        .collect(toList());
    potentialReplacements.removeAll(potentialReplacementsWithDetermined);
  }

}
