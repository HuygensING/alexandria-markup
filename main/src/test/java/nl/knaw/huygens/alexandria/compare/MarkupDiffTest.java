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
  public void testMarkupDiff() {
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
    List<MarkupInfo>[] markupInfoLists = doDiff(originText, editedText);
    assertThat(markupInfoLists).hasSize(2);
    for (int i = 0; i < 2; i++) {
      for (MarkupInfo mi : markupInfoLists[i]) {
        LOG.info("{}: {}", i, mi);
      }
    }

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

}

