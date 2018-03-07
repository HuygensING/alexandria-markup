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
import java.util.ArrayList;
import java.util.List;

import static nl.knaw.huygens.alexandria.compare.MarkupInfo.State.openEnd;

public class SegmentTool {

  enum ParseState {text, markup}

  public static List<TextNodeInfo> computeTextNodeInfo(Segment segment) {
    List<TextNodeInfo> info = new ArrayList<>();
    StringBuilder textBuilder = new StringBuilder();
    List<TAGToken> closingMarkup = new ArrayList<>();
    List<TAGToken> openingMarkup = new ArrayList<>();
    ParseState state = ParseState.text;
    for (TAGToken token : segment.tokensB()) {
      if (token instanceof TextToken) {
        TextToken textToken = (TextToken) token;
        textBuilder.append(textToken.content);
        state = ParseState.text;

      } else if (token instanceof MarkupOpenToken) {
        saveOnStateChange(info, textBuilder, openingMarkup, state);
        openingMarkup.add(token);
        state = ParseState.markup;

      } else if (token instanceof MarkupCloseToken) {
        saveOnStateChange(info, textBuilder, openingMarkup, state);
        TextNodeInfo lastTextNodeInfo = info.get(info.size() - 1);
        MarkupCloseToken markupCloseToken = (MarkupCloseToken) token;
        lastTextNodeInfo.closeMarkup(markupCloseToken.content);
        closingMarkup.add(token);
        state = ParseState.markup;

      }

    }
    return info;
  }

  private static void saveOnStateChange(List<TextNodeInfo> info, StringBuilder textBuilder, List<TAGToken> openingMarkup, ParseState state) {
    if (state == ParseState.text) {
      TextNodeInfo textNodeInfo = new TextNodeInfo();
      textNodeInfo.setText(textBuilder.toString());
      textBuilder.delete(0, textBuilder.length());
      openingMarkup.stream()
          .map(m -> new MarkupInfo(m.content, openEnd))
          .forEach(textNodeInfo::addMarkupInfo);
      info.add(textNodeInfo);
    }
  }
}
