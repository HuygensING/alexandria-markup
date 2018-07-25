package nl.knaw.huygens.alexandria;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2018 HuC DI (KNAW)
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

import nl.knaw.huc.di.tag.tagml.TAGMLBreakingError;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import static java.lang.String.format;

public class ErrorListener implements ANTLRErrorListener {
  private static final Logger LOG = LoggerFactory.getLogger(ErrorListener.class);
  private final List<String> errors = new ArrayList<>();
  private boolean reportAmbiguity = false;
  private boolean reportAttemptingFullContext = false;
  private boolean reportContextSensitivity = true;

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
    errors.add(format("syntax error: line %d:%d %s", line, charPositionInLine, msg.replace("token recognition error at", "unexpected token")));
  }

  @Override
  public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
    if (reportAmbiguity) {
      errors.add("ambiguity:\n recognizer=" + recognizer //
          + ",\n dfa=" + dfa //
          + ",\n startIndex=" + startIndex //
          + ",\n stopIndex=" + stopIndex//
          + ",\n exact=" + exact //
          + ",\n ambigAlts=" + ambigAlts //
          + ",\n configs=" + configs);
    }
  }

  @Override
  public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
    if (reportAttemptingFullContext) {
      errors.add("attempting full context error:\n recognizer=" + recognizer //
          + ",\n dfa=" + dfa //
          + ",\n startIndex=" + startIndex //
          + ",\n stopIndex=" + stopIndex//
          + ",\n conflictingAlts=" + conflictingAlts //
          + ",\n configs=" + configs);
    }
  }

  @Override
  public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
    if (reportContextSensitivity) {
      errors.add("context sensitivity error:\n recognizer=" + recognizer //
          + ",\n dfa=" + dfa //
          + ",\n startIndex=" + startIndex //
          + ",\n stopIndex=" + stopIndex//
          + ",\n prediction=" + prediction //
          + ",\n configs=" + configs);
    }
  }

  public List<String> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public void addError(String messageTemplate, Object... messageArgs) {
    errors.add(format(messageTemplate, messageArgs));
  }

  public void addBreakingError(String messageTemplate, Object... messageArgs) {
    addError(messageTemplate, messageArgs);
    addError("parsing aborted!");
    throw new TAGMLBreakingError(format(messageTemplate, messageArgs));
  }

}
