package nl.knaw.huc.di.tag.tagml;

/*-
 * #%L
 * tagml
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

import static java.util.regex.Pattern.quote;

public class TAGML {
  public static final String OPEN_TAG_STARTCHAR = "[";
  public static final String OPEN_TAG_ENDCHAR = ">";
  public static final String MILESTONE_TAG_ENDCHAR = "]";
  public static final String CLOSE_TAG_STARTCHAR = "<";
  public static final String CLOSE_TAG_ENDCHAR = "]";

  public static final String DIVERGENCE = "<|";
  public static final String DIVERGENCE_STARTCHAR = "<";
  public static final String CONVERGENCE = "|>";
  public static final String DIVIDER = "|";

  public static final String OPTIONAL_PREFIX = "?";
  public static final String SUSPEND_PREFIX = "-";
  public static final String RESUME_PREFIX = "+";

  public static final String DEFAULT_LAYER = "";

  public static final String BRANCHES = ":branches";
  public static final String BRANCH = ":branch";

  public static final String BRANCHES_START = OPEN_TAG_STARTCHAR + BRANCHES + OPEN_TAG_ENDCHAR;
  public static final String BRANCH_START = OPEN_TAG_STARTCHAR + BRANCH + OPEN_TAG_ENDCHAR;
  public static final String BRANCH_END = CLOSE_TAG_STARTCHAR + BRANCH + CLOSE_TAG_ENDCHAR;
  public static final String BRANCHES_END = CLOSE_TAG_STARTCHAR + BRANCHES + CLOSE_TAG_ENDCHAR;

  public static String escapeRegularText(final String content) {
    return content
        .replace("\\", "\\\\")
        .replace("<", "\\<")
        .replace("[", "\\[")
        ;
  }

  public static String escapeVariantText(final String content) {
    return content
        .replace("\\", "\\\\")
        .replace("<", "\\<")
        .replace("[", "\\[")
        .replace("|", "\\|");
  }

  public static String escapeSingleQuotedText(final String content) {
    return content
        .replace("\\", "\\\\")
        .replace("'", "\\'");
  }

  public static String escapeDoubleQuotedText(final String content) {
    return content
        .replace("\\", "\\\\")
        .replace("\"", "\\\"");
  }

  public static String unEscape(final String text) {
    return text
        .replaceAll(quote("\\<"), "<")
        .replaceAll(quote("\\["), "[")
        .replaceAll(quote("\\|"), "|")
        .replaceAll(quote("\\!"), "!")
        .replaceAll(quote("\\\""), "\"")
        .replaceAll(quote("\\'"), "'")
        .replace("\\\\", "\\");
  }

}


