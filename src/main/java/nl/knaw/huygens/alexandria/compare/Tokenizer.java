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
import nl.knaw.huygens.alexandria.view.TAGView;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static nl.knaw.huygens.alexandria.StreamUtil.stream;

class Tokenizer {
  private final DocumentWrapper document;
  private final TAGView tagView;

  public Tokenizer(DocumentWrapper document, TAGView tagView) {
    this.document = document;
    this.tagView = tagView;
  }

  public List<TAGToken> getTAGTokenStream() {
    List<TAGToken> tokens = new ArrayList<>();
    Deque<MarkupWrapper> openMarkup = new ArrayDeque<>();
    document.getTextNodeStream().forEach(tn -> {
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

      toClose.stream()//
          .map(MarkupWrapper::getTag)//
          .map(MarkupCloseToken::new)//
          .forEach(tokens::add);

      toOpen.stream()//
          .map(MarkupWrapper::getTag)//
          .map(MarkupOpenToken::new)//
          .forEach(tokens::add);

      tokens.add(new TextToken(tn.getText()));

    });
    stream(openMarkup.descendingIterator())//
        .map(MarkupWrapper::getTag)//
        .map(MarkupCloseToken::new)//
        .forEach(tokens::add);

    return tokens;
  }

}
