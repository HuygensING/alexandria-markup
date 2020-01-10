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
import nl.knaw.huygens.alexandria.view.TAGView;
import prioritised_xml_collation.MarkupCloseToken;
import prioritised_xml_collation.MarkupOpenToken;
import prioritised_xml_collation.TAGToken;
import prioritised_xml_collation.TextToken;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.StreamUtil.stream;
import static nl.knaw.huygens.alexandria.compare.SimplePatternTokenizer.PUNCT;

class Tokenizer {
  private final TAGDocument document;
  private final TAGView tagView;

  public Tokenizer(TAGDocument document, TAGView tagView) {
    this.document = document;
    this.tagView = tagView;
  }

  static class TextTokenInfo {
    Integer offset;
    Integer length;

    TextTokenInfo(Integer offset, Integer length) {
      this.offset = offset;
      this.length = length;
    }

    public void setOffset(final Integer offset) {
      this.offset = offset;
    }

    Integer getOffset() {
      return offset;
    }

    public void setLength(final Integer length) {
      this.length = length;
    }

    Integer getLength() {
      return length;
    }
  }

  public List<TAGToken> getTAGTokens() {
    List<TAGToken> tokens = new ArrayList<>();
    Deque<TAGMarkup> openMarkup = new ArrayDeque<>();
    StringBuilder textBuilder = new StringBuilder();
    List<Long> textNodeIds = new ArrayList<>();
    final AtomicReference<Integer> totalTextSize = new AtomicReference<>(0);
    Map<Long, TextTokenInfo> textTokenInfoMap = new HashMap<>();
    document.getTextNodeStream().forEach(tn -> {
      List<TAGMarkup> markups = document.getMarkupStreamForTextNode(tn)//
          .filter(tagView::isIncluded)//
          .collect(toList());

      List<TAGMarkup> toClose = new ArrayList<>(openMarkup);
      toClose.removeAll(markups);
      Collections.reverse(toClose);

      List<TAGMarkup> toOpen = new ArrayList<>(markups);
      toOpen.removeAll(openMarkup);

      openMarkup.removeAll(toClose);
      openMarkup.addAll(toOpen);

      if (!toClose.isEmpty() || !toOpen.isEmpty()) {
        tokens.addAll(tokenizeText(textBuilder.toString(), totalTextSize.get(), textNodeIds, textTokenInfoMap));
        textBuilder.delete(0, textBuilder.length());
        textNodeIds.clear();
      }

      toClose.stream()//
          .map(this::toMarkupCloseToken)//
          .forEach(tokens::add);

      toOpen.stream()//
          .map(this::toMarkupOpenToken)//
          .forEach(tokens::add);

      String text = tn.getText();
      textBuilder.append(text);
      Long textNodeId = tn.getDbId();
      textNodeIds.add(textNodeId);
      textTokenInfoMap.put(textNodeId, new TextTokenInfo(totalTextSize.get(), text.length()));
      totalTextSize.updateAndGet(v -> v + text.length());
    });
    tokens.addAll(tokenizeText(textBuilder.toString(), totalTextSize.get(), textNodeIds, textTokenInfoMap));
    stream(openMarkup.descendingIterator())//
        .map(this::toMarkupCloseToken)//
        .forEach(tokens::add);

    return tokens;
  }

  private MarkupOpenToken toMarkupOpenToken(TAGMarkup tagMarkup) {
    return new MarkupOpenToken(tagMarkup.getTag());
  }

  private MarkupCloseToken toMarkupCloseToken(TAGMarkup tagMarkup) {
    return new MarkupCloseToken("/" + tagMarkup.getTag());
  }

  private static final Pattern WS_OR_PUNCT = Pattern.compile(format("[%s]+[\\s]*|[\\s]+", PUNCT));

  static List<TextToken> tokenizeText(String text, Integer endOffset, List<Long> textNodeIds, final Map<Long, TextTokenInfo> textTokenInfoMap) {
    if (WS_OR_PUNCT.matcher(text).matches()) {
      return new ArrayList<>(singletonList(new ExtendedTextToken(text).addTextNodeIds(textNodeIds)));
    }
    List<String> parts = SimplePatternTokenizer.BY_WS_OR_PUNCT//
        .apply(text)//
        .collect(toList());
    final List<TextToken> textTokens = new ArrayList<>();
    int textPartStart = endOffset - text.length();
    for (String part : parts) {
      final ExtendedTextToken textToken = new ExtendedTextToken(part);
      int length = part.length();
      for (final Long textNodeId : textNodeIds) {
        TextTokenInfo textTokenInfo = textTokenInfoMap.get(textNodeId);
        Integer textPartEnd = textPartStart + length - 1;
        Integer textNodeStart = textTokenInfo.getOffset();
        Integer textNodeEnd = textNodeStart + textTokenInfo.getLength() - 1;
        boolean textPartStartIsInTextNode = textNodeStart <= textPartStart && textNodeEnd >= textPartStart;
        boolean textPartEndIsInTextNode = textNodeStart <= textPartEnd && textNodeEnd >= textPartEnd;
        if (textPartStartIsInTextNode || textPartEndIsInTextNode) {
          textToken.addTextNodeId(textNodeId);
        }
      }
      textPartStart += length;
      textTokens.add(textToken);
    }
    return textTokens;
  }

}
