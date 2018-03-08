package nl.knaw.huygens.alexandria.compare;

/*-
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 Huygens ING (KNAW)
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

import nl.knaw.huygens.alexandria.storage.wrappers.DocumentWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.MarkupWrapper;
import nl.knaw.huygens.alexandria.storage.wrappers.TextNodeWrapper;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.apache.commons.lang3.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.StreamUtil.stream;

class Tokenizer {
  public static final Pattern PATTERN = Pattern.compile("\\w+|[^\\w\\s]+");
  private static final Logger LOG = LoggerFactory.getLogger(Tokenizer.class);
  List<TAGToken> tokens = new ArrayList<>();
  Map<TAGToken, List<TokenProvenance>> tokenProvenanceMap = new HashMap<>();

  public Tokenizer(DocumentWrapper document, TAGView tagView) {
    if (!document.hasTextNodes()) {
      return;
    }

    Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
//    StringBuilder textBuilder = new StringBuilder();
    List<TextNodeWrapper> textNodesToJoin = new ArrayList<>();
    List<TextNodeWrapper> textNodesToMap = new ArrayList<>();
    document.getTextNodeStream().forEach(tn -> {
      textNodesToMap.add(tn);
      List<MarkupWrapper> markups = document.getMarkupStreamForTextNode(tn)//
          .filter(tagView::isIncluded)//
          .collect(toList());

      List<MarkupWrapper> toClose = new ArrayList<>(openMarkup);
      toClose.removeAll(markups);
      Collections.reverse(toClose);

      List<MarkupWrapper> toOpen = new ArrayList<>(markups);
      toOpen.removeAll(openMarkup);

      openMarkup.removeAll(toClose);
      openMarkup.addAll(toOpen);

      if (!toClose.isEmpty() || !toOpen.isEmpty()) {
        addTokens(textNodesToJoin);
        textNodesToJoin.clear();
      }
      toClose.stream()//
          .map(MarkupWrapper::getTag)//
          .map(MarkupCloseToken::new)//
          .forEach(t -> {
            tokens.add(t);
            tokenProvenanceMap.put(t, singletonList(new MarkupCloseTokenProvenance(tn)));
          });

      toOpen.stream()//
          .map(MarkupWrapper::getTag)//
          .map(MarkupOpenToken::new)//
          .forEach(t -> {
            tokens.add(t);
            tokenProvenanceMap.put(t, singletonList(new MarkupOpenTokenProvenance(tn)));
          });

      textNodesToJoin.add(tn);
    });
    addTokens(textNodesToJoin);
    TextNodeWrapper lastTextNodeWrapper = textNodesToJoin.get(textNodesToJoin.size() - 1);
    stream(openMarkup.descendingIterator())//
        .map(MarkupWrapper::getTag)//
        .map(MarkupCloseToken::new)//
        .forEach(t -> {
          tokens.add(t);
          tokenProvenanceMap.put(t, singletonList(new MarkupCloseTokenProvenance(lastTextNodeWrapper)));
        });
  }

  private void addTokens(final List<TextNodeWrapper> textNodesToJoin) {
    String joinedText = textNodesToJoin.stream().map(TextNodeWrapper::getText).collect(joining());
    Map<TextNodeWrapper, Range<Integer>> textNodeRanges = getTextNodeWrapperRangeMap(textNodesToJoin);
    AtomicInteger start = new AtomicInteger(0);
    for (TextToken t : tokenizeText(joinedText)) {
      tokens.add(t);
      final int offset = start.get();
      Range<Integer> range = calcRange(t.content, start);
      final List<TokenProvenance> textNodeWrappers = textNodeRanges.keySet()
          .stream()
          .filter(tn -> textNodeRanges.get(tn).isOverlappedBy(range))
          .map(tn -> new TextTokenProvenance(tn, offset))
          .collect(toList());
      tokenProvenanceMap.put(t, textNodeWrappers);
    }
  }

  static Range<Integer> calcRange(final String content, final AtomicInteger start) {
    int fromInclusive = start.get();
    int toInclusive = fromInclusive + content.length();
    Range<Integer> range = Range.between(fromInclusive, toInclusive);
    start.set(toInclusive + 1);
    return range;
  }

  private Map<TextNodeWrapper, Range<Integer>> getTextNodeWrapperRangeMap(final List<TextNodeWrapper> textNodesToJoin) {
    Map<TextNodeWrapper, Range<Integer>> textNodeRanges = new LinkedHashMap<>();
    AtomicInteger start = new AtomicInteger(0);
    textNodesToJoin.forEach(tn -> {
      Range<Integer> range = calcRange(tn.getText(), start);
      textNodeRanges.put(tn, range);
    });
    return textNodeRanges;
  }

  public List<TAGToken> getTAGTokens() {
    return tokens;
  }

  public Map<TAGToken, List<TokenProvenance>> getTokenProvenanceMap() {
    return tokenProvenanceMap;
  }

  private static final Pattern WS_AND_PUNCT = Pattern.compile("[" + SimplePatternTokenizer.PUNCT + "\\s]+");

  static List<TextToken> tokenizeText(String text) {
    if (WS_AND_PUNCT.matcher(text).matches()) {
      return new ArrayList<>(singletonList(new TextToken(text)));
    }
    return SimplePatternTokenizer.BY_WS_AND_PUNCT//
        .apply(text)//
        .map(TextToken::new)//
        .collect(toList());
  }

}
