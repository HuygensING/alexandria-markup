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

import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGMarkup;
import nl.knaw.huygens.alexandria.view.TAGView;
import prioritised_xml_collation.MarkupCloseToken;
import prioritised_xml_collation.MarkupOpenToken;
import prioritised_xml_collation.TAGToken;
import prioritised_xml_collation.TextToken;

import java.util.*;
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

  public List<TAGToken> getTAGTokens() {
    List<TAGToken> tokens = new ArrayList<>();
    Deque<TAGMarkup> openMarkup = new ArrayDeque<>();
    StringBuilder textBuilder = new StringBuilder();
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
        tokens.addAll(tokenizeText(textBuilder.toString()));
        textBuilder.delete(0, textBuilder.length());
      }
      toClose.stream()//
          .map(this::toMarkupCloseToken)//
          .forEach(tokens::add);

      toOpen.stream()//
          .map(this::toMarkupOpenToken)//
          .forEach(tokens::add);

      String text = tn.getText();
      textBuilder.append(text);
    });
    tokens.addAll(tokenizeText(textBuilder.toString()));
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

  static List<TextToken> tokenizeText(String text) {
    if (WS_OR_PUNCT.matcher(text).matches()) {
      return new ArrayList<>(singletonList(new TextToken(text)));
    }
    return SimplePatternTokenizer.BY_WS_OR_PUNCT//
        .apply(text)//
        .map(TextToken::new)//
        .collect(toList());
  }

}
