package nl.knaw.huygens.alexandria;

/*
 * #%L
 * alexandria-markup
 * =======
 * Copyright (C) 2016 - 2017 Huygens ING (KNAW)
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
import java.util.BitSet;
import java.util.List;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;

public class ErrorListener implements ANTLRErrorListener {
  private final List<String> errors = new ArrayList<>();

  @Override
  public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
    errors.add("syntax error: line " + line + ":" + charPositionInLine + " " + msg.replace("token recognition error at", "unexpected token"));
  }

  @Override
  public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
    errors.add("ambiguity: recognizer=" + recognizer //
        + ", dfa=" + dfa //
        + ", startIndex=" + startIndex //
        + ", stopIndex=" + stopIndex//
        + ", exact=" + exact //
        + ", ambigAlts=" + ambigAlts //
        + ", configs=" + configs);
  }

  @Override
  public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
    errors.add("attempting full context error: recognizer=" + recognizer //
        + ", dfa=" + dfa //
        + ", startIndex=" + startIndex //
        + ", stopIndex=" + stopIndex//
        + ", conflictingAlts=" + conflictingAlts //
        + ", configs=" + configs);
  }

  @Override
  public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
    errors.add("context sensitivity error: recognizer=" + recognizer //
        + ", dfa=" + dfa //
        + ", startIndex=" + startIndex //
        + ", stopIndex=" + stopIndex//
        + ", prediction=" + prediction //
        + ", configs=" + configs);

  }

  public List<String> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

}
