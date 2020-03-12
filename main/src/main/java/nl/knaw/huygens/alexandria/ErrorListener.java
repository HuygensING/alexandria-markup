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
import nl.knaw.huc.di.tag.tagml.importer.Position;
import nl.knaw.huc.di.tag.tagml.importer.Range;
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
import java.util.Comparator;
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
  private boolean hasBreakingError = false;

  static final Comparator<? super TAGError> TAG_ERROR_COMPARATOR =
          (Comparator<TAGError>)
                  (e1, e2) -> {
                    if (e1 instanceof CustomError && e2 instanceof CustomError) {
                      return Comparator.comparing(
                              ce -> ((CustomError) ce).range.getStartPosition().getLine())
                              .thenComparing(ce -> ((CustomError) ce).range.getStartPosition().getCharacter())
                              .thenComparing(ce -> ((CustomError) ce).range.getEndPosition().getLine())
                              .thenComparing(ce -> ((CustomError) ce).range.getEndPosition().getCharacter())
                              .thenComparing(e -> ((TAGError) e).getMessage())
                              .compare(e1, e2);
                    }
                    if (e1 instanceof TAGSyntaxError && e2 instanceof TAGSyntaxError) {
                      return Comparator.comparing(se -> ((TAGSyntaxError) se).position.getLine())
                              .thenComparing(se -> ((TAGSyntaxError) se).position.getCharacter())
                              .thenComparing(e -> ((TAGError) e).getMessage())
                              .compare(e1, e2);
                    }
                    if (e1 instanceof CustomError && e2 instanceof TAGSyntaxError) {
                      return Comparator.comparing(Position::getLine)
                              .thenComparing(Position::getCharacter)
                              .compare(
                                      ((CustomError) e1).range.getStartPosition(), ((TAGSyntaxError) e2).position);
                    }
                    if (e1 instanceof TAGSyntaxError && e2 instanceof CustomError) {
                      return Comparator.comparing(Position::getLine)
                              .thenComparing(Position::getCharacter)
                              .compare(
                                      ((TAGSyntaxError) e1).position, ((CustomError) e2).range.getStartPosition());
                    }
                    return Comparator.comparing(TAGError::getMessage).compare(e1, e2);
                  };

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

  @Override
  public void syntaxError(
          Recognizer<?, ?> recognizer,
          Object offendingSymbol,
          int line,
          int charPositionInLine,
          String msg,
          RecognitionException e) {
    String message =
            format("syntax error: %s", msg.replace("token recognition error at", "unexpected token"));
    errors.add(new TAGSyntaxError(message, line, charPositionInLine));
  }

  public String getPrefixedErrorMessagesAsString() {
    return errors.stream()
            .sorted(TAG_ERROR_COMPARATOR)
            .map(this::prefixedErrorMessage)
            .collect(joining("\n"));
  }

  private String prefixedErrorMessage(TAGError error) {
    if (error instanceof CustomError) {
      return prefix(((CustomError) error).range.getStartPosition()) + error.getMessage();
    }
    if (error instanceof TAGSyntaxError) {
      return prefix(((TAGSyntaxError) error).position) + error.getMessage();
    }
    return "";
  }

  private String prefix(Position position) {
    return String.format("line %d:%d : ", position.getLine(), position.getCharacter());
  }

  public void addError(
          Position startPos, Position endPos, String messageTemplate, Object... messageArgs) {
    errors.add(new CustomError(startPos, endPos, format(messageTemplate, messageArgs)));
  }

  @Deprecated()
  public void addError(String messageTemplate, Object... messageArgs) {
    errors.add(
            new CustomError(
                    new Position(1, 1), new Position(1, 1), format(messageTemplate, messageArgs)));
  }

  public void addBreakingError(
          Position startPos, Position endPos, String messageTemplate, Object... messageArgs) {
    addError(startPos, endPos, messageTemplate, messageArgs);
    abortParsing(messageTemplate, messageArgs);
  }

  @Deprecated()
  public void addBreakingError(String messageTemplate, Object... messageArgs) {
    addError(messageTemplate, messageArgs);
    abortParsing(messageTemplate, messageArgs);
  }

  private void abortParsing(final String messageTemplate, Object... messageArgs) {
    hasBreakingError = true;
    throw new TAGMLBreakingError(format(messageTemplate, messageArgs) + "\nparsing aborted!");
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasBreakingError() {
    return hasBreakingError;
  }

  public abstract static class TAGError {
    private final String message;

    public TAGError(String message) {
      this.message = message;
    }

    public String getMessage() {
      return message;
    }
  }

  public static class TAGSyntaxError extends TAGError {
    public final Position position;

    public TAGSyntaxError(String message, int line, int character) {
      super(message);
      this.position = new Position(line, character);
    }
  }

  public static class TAGAmbiguityError extends TAGError {
    public TAGAmbiguityError(String message) {
      super(message);
    }
  }

  public class TAGAttemptingFullContextError extends TAGError {
    public TAGAttemptingFullContextError(String message) {
      super(message);
    }
  }

  public class TAGContextSensitivityError extends TAGError {
    public TAGContextSensitivityError(String message) {
      super(message);
    }
  }

  public class CustomError extends TAGError {
    public final Range range;

    public CustomError(Position startPos, Position endPos, String message) {
      super(message);
      this.range = new Range(startPos, endPos);
    }

    @Override
    public String toString() {
      return "CustomError{" + "range=" + range + ", message=" + getMessage() + '}';
    }
  }
}
