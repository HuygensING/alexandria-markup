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

import java.util.*;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.StreamUtil.stream;

class Tokenizer {
  public static final Pattern PATTERN = Pattern.compile("\\w+|[^\\w\\s]+");
  List<TAGToken> tokens = new ArrayList<>();
  Map<TAGToken, List<TextNodeWrapper>> tokenToNodeMap = new HashMap<>();

  public Tokenizer(DocumentWrapper document, TAGView tagView) {
    Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
    StringBuilder textBuilder = new StringBuilder();
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
        tokens.addAll(tokenizeText(textBuilder.toString()));
        textBuilder.delete(0, textBuilder.length());
      }
      toClose.stream()//
          .map(MarkupWrapper::getTag)//
          .map(MarkupCloseToken::new)//
          .forEach(tokens::add);

      toOpen.stream()//
          .map(MarkupWrapper::getTag)//
          .map(MarkupOpenToken::new)//
          .forEach(tokens::add);

      String text = tn.getText();
      textBuilder.append(text);
    });
    tokens.addAll(tokenizeText(textBuilder.toString()));
    stream(openMarkup.descendingIterator())//
        .map(MarkupWrapper::getTag)//
        .map(MarkupCloseToken::new)//
        .forEach(tokens::add);
  }

  public List<TAGToken> getTAGTokens() {
    return tokens;
  }

  public Map<TAGToken, List<TextNodeWrapper>> getTokenToNodeMap() {
    return tokenToNodeMap;
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
