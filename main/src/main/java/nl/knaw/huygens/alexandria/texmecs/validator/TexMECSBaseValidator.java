package nl.knaw.huygens.alexandria.texmecs.validator;

/*
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

import nl.knaw.huygens.alexandria.texmecs.grammar.TexMECSParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TexMECSBaseValidator implements TexMECSValidator {
  private final ValidationReport report = new ValidationReport();
  private final List<String> errors = new ArrayList<>();

  @Override
  public ValidationReport getValidationReport() {
    return report;
  }

  @Override
  public boolean hasErrors() {
    return !getErrors().isEmpty();
  }

  @Override
  public Collection<String> getErrors() {
    return errors;
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterDocument(TexMECSParser.DocumentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitDocument(TexMECSParser.DocumentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterChunk(TexMECSParser.ChunkContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitChunk(TexMECSParser.ChunkContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterStartTag(TexMECSParser.StartTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitStartTag(TexMECSParser.StartTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterEndTag(TexMECSParser.EndTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitEndTag(TexMECSParser.EndTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterSoleTag(TexMECSParser.SoleTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitSoleTag(TexMECSParser.SoleTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterSuspendTag(TexMECSParser.SuspendTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitSuspendTag(TexMECSParser.SuspendTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterResumeTag(TexMECSParser.ResumeTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitResumeTag(TexMECSParser.ResumeTagContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterVirtualElement(TexMECSParser.VirtualElementContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitVirtualElement(TexMECSParser.VirtualElementContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterStartTagSet(TexMECSParser.StartTagSetContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitStartTagSet(TexMECSParser.StartTagSetContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterEndTagSet(TexMECSParser.EndTagSetContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitEndTagSet(TexMECSParser.EndTagSetContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterCdataSection(TexMECSParser.CdataSectionContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitCdataSection(TexMECSParser.CdataSectionContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterCdsecdata(TexMECSParser.CdsecdataContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitCdsecdata(TexMECSParser.CdsecdataContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterCdchars(TexMECSParser.CdcharsContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitCdchars(TexMECSParser.CdcharsContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterComment(TexMECSParser.CommentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitComment(TexMECSParser.CommentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterCommcontent(TexMECSParser.CommcontentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitCommcontent(TexMECSParser.CommcontentContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterCommentdata(TexMECSParser.CommentdataContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitCommentdata(TexMECSParser.CommentdataContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterEid(TexMECSParser.EidContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitEid(TexMECSParser.EidContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterGi(TexMECSParser.GiContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitGi(TexMECSParser.GiContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterId(TexMECSParser.IdContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitId(TexMECSParser.IdContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterIdref(TexMECSParser.IdrefContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitIdref(TexMECSParser.IdrefContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterAtts(TexMECSParser.AttsContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitAtts(TexMECSParser.AttsContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterAvs(TexMECSParser.AvsContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitAvs(TexMECSParser.AvsContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterText(TexMECSParser.TextContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitText(TexMECSParser.TextContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void enterEveryRule(ParserRuleContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void exitEveryRule(ParserRuleContext ctx) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void visitTerminal(TerminalNode node) {
  }

  /**
   * {@inheritDoc}
   * <p>
   * <p>The default implementation does nothing.</p>
   */
  @Override
  public void visitErrorNode(ErrorNode node) {
  }

}
