package nl.knaw.huygens.alexandria;

/*
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
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

public class ErrorListener implements ANTLRErrorListener {
  private static final Logger LOG = LoggerFactory.getLogger(ErrorListener.class);
  private final List<TAGError> errors = new ArrayList<>();
  private boolean reportAmbiguity = false;
  private boolean reportAttemptingFullContext = false;
  private boolean reportContextSensitivity = true;

  @Override
  public void syntaxError(
      Recognizer<?, ?> recognizer,
      Object offendingSymbol,
      int line,
      int charPositionInLine,
      String msg,
      RecognitionException e) {
    String message =
        format(
            "syntax error: line %d:%d %s",
            line,
            charPositionInLine,
            msg.replace("token recognition error at", "unexpected token"));
    errors.add(new TAGSyntaxError(message, line, charPositionInLine));
  }

  @Override
  public void reportAmbiguity(
      Parser recognizer,
      DFA dfa,
      int startIndex,
      int stopIndex,
      boolean exact,
      BitSet ambigAlts,
      ATNConfigSet configs) {
    if (reportAmbiguity) {
      String message =
          "ambiguity:\n recognizer="
              + recognizer //
              + ",\n dfa="
              + dfa //
              + ",\n startIndex="
              + startIndex //
              + ",\n stopIndex="
              + stopIndex //
              + ",\n exact="
              + exact //
              + ",\n ambigAlts="
              + ambigAlts //
              + ",\n configs="
              + configs;
      errors.add(new TAGAmbiguityError(message));
    }
  }

  @Override
  public void reportAttemptingFullContext(
      Parser recognizer,
      DFA dfa,
      int startIndex,
      int stopIndex,
      BitSet conflictingAlts,
      ATNConfigSet configs) {
    if (reportAttemptingFullContext) {
      String message =
          "attempting full context error:\n recognizer="
              + recognizer //
              + ",\n dfa="
              + dfa //
              + ",\n startIndex="
              + startIndex //
              + ",\n stopIndex="
              + stopIndex //
              + ",\n conflictingAlts="
              + conflictingAlts //
              + ",\n configs="
              + configs;
      errors.add(new TAGAttemptingFullContextError(message));
    }
  }

  @Override
  public void reportContextSensitivity(
      Parser recognizer,
      DFA dfa,
      int startIndex,
      int stopIndex,
      int prediction,
      ATNConfigSet configs) {
    if (reportContextSensitivity) {
      String message =
          "context sensitivity error:\n recognizer="
              + recognizer //
              + ",\n dfa="
              + dfa //
              + ",\n startIndex="
              + startIndex //
              + ",\n stopIndex="
              + stopIndex //
              + ",\n prediction="
              + prediction //
              + ",\n configs="
              + configs;
      errors.add(new TAGContextSensitivityError(message));
    }
  }

  public List<TAGError> getErrors() {
    return errors;
  }

  public List<String> getErrorMessages() {
    return errors.stream().map(TAGError::getMessage).collect(toList());
  }

  public String getErrorMessagesAsString() {
    return errors.stream().map(TAGError::getMessage).collect(joining("\n"));
  }

  public void addError(String messageTemplate, Object... messageArgs) {
    errors.add(new CustomError(format(messageTemplate, messageArgs)));
  }

  public abstract class TAGError {
    private final String message;

    public TAGError(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  class TAGSyntaxError extends TAGError {
    public final int line;
    public final int character;

    public TAGSyntaxError(String message, int line, int character) {
      super(message);
      this.line = line;
      this.character = character;
    }
  }

  private class TAGAmbiguityError extends TAGError {
    public TAGAmbiguityError(String message) {
      super(message);
    }
  }

  private class TAGAttemptingFullContextError extends TAGError {
    public TAGAttemptingFullContextError(String message) {
      super(message);
    }
  }

  private class TAGContextSensitivityError extends TAGError {
    public TAGContextSensitivityError(String message) {
      super(message);
    }
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  private class CustomError extends TAGError {
    public CustomError(String message) {
      super(message);
    }
  }

  public void addBreakingError(String messageTemplate, Object... messageArgs) {
    addError(messageTemplate, messageArgs);
    addError("parsing aborted!");
    throw new TAGMLBreakingError(format(messageTemplate, messageArgs));
  }
}
