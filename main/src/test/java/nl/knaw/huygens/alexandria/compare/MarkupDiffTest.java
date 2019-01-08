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
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import prioritised_xml_collation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class MarkupDiffTest extends AlexandriaBaseStoreTest {
  Logger LOG = LoggerFactory.getLogger(MarkupDiffTest.class);

  @Test
  public void testMarkupDiff0() {
    String originText = "[TAGML|+M>\n" +
        "[text|M>\n" +
        "[l|M>\n" +
        "Une [del|M>jolie<del][add|M>belle<add] main de femme, élégante et fine,<l][l|M> malgré l'agrandissement du close-up.\n" +
        "<l]\n" +
        "<text]<TAGML]\n";
    String editedText = "[TAGML|+N>\n" +
        "[text|N>\n" +
        "[s|N>Une belle main de femme, élégante et fine.<s][s|N>Malgré l'agrandissement du close-up.\n" +
        "<s]\n" +
        "<text]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[l|M] replaced by [s|N]", "[l|M] replaced by [s|N]");
  }

  @Test
  public void testMarkupDeletion() {
    String originText = "[TAGML>A simple [del>short<del] text<TAGML]\n";
    String editedText = "[TAGML>A simple text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[del](2-2) deleted");
  }

  @Test
  public void testMarkupAddition() {
    String originText = "[TAGML>A simple text<TAGML]\n";
    String editedText = "[TAGML>A simple [add>short<add] text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[add](2-2) added");
  }

  @Test
  public void testMarkupReplacement() {
    String originText = "[TAGML>A [a>simple<a] text<TAGML]\n";
    String editedText = "[TAGML>A [b>simple<b] text<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[a](1-1) replaced by [b](1-1)");
  }

  @Test
  public void testMarkupSplit() {
    String originText = "[TAGML>[l>Sentence one. Sentence two.<l]<TAGML]\n";
    String editedText = "[TAGML>[l>Sentence one.<l][l>Sentence two.<l]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("[l](1,2) split in {[l](1,1),[l](2,1)}");
  }

  @Test
  public void testMarkupJoin() {
    String originText = "[TAGML>[l>Sentence one.<l][l>Sentence two.<l]<TAGML]\n";
    String editedText = "[TAGML>[l>Sentence one. Sentence two.<l]<TAGML]\n";
    List<String> markupInfoDiffs = getMarkupDiffs(originText, editedText);
    assertThat(markupInfoDiffs).containsExactly("{[l](1,1),[l](2,1)} joined to [l](1,2)");
  }

  private List<String> getMarkupDiffs(final String originText, final String editedText) {
    visualizeDiff("A", originText, "B", editedText);
    List<MarkupInfo>[] markupInfoLists = doDiff(originText, editedText);
    assertThat(markupInfoLists).hasSize(2);
    for (int i = 0; i < 2; i++) {
      for (MarkupInfo mi : markupInfoLists[i]) {
        LOG.info("{}: {}", i, mi);
      }
    }
    List<String> diffMarkupInfo = diffMarkupInfo(markupInfoLists);
    LOG.info("{}", diffMarkupInfo);
    return diffMarkupInfo;
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

  private List<MarkupInfo>[] doDiff(String tagmlA, String tagmlB) {
    return runInStoreTransaction(store -> {
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
    });
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

  private List<String> diffMarkupInfo(final List<MarkupInfo>[] markupInfoLists) {
    final List<String> diff = new ArrayList<>();
    List<MarkupInfo> markupInfoListA = markupInfoLists[0];
    List<MarkupInfo> markupInfoListB = markupInfoLists[1];
    List<Pair<Integer, Integer>> unchanged = new ArrayList<>();
    boolean[] determinedInA = new boolean[markupInfoListA.size()];
    boolean[] determinedInB = new boolean[markupInfoListB.size()];

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
        }
      }
    }

    for (int i = 0; i < determinedInA.length; i++) {
      if (!determinedInA[i]) {
        MarkupInfo markupInfo = markupInfoListA.get(i);
        diff.add(String.format("[%s](%d-%d) deleted", markupInfo.markup.getExtendedTag(), markupInfo.getStartRank(), markupInfo.getEndRank()));
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

  private void visualizeDiff(final String witness1, final String tagml1, final String witness2, final String tagml2) {
    LOG.info("{}:\n{}", witness1, tagml1);
    LOG.info("{}:\n{}", witness2, tagml2);
    runInStoreTransaction(store -> {
      TAGMLImporter importer = new TAGMLImporter(store);
      TAGDocument original = importer.importTAGML(tagml1.replace("\n", ""));
      TAGDocument edited = importer.importTAGML(tagml2.replace("\n", ""));
      Set<String> none = Collections.EMPTY_SET;
      TAGView allTags = new TAGView(store).setMarkupToExclude(none);

      DiffVisualizer visualizer = new AsHTMLDiffVisualizer();
//      DiffVisualizer visualizer = new AsDOTDiffVisualizer();
      new VariantGraphVisualizer(visualizer)
          .visualizeVariation(witness1, original, witness2, edited, allTags);
      String result = visualizer.getResult();
      LOG.info("result=\n" +
          "------8<---------------------------------------\n" +
          "{}\n" +
          "------8<---------------------------------------\n", result);
    });
  }

}

