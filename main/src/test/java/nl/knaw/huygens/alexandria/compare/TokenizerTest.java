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

import nl.knaw.huc.di.tag.tagml.importer.TAGMLImporter;
import nl.knaw.huygens.alexandria.AlexandriaBaseStoreTest;
import nl.knaw.huygens.alexandria.storage.TAGDocument;
import nl.knaw.huygens.alexandria.storage.TAGStore;
import nl.knaw.huygens.alexandria.storage.TAGTextNode;
import nl.knaw.huygens.alexandria.view.TAGView;
import org.junit.Ignore;
import org.junit.Test;
import prioritised_xml_collation.TAGToken;
import prioritised_xml_collation.TextToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class TokenizerTest extends AlexandriaBaseStoreTest {
  @Test
  public void testTokenizeText() {
    String a_b_c = "a b c";
    List<TextToken> textTokens = tokenize(a_b_c);
    assertThat(textTokens).extracting("content")
        .containsExactly("a ", "b ", "c");
  }

  @Test
  public void testLeadingWhitespaceIsPreserved() {
    List<TextToken> textTokens = tokenize(" a b c");
    assertThat(textTokens).extracting("content")
        .containsExactly(" ", "a ", "b ", "c");
  }

  @Test
  public void testTokenizeText2() {
    List<TextToken> textTokens = tokenize("\n");
    assertThat(textTokens).extracting("content")
        .containsExactly("\n");
  }

  @Test
  public void testTokenizeText3() {
    List<TextToken> textTokens = tokenize(" ");
    assertThat(textTokens).extracting("content")
        .containsExactly(" ");
  }

  @Test
  public void testTokenizeText4() {
    List<TextToken> textTokens = tokenize("! ");
    assertThat(textTokens).extracting("content")
        .containsExactly("! ");
  }

  @Test
  public void testTokenizeText5() {
    List<TextToken> textTokens = tokenize("(Alas, poor Yorick!)");
    assertThat(textTokens).extracting("content")
        .containsExactly("(", "Alas", ", ", "poor ", "Yorick", "!)");
  }

  @Test
  public void testTokenizeText6() {
    List<TextToken> textTokens = tokenize("Lucy, for you the snowdrop and the bay");
    assertThat(textTokens).extracting("content")
        .containsExactly("Lucy", ", ", "for ", "you ", "the ", "snowdrop ", "and ", "the ", "bay");
  }

  @Test
  public void testTokenizeText7() {
    List<TextToken> textTokens = tokenize("main de femme, élégante et fine");
    assertThat(textTokens).extracting("content")
        .containsExactly("main ", "de ", "femme", ", ", "élégante ", "et ", "fine");
  }

  @Ignore
  @Test
  public void testTokenizer() {
    runInStoreTransaction(store -> {
      TAGDocument doc = new TAGMLImporter(store).importTAGML("[l>[phr>Alas,<phr] [phr>poor Yorick!<phr]<l]");
      TAGView onlyLines = new TAGView(store).setMarkupToInclude(singleton("l"));
      Tokenizer tokenizer = new Tokenizer(doc, onlyLines);
      List<TAGToken> tokens = tokenizer.getTAGTokens();
      assertThat(tokens).extracting("content")//
          .containsExactly("l", "Alas", ", ", "poor ", "Yorick", "!", "/l");

      ExtendedTextToken alas = ((ExtendedTextToken) tokens.get(1));
      assertTextNodesTextMatches(alas, store, "Alas,");

      ExtendedTextToken comma = (ExtendedTextToken) tokens.get(2);
      assertTextNodesTextMatches(comma, store, "Alas,", " ");

      ExtendedTextToken poor = (ExtendedTextToken) tokens.get(3);
      assertTextNodesTextMatches(poor, store, "poor Yorick!");

      ExtendedTextToken yorick = (ExtendedTextToken) tokens.get(4);
      assertTextNodesTextMatches(yorick, store, "poor Yorick!");

      ExtendedTextToken exclamation = (ExtendedTextToken) tokens.get(5);
      assertTextNodesTextMatches(exclamation, store, "poor Yorick!");

    });
  }

  private List<TextToken> tokenize(final String text) {
    final List<Long> textNodeIds = new ArrayList<>();
    final Map<Long, Tokenizer.TextTokenInfo> textTokenInfoMap = new HashMap<>();
    return Tokenizer.tokenizeText(text, 0, textNodeIds, textTokenInfoMap);
  }

  private void assertTextNodesTextMatches(final ExtendedTextToken alas, final TAGStore store, final String... contents) {
    List<String> textNodeContents = alas.getTextNodeIds()
        .stream()
//        .peek(System.out::println)
        .map(store::getTextNode)
        .map(TAGTextNode::getText)
        .collect(toList());
    assertThat(textNodeContents).containsExactly(contents);
  }
}
