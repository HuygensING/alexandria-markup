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

import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author <a href="http://gregor.middell.net/">Gregor Middell</a>
 * @author Ronald Haentjens Dekker
 */
public class SimplePatternTokenizer {

  static final String PUNCT = Pattern.quote(".?!,;:");

  static Function<String, Stream<String>> tokenizer(Pattern pattern) {
    return input -> {
      final Matcher matcher = pattern.matcher(input);
      final List<String> tokens = new LinkedList<>();
      while (matcher.find()) {
        tokens.add(input.substring(matcher.start(), matcher.end()));
      }
      return tokens.stream();
    };
  }

  public static final Function<String, Stream<String>> BY_WHITESPACE = tokenizer(Pattern.compile("\\s*?\\S+\\s*]"));

  public static final Function<String, Stream<String>> BY_WS_AND_PUNCT = tokenizer(Pattern.compile("[\\s" + PUNCT + "]*?[^\\s" + PUNCT + "]+[\\s" + PUNCT + "]*"));

  public static final Function<String, Stream<String>> BY_WS_OR_PUNCT = tokenizer(Pattern.compile("[" + PUNCT + "]+[\\s]*|[^" + PUNCT + "\\s]+[\\s]*"));

}
